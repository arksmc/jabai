package com.jabai.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InventoryDao {

    /** Subtracts stock. Fails (and the caller rolls back) if there isn't enough. */
    public void decrease(Connection c, int locationId, int productId, double qty) throws SQLException {
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
    }

    /** Adds stock, creating the inventory row if this product has none at this location yet. */
    public void increase(Connection c, int locationId, int productId, double qty) throws SQLException {
        String sql = "INSERT INTO inventory(location_id, product_id, quantity) VALUES (?,?,?) "
                   + "ON CONFLICT(location_id, product_id) "
                   + "DO UPDATE SET quantity = quantity + excluded.quantity";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            ps.setInt(2, productId);
            ps.setDouble(3, qty);
            ps.executeUpdate();
        }
    }
}