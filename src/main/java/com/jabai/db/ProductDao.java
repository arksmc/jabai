package com.jabai.db;

import com.jabai.model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

	public List<Product> findAll() throws SQLException {
	    List<Product> list = new ArrayList<>();
	    String sql = "SELECT id, code, name, category, unit, status, "
	               + "price_centavos, cost_price_centavos, reorder_level "
	               + "FROM products ORDER BY category, name";
	    try (Connection c = Database.connect();
	         Statement s = c.createStatement();
	         ResultSet rs = s.executeQuery(sql)) {
	        while (rs.next()) {
	            list.add(new Product(
	                    rs.getInt("id"), rs.getString("code"), rs.getString("name"),
	                    rs.getString("category"), rs.getString("unit"), rs.getString("status"),
	                    rs.getLong("price_centavos"), rs.getLong("cost_price_centavos"),
	                    rs.getDouble("reorder_level")));
	        }
	    }
	    return list;
	}

	public void add(Product p) throws SQLException {
	    String sql = "INSERT INTO products(code, name, category, unit, status, "
	               + "price_centavos, cost_price_centavos, reorder_level) VALUES (?,?,?,?,?,?,?,?)";
	    try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
	        ps.setString(1, p.getCode());
	        ps.setString(2, p.getName());
	        ps.setString(3, p.getCategory());
	        ps.setString(4, p.getUnit());
	        ps.setString(5, p.getStatus());
	        ps.setLong(6, p.getPriceCentavos());
	        ps.setLong(7, p.getCostPriceCentavos());
	        ps.setDouble(8, p.getReorderLevel());
	        ps.executeUpdate();
	    }
	}

    public boolean codeExists(String code) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM products WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COALESCE(SUM(quantity), 0) FROM inventory WHERE product_id = ?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getDouble(1) > 0) {
                            throw new SQLException("Cannot remove: this product still has stock in inventory.");
                        }
                    }
                }

                // Anything that references this product means we must keep the row.
                if (hasHistory(c, id)) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE products SET status = 'Inactive' WHERE id = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                    c.commit();
                    return false;
                }

                try (PreparedStatement ps = c.prepareStatement("DELETE FROM inventory WHERE product_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** True if the product appears in any sale, stock order, stock request or stock movement. */
    private boolean hasHistory(Connection c, int productId) throws SQLException {
        String sql = """
            SELECT EXISTS (SELECT 1 FROM sale_items WHERE product_id = ?)
                OR EXISTS (SELECT 1 FROM stock_order_items WHERE product_id = ?)
                OR EXISTS (SELECT 1 FROM stock_request_items WHERE product_id = ?)
                OR EXISTS (SELECT 1 FROM stock_movements WHERE product_id = ?)
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                ps.setInt(i, productId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }
}