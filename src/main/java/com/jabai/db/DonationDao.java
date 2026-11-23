package com.jabai.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import com.jabai.model.DonationRow;
import java.util.ArrayList;
import java.util.List;

public class DonationDao {

    public record Header(int locationId, String type, String status, Integer productId, double quantity) {}

    public long insertCash(Connection c, String donorName, long amountCentavos,
                           int locationId, int userId) throws SQLException {
        String sql = "INSERT INTO donations(donor_name, type, amount_centavos, location_id, user_id) "
                   + "VALUES (?, 'CASH', ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, donorName);
            ps.setLong(2, amountCentavos);
            ps.setInt(3, locationId);
            ps.setInt(4, userId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public long insertItem(Connection c, String donorName, int productId, double quantity,
                           int locationId, int userId) throws SQLException {
        String sql = "INSERT INTO donations(donor_name, type, product_id, quantity, location_id, user_id) "
                   + "VALUES (?, 'ITEM', ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, donorName);
            ps.setInt(2, productId);
            ps.setDouble(3, quantity);
            ps.setInt(4, locationId);
            ps.setInt(5, userId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public Optional<Header> findHeader(Connection c, long donationId) throws SQLException {
        String sql = "SELECT location_id, type, status, product_id, quantity FROM donations WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, donationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                int pid = rs.getInt(4);
                Integer productId = rs.wasNull() ? null : pid;
                return Optional.of(new Header(rs.getInt(1), rs.getString(2), rs.getString(3),
                        productId, rs.getDouble(5)));
            }
        }
    }

    public boolean markVoided(Connection c, long donationId, int userId, String reason) throws SQLException {
        String sql = "UPDATE donations SET status = 'Voided', voided_at = datetime('now','localtime'), "
                   + "voided_by = ?, void_reason = ? WHERE id = ? AND status = 'Completed'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, reason);
            ps.setLong(3, donationId);
            return ps.executeUpdate() == 1;
        }
    }
    
    public long completedCashTotal(int locationId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount_centavos), 0) FROM donations "
                   + "WHERE location_id = ? AND type = 'CASH' AND status = 'Completed'";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    public List<DonationRow> findRecent(int locationId, int limit) throws SQLException {
        String sql = """
            SELECT d.id, d.donor_name, d.type, d.amount_centavos, p.name, p.unit,
                   d.quantity, d.status, d.created_at
            FROM donations d
            LEFT JOIN products p ON p.id = d.product_id
            WHERE d.location_id = ?
            ORDER BY d.created_at DESC, d.id DESC
            LIMIT ?
            """;
        List<DonationRow> rows = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new DonationRow(
                            rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4),
                            rs.getString(5), rs.getString(6), rs.getDouble(7),
                            rs.getString(8), rs.getString(9)));
                }
            }
        }
        return rows;
    }
}