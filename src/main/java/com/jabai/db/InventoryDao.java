package com.jabai.db;

import com.jabai.model.MovementReason;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class InventoryDao {

    /** Subtracts stock and logs it. Throws InsufficientStockException if there isn't enough. */
    public void decrease(Connection c, int locationId, int productId, double qty,
                         MovementReason reason, Long referenceId, int userId) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity - ? "
                   + "WHERE location_id = ? AND product_id = ? AND quantity >= ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, qty);
            ps.setInt(2, locationId);
            ps.setInt(3, productId);
            ps.setDouble(4, qty);
            if (ps.executeUpdate() == 0) {
                throw new InsufficientStockException(productId);
            }
        }
        logMovement(c, productId, locationId, -qty, reason, referenceId, userId);
    }

    /** Adds stock and logs it, creating the inventory row if this product has none here yet. */
    public void increase(Connection c, int locationId, int productId, double qty,
                         MovementReason reason, Long referenceId, int userId) throws SQLException {
        String sql = "INSERT INTO inventory(location_id, product_id, quantity) VALUES (?,?,?) "
                   + "ON CONFLICT(location_id, product_id) "
                   + "DO UPDATE SET quantity = quantity + excluded.quantity";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            ps.setInt(2, productId);
            ps.setDouble(3, qty);
            ps.executeUpdate();
        }
        logMovement(c, productId, locationId, qty, reason, referenceId, userId);
    }

    private void logMovement(Connection c, int productId, int locationId, double delta,
                             MovementReason reason, Long referenceId, int userId) throws SQLException {
        String sql = "INSERT INTO stock_movements(product_id, location_id, delta, reason, reference_id, user_id) "
                   + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, locationId);
            ps.setDouble(3, delta);
            ps.setString(4, reason.name());
            if (referenceId == null) ps.setNull(5, Types.INTEGER);
            else ps.setLong(5, referenceId);
            ps.setInt(6, userId);
            ps.executeUpdate();
        }
    }
}