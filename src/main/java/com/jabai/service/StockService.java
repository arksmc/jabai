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
     * The manager chooses the quantity per item (0 = don't send that item).
     * Everything happens in one transaction: if the warehouse is short on any item, nothing moves.
     */
    public void fulfillRequest(long requestId, int managerId, List<SendLine> sends,
                               String responseNote) throws SQLException {
        List<SendLine> toSend = sends.stream().filter(s -> s.quantity() > 0).toList();
        if (toSend.isEmpty()) {
            throw new IllegalArgumentException("Nothing to send. Reject the request instead.");
        }
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE stock_requests SET status = 'Fulfilled', resolved_by = ?, response_note = ?, "
                  + "resolved_at = datetime('now','localtime') WHERE id = ? AND status = 'Pending'")) {
                ps.setInt(1, managerId);
                ps.setString(2, responseNote);
                ps.setLong(3, requestId);
                if (ps.executeUpdate() == 0) {
                    throw new IllegalStateException("This request has already been handled.");
                }
            }

            int outletId = requestOutlet(c, requestId);
            int warehouseId = locationDao.findWarehouseId(c);

            for (SendLine send : toSend) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE stock_request_items SET quantity_sent = ? "
                      + "WHERE request_id = ? AND product_id = ?")) {
                    ps.setDouble(1, send.quantity());
                    ps.setLong(2, requestId);
                    ps.setInt(3, send.productId());
                    if (ps.executeUpdate() == 0) {
                        throw new IllegalArgumentException(
                                "Product " + send.productId() + " is not part of this request.");
                    }
                }
                inventoryDao.decrease(c, warehouseId, send.productId(), send.quantity(),
                        MovementReason.TRANSFER_OUT, requestId, managerId);
                inventoryDao.increase(c, outletId, send.productId(), send.quantity(),
                        MovementReason.TRANSFER_IN, requestId, managerId);
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

    private int requestOutlet(Connection c, long requestId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT location_id FROM stock_requests WHERE id = ?")) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                throw new IllegalArgumentException("Request not found.");
            }
        }
    }
}