package com.jabai.db;

import com.jabai.model.SaleLine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleDao {

    public record Header(int locationId, String status) {}
    public record Item(int productId, double quantity) {}

    public long insertSale(Connection c, int locationId, int userId,
                           long totalCentavos, long tenderedCentavos, long changeCentavos) throws SQLException {
        String sql = "INSERT INTO sales(location_id, user_id, total_centavos, "
                   + "cash_tendered_centavos, change_given_centavos) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, locationId);
            ps.setInt(2, userId);
            ps.setLong(3, totalCentavos);
            ps.setLong(4, tenderedCentavos);
            ps.setLong(5, changeCentavos);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void insertItem(Connection c, long saleId, SaleLine line) throws SQLException {
        String sql = "INSERT INTO sale_items(sale_id, product_id, quantity, unit_price_centavos) VALUES (?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, saleId);
            ps.setInt(2, line.productId());
            ps.setDouble(3, line.quantity());
            ps.setLong(4, line.unitPriceCentavos());
            ps.executeUpdate();
        }
    }

    public Optional<Header> findHeader(Connection c, long saleId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT location_id, status FROM sales WHERE id = ?")) {
            ps.setLong(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new Header(rs.getInt(1), rs.getString(2))) : Optional.empty();
            }
        }
    }

    public List<Item> findItems(Connection c, long saleId) throws SQLException {
        List<Item> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT product_id, quantity FROM sale_items WHERE sale_id = ?")) {
            ps.setLong(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Item(rs.getInt(1), rs.getDouble(2)));
            }
        }
        return out;
    }

    /** Returns false if the sale was already voided (or doesn't exist). */
    public boolean markVoided(Connection c, long saleId, int userId, String reason) throws SQLException {
        String sql = "UPDATE sales SET status = 'Voided', voided_at = datetime('now','localtime'), "
                   + "voided_by = ?, void_reason = ? WHERE id = ? AND status = 'Completed'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, reason);
            ps.setLong(3, saleId);
            return ps.executeUpdate() == 1;
        }
        
        
    }
    
    public long completedTotal(int locationId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_centavos), 0) FROM sales "
                   + "WHERE location_id = ? AND status = 'Completed'";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }
}