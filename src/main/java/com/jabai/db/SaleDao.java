/*
 * this file saves sales and their items and reads them back for the sale history, with optional filters by outlet and cashier. it also marks a sale as voided when a clerk cancels it.
 */

package com.jabai.db;

import com.jabai.model.FilterOption;
import com.jabai.model.SaleItemView;
import com.jabai.model.SaleLine;
import com.jabai.model.SaleSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
                while (rs.next()) {
                    out.add(new Item(rs.getInt(1), rs.getDouble(2)));
                }
            }
        }
        return out;
    }

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

    public List<SaleSummary> findByLocation(int locationId) throws SQLException {
        List<SaleSummary> sales = new ArrayList<>();

        String sql = """
            SELECT s.id, l.name AS location_name, u.full_name AS cashier_name,
                   s.total_centavos, s.cash_tendered_centavos, s.change_given_centavos,
                   s.created_at
            FROM sales s
            JOIN locations l ON s.location_id = l.id
            JOIN users u ON s.user_id = u.id
            WHERE s.location_id = ?
            ORDER BY s.created_at DESC
            """;

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locationId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sales.add(new SaleSummary(
                        rs.getLong("id"),
                        rs.getString("location_name"),
                        rs.getString("cashier_name"),
                        rs.getLong("total_centavos"),
                        rs.getLong("cash_tendered_centavos"),
                        rs.getLong("change_given_centavos"),
                        rs.getString("created_at")
                    ));
                }
            }
        }

        return sales;
    }

    public List<SaleSummary> findFiltered(Integer locationId, Integer clerkId,
                                          String fromDate, String toDate) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT s.id, l.name AS location_name, u.full_name AS cashier_name,
                   s.total_centavos, s.cash_tendered_centavos, s.change_given_centavos,
                   s.created_at, s.status, s.void_reason
            FROM sales s
            JOIN locations l ON s.location_id = l.id
            JOIN users u ON s.user_id = u.id
            WHERE 1 = 1
            """);
        List<Object> params = new ArrayList<>();

        if (locationId != null) {
            sql.append(" AND s.location_id = ?");
            params.add(locationId);
        }
        if (clerkId != null) {
            sql.append(" AND s.user_id = ?");
            params.add(clerkId);
        }
        if (fromDate != null) {
            sql.append(" AND date(s.created_at) >= ?");
            params.add(fromDate);
        }
        if (toDate != null) {
            sql.append(" AND date(s.created_at) <= ?");
            params.add(toDate);
        }
        sql.append(" ORDER BY s.created_at DESC, s.id DESC");

        List<SaleSummary> sales = new ArrayList<>();

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sales.add(new SaleSummary(
                        rs.getLong("id"),
                        rs.getString("location_name"),
                        rs.getString("cashier_name"),
                        rs.getLong("total_centavos"),
                        rs.getLong("cash_tendered_centavos"),
                        rs.getLong("change_given_centavos"),
                        rs.getString("created_at"),
                        rs.getString("status"),
                        rs.getString("void_reason")
                    ));
                }
            }
        }

        return sales;
    }

    public List<FilterOption> findOutletOptions() throws SQLException {
        String sql = """
            SELECT id, name FROM locations
            WHERE id IN (SELECT DISTINCT location_id FROM users
                         WHERE role = 'CLERK' AND location_id IS NOT NULL)
            ORDER BY name
            """;
        List<FilterOption> out = new ArrayList<>();

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new FilterOption(rs.getInt("id"), rs.getString("name"), null));
            }
        }

        return out;
    }

    public List<FilterOption> findCashierOptions() throws SQLException {
        String sql = "SELECT id, full_name, location_id FROM users WHERE role = 'CLERK' ORDER BY full_name";
        List<FilterOption> out = new ArrayList<>();

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new FilterOption(rs.getInt("id"), rs.getString("full_name"), rs.getInt("location_id")));
            }
        }

        return out;
    }

    public List<SaleItemView> findItems(long saleId) throws SQLException {
        List<SaleItemView> items = new ArrayList<>();

        String sql = """
            SELECT p.code AS product_code, p.name AS product_name, p.unit,
                   si.quantity, si.unit_price_centavos
            FROM sale_items si
            JOIN products p ON si.product_id = p.id
            WHERE si.sale_id = ?
            ORDER BY p.name
            """;

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, saleId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new SaleItemView(
                        rs.getString("product_code"),
                        rs.getString("product_name"),
                        rs.getString("unit"),
                        rs.getDouble("quantity"),
                        rs.getLong("unit_price_centavos")
                    ));
                }
            }
        }

        return items;
    }
}