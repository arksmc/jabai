package com.jabai.service;

import com.jabai.db.Database;
import com.jabai.db.InventoryDao;
import com.jabai.db.LocationDao;
import com.jabai.model.MovementReason;
import com.jabai.model.OrderLine;
import com.jabai.model.RequestLine;
import com.jabai.model.SendLine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockService {
    private final InventoryDao inventoryDao = new InventoryDao();
    private final LocationDao locationDao = new LocationDao();

    @FunctionalInterface
    private interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    /** Runs the work in one transaction: commit if it finishes, roll back if anything throws. */
    private <T> T inTransaction(TxWork<T> work) throws SQLException {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private long insertAndGetId(PreparedStatement ps) throws SQLException {
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            return keys.getLong(1);
        }
    }

    // ---------------------------------------------------------------
    // Manager: order stock from a supplier
    // ---------------------------------------------------------------

    /** Creates an order. Stock does NOT change until the order is received. */
    public long createOrder(int managerId, String supplier, String notes,
                            List<OrderLine> lines) throws SQLException {
        if (supplier == null || supplier.isBlank()) {
            throw new IllegalArgumentException("Supplier is required.");
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Order has no items.");
        }
        return inTransaction(c -> {
            long orderId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO stock_orders(supplier, ordered_by, notes) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, supplier.trim());
                ps.setInt(2, managerId);
                ps.setString(3, notes);
                orderId = insertAndGetId(ps);
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO stock_order_items(order_id, product_id, quantity, unit_cost_centavos) "
                  + "VALUES (?,?,?,?)")) {
                for (OrderLine l : lines) {
                    ps.setLong(1, orderId);
                    ps.setInt(2, l.productId());
                    ps.setDouble(3, l.quantity());
                    ps.setLong(4, l.unitCostCentavos());
                    ps.executeUpdate();
                }
            }
            return orderId;
        });
    }

    /** Marks an order received and adds its items to the WAREHOUSE only. */
    public void receiveOrder(long orderId, int userId) throws SQLException {
        inTransaction(c -> {
            // The status guard stops the same order being received twice.
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE stock_orders SET status = 'Received', received_at = datetime('now','localtime') "
                  + "WHERE id = ? AND status = 'Ordered'")) {
                ps.setLong(1, orderId);
                if (ps.executeUpdate() == 0) {
                    throw new IllegalStateException("This order is not waiting to be received.");
                }
            }

            List<OrderLine> items = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT product_id, quantity FROM stock_order_items WHERE order_id = ?")) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(new OrderLine(rs.getInt("product_id"), rs.getDouble("quantity"), 0));
                    }
                }
            }

            int warehouseId = locationDao.findWarehouseId(c);
            for (OrderLine item : items) {
                inventoryDao.increase(c, warehouseId, item.productId(), item.quantity(),
                        MovementReason.PURCHASE, orderId, userId);
            }
            return null;
        });
    }

    public void cancelOrder(long orderId) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE stock_orders SET status = 'Cancelled' WHERE id = ? AND status = 'Ordered'")) {
            ps.setLong(1, orderId);
            if (ps.executeUpdate() == 0) {
                throw new IllegalStateException("Only orders that are still waiting can be cancelled.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Clerk: request stock for their outlet
    // ---------------------------------------------------------------

    public long createRequest(int outletId, int clerkId, String note,
                              List<RequestLine> lines) throws SQLException {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Request has no items.");
        }
        return inTransaction(c -> {
            long requestId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO stock_requests(location_id, requested_by, note) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, outletId);
                ps.setInt(2, clerkId);
                ps.setString(3, note);
                requestId = insertAndGetId(ps);
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO stock_request_items(request_id, product_id, quantity_requested) VALUES (?,?,?)")) {
                for (RequestLine l : lines) {
                    ps.setLong(1, requestId);
                    ps.setInt(2, l.productId());
                    ps.setDouble(3, l.quantity());
                    ps.executeUpdate();
                }
            }
            return requestId;
        });
    }

    // ---------------------------------------------------------------
    // Manager: answer a request
    // ---------------------------------------------------------------

    /**
     * Sends stock from the warehouse to the outlet that made the request.
     * The manager chooses the quantity per item. Only positive quantities should be included in lines
     * Everything happens in one transaction: if the warehouse is short on any item, nothing moves.
     */

    public void fulfillRequest(
            long requestId,
            int managerId,
            List<SendLine> lines,
            String responseNote) throws SQLException {

        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("No items supplied.");
        }

        inTransaction(c -> {

            // 1. Retrieve request and verify its status.
            String requestSql = """
                    SELECT location_id, status
                    FROM stock_requests
                    WHERE id = ?
                    """;

            int outletId;

            try (PreparedStatement ps = c.prepareStatement(requestSql)) {

                ps.setLong(1, requestId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException("Stock request not found.");
                    }

                    outletId = rs.getInt("location_id");

                    if (!"Pending".equals(rs.getString("status"))) {
                        throw new SQLException("Request is not pending.");
                    }
                }
            }

            // 2. Find warehouse.
            int warehouseId = locationDao.findWarehouseId(c);

            // 3. Validate and process every submitted item.
            java.util.Set<Integer> processedProducts = new java.util.HashSet<>();

            for (SendLine line : lines) {

                if (line == null || line.quantity() <= 0) {
                    throw new IllegalArgumentException(
                            "Sent quantity must be greater than zero."
                    );
                }

                // Prevent duplicate product entries.
                if (!processedProducts.add(line.productId())) {
                    throw new IllegalArgumentException(
                            "A product can only appear once in a fulfillment."
                    );
                }

                String itemSql = """
                        SELECT quantity_requested
                        FROM stock_request_items
                        WHERE request_id = ?
                        AND product_id = ?
                        """;

                double requestedQuantity;

                try (PreparedStatement ps = c.prepareStatement(itemSql)) {

                    ps.setLong(1, requestId);
                    ps.setInt(2, line.productId());

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException(
                                    "Product does not belong to this request: "
                                    + line.productId()
                            );
                        }

                        requestedQuantity = rs.getDouble("quantity_requested");
                    }
                }

                if (line.quantity() > requestedQuantity) {
                    throw new SQLException(
                            "Sent quantity exceeds requested quantity for product "
                            + line.productId()
                    );
                }

                // Record the actual quantity sent.
                String updateSql = """
                        UPDATE stock_request_items
                        SET quantity_sent = ?
                        WHERE request_id = ?
                        AND product_id = ?
                        """;

                try (PreparedStatement ps = c.prepareStatement(updateSql)) {

                    ps.setDouble(1, line.quantity());
                    ps.setLong(2, requestId);
                    ps.setInt(3, line.productId());

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Could not update requested item.");
                    }
                }

                // Deduct from warehouse.
                inventoryDao.decrease(
                        c,
                        warehouseId,
                        line.productId(),
                        line.quantity(),
                        MovementReason.TRANSFER_OUT,
                        requestId,
                        managerId
                );

                // Add to outlet.
                inventoryDao.increase(
                        c,
                        outletId,
                        line.productId(),
                        line.quantity(),
                        MovementReason.TRANSFER_IN,
                        requestId,
                        managerId
                );
            }

            // 4. Mark request as fulfilled.
            String updateRequestSql = """
                    UPDATE stock_requests
                    SET status = 'Fulfilled',
                        response_note = ?,
                        datetime('now','localtime'),
                        resolved_by = ?
                    WHERE id = ?
                    AND status = 'Pending'
                    """;

            try (PreparedStatement ps = c.prepareStatement(updateRequestSql)) {

                ps.setString(1, responseNote);
                ps.setInt(2, managerId);
                ps.setLong(3, requestId);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException(
                            "Could not update stock request status."
                    );
                }
            }

            return null;
        });
    }

    public void rejectRequest(long requestId, int managerId, String reason) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE stock_requests SET status = 'Rejected', resolved_by = ?, response_note = ?, "
                   + "resolved_at = datetime('now','localtime') WHERE id = ? AND status = 'Pending'")) {
            ps.setInt(1, managerId);
            ps.setString(2, reason);
            ps.setLong(3, requestId);
            if (ps.executeUpdate() == 0) {
                throw new IllegalStateException("This request has already been handled.");
            }
        }
    }
}
