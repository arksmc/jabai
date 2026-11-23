package com.jabai.service;

import com.jabai.db.Database;
import com.jabai.db.InventoryDao;
import com.jabai.db.LocationDao;
import com.jabai.db.ProductDao;
import com.jabai.db.StockRequestDao;
import com.jabai.model.Manager;
import com.jabai.model.MovementReason;
import com.jabai.model.OrderLine;
import com.jabai.model.Product;
import com.jabai.model.RequestItemView;
import com.jabai.model.RequestLine;
import com.jabai.model.SendLine;
import com.jabai.model.StockRequestSummary;
import com.jabai.model.User;
import com.jabai.util.Format;
import com.jabai.model.StockRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class StockService {

    private static final Set<String> WHOLE_UNITS = Set.of("pc", "packet");
    private static final int MAX_NOTE_LENGTH = 300;

    private final InventoryDao inventoryDao = new InventoryDao();
    private final LocationDao locationDao = new LocationDao();
    private final ProductDao productDao = new ProductDao();

    @FunctionalInterface
    private interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    private static boolean isWholeUnit(String unit) {
        return unit != null && WHOLE_UNITS.contains(unit.trim().toLowerCase());
    }

    private static void requireManager(User actor) {
        if (!(actor instanceof Manager)) {
            throw new SecurityException("Only a manager can do this.");
        }
    }

    private static String cleanNote(String text, boolean required, String label) {
        String t = text == null ? "" : text.trim();
        if (required && t.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        if (t.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException(label + " must be at most " + MAX_NOTE_LENGTH + " characters.");
        }
        return t.isEmpty() ? null : t;
    }

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

    public void receiveOrder(long orderId, int userId) throws SQLException {
        inTransaction(c -> {
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

    public long createRequest(User actor, int outletId, String note,
                              List<RequestLine> lines) throws SQLException {
        if (actor == null || !actor.canAccessLocation(outletId)) {
            throw new SecurityException("You cannot request stock for this outlet.");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Request has no items.");
        }
        String trimmedNote = cleanNote(note, false, "Note");

        Map<Integer, Double> merged = new LinkedHashMap<>();
        for (RequestLine l : lines) {
            if (l == null || !Double.isFinite(l.quantity()) || l.quantity() <= 0) {
                throw new IllegalArgumentException("Every quantity must be greater than zero.");
            }
            merged.merge(l.productId(), l.quantity(), Double::sum);
        }

        return inTransaction(c -> {
            int warehouseId = locationDao.findWarehouseId(c);
            if (outletId == warehouseId) {
                throw new IllegalArgumentException("The Main Warehouse cannot request stock from itself.");
            }

            for (Map.Entry<Integer, Double> e : merged.entrySet()) {
                Optional<Product> found = productDao.findById(c, e.getKey());
                if (found.isEmpty()) {
                    throw new IllegalArgumentException("A selected product no longer exists.");
                }
                Product p = found.get();
                if (!"Active".equalsIgnoreCase(p.getStatus())) {
                    throw new IllegalArgumentException(p.getName() + " is inactive and cannot be requested.");
                }
                double qty = e.getValue();
                if (isWholeUnit(p.getUnit()) && qty != Math.rint(qty)) {
                    throw new IllegalArgumentException(
                            p.getName() + " is counted by the " + p.getUnit() + " (whole numbers only).");
                }
            }

            long requestId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO stock_requests(location_id, requested_by, note) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, outletId);
                ps.setInt(2, actor.getId());
                ps.setString(3, trimmedNote);
                requestId = insertAndGetId(ps);
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO stock_request_items(request_id, product_id, quantity_requested) VALUES (?,?,?)")) {
                for (Map.Entry<Integer, Double> e : merged.entrySet()) {
                    ps.setLong(1, requestId);
                    ps.setInt(2, e.getKey());
                    ps.setDouble(3, e.getValue());
                    ps.executeUpdate();
                }
            }
            return requestId;
        });
    }

    public List<StockRequestSummary> requestsForLocation(User actor, int locationId) throws SQLException {
        if (actor == null || !actor.canAccessLocation(locationId)) {
            throw new SecurityException("You cannot view requests for this outlet.");
        }
        return StockRequestDao.findByLocation(locationId);
    }

    public List<RequestItemView> requestItems(User actor, long requestId) throws SQLException {
        int locationId;
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT location_id FROM stock_requests WHERE id = ?")) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Stock request not found.");
                }
                locationId = rs.getInt(1);
            }
        }
        if (actor == null || !actor.canAccessLocation(locationId)) {
            throw new SecurityException("You cannot view this request.");
        }
        return StockRequestDao.findItems(requestId);
    }
    
    public List<StockRequestSummary> allRequests(User actor) throws SQLException {
        requireManager(actor);
        return StockRequestDao.findAll();
    }

    public Map<Integer, Double> warehouseStock(User actor) throws SQLException {
        requireManager(actor);
        int warehouseId;
        try (Connection c = Database.connect()) {
            warehouseId = locationDao.findWarehouseId(c);
        }
        Map<Integer, Double> stock = new java.util.HashMap<>();
        for (StockRow row : inventoryDao.findStock(warehouseId)) {
            stock.put(row.productId(), row.quantity());
        }
        return stock;
    }

    public void fulfillRequest(User actor, long requestId, List<SendLine> lines,
                               String responseNote) throws SQLException {
        requireManager(actor);
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Send at least one item.");
        }
        String trimmedNote = cleanNote(responseNote, false, "Response note");
        int managerId = actor.getId();

        Set<Integer> seen = new HashSet<>();
        for (SendLine line : lines) {
            if (line == null || !Double.isFinite(line.quantity()) || line.quantity() <= 0) {
                throw new IllegalArgumentException("Sent quantities must be greater than zero.");
            }
            if (!seen.add(line.productId())) {
                throw new IllegalArgumentException("A product can only appear once in a fulfillment.");
            }
        }

        inTransaction(c -> {
            int outletId;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT location_id, status FROM stock_requests WHERE id = ?")) {
                ps.setLong(1, requestId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Stock request not found.");
                    }
                    outletId = rs.getInt("location_id");
                    if (!"Pending".equals(rs.getString("status"))) {
                        throw new IllegalStateException("This request has already been handled.");
                    }
                }
            }

            int warehouseId = locationDao.findWarehouseId(c);

            for (SendLine line : lines) {
                double requested;
                String productName;
                String unit;
                try (PreparedStatement ps = c.prepareStatement("""
                        SELECT sri.quantity_requested, p.name, p.unit
                        FROM stock_request_items sri
                        JOIN products p ON p.id = sri.product_id
                        WHERE sri.request_id = ? AND sri.product_id = ?
                        """)) {
                    ps.setLong(1, requestId);
                    ps.setInt(2, line.productId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("A selected product does not belong to this request.");
                        }
                        requested = rs.getDouble(1);
                        productName = rs.getString(2);
                        unit = rs.getString(3);
                    }
                }

                if (isWholeUnit(unit) && line.quantity() != Math.rint(line.quantity())) {
                    throw new IllegalArgumentException(
                            productName + " is counted by the " + unit + " (whole numbers only).");
                }
                if (line.quantity() > requested) {
                    throw new IllegalArgumentException("You can send at most " + Format.qty(requested)
                            + " " + unit + " of " + productName + ".");
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE stock_request_items SET quantity_sent = ? WHERE request_id = ? AND product_id = ?")) {
                    ps.setDouble(1, line.quantity());
                    ps.setLong(2, requestId);
                    ps.setInt(3, line.productId());
                    if (ps.executeUpdate() != 1) {
                        throw new IllegalStateException("Could not update " + productName + ".");
                    }
                }

                inventoryDao.decrease(c, warehouseId, line.productId(), line.quantity(),
                        MovementReason.TRANSFER_OUT, requestId, managerId);
                inventoryDao.increase(c, outletId, line.productId(), line.quantity(),
                        MovementReason.TRANSFER_IN, requestId, managerId);
            }

            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE stock_requests
                    SET status = 'Fulfilled',
                        response_note = ?,
                        resolved_at = datetime('now','localtime'),
                        resolved_by = ?
                    WHERE id = ? AND status = 'Pending'
                    """)) {
                ps.setString(1, trimmedNote);
                ps.setInt(2, managerId);
                ps.setLong(3, requestId);
                if (ps.executeUpdate() != 1) {
                    throw new IllegalStateException("This request has already been handled.");
                }
            }
            return null;
        });
    }

    public void rejectRequest(User actor, long requestId, String reason) throws SQLException {
        requireManager(actor);
        String trimmedReason = cleanNote(reason, true, "A reason");
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE stock_requests SET status = 'Rejected', resolved_by = ?, response_note = ?, "
                   + "resolved_at = datetime('now','localtime') WHERE id = ? AND status = 'Pending'")) {
            ps.setInt(1, actor.getId());
            ps.setString(2, trimmedReason);
            ps.setLong(3, requestId);
            if (ps.executeUpdate() == 0) {
                throw new IllegalStateException("This request has already been handled.");
            }
        }
    }
}