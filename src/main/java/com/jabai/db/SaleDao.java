package com.jabai.db;

import com.jabai.model.SaleLine;

import java.sql.*;

public class SaleDao {

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
}