package com.jabai.db;

import com.jabai.model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    public List<Product> findAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection c = Database.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM products ORDER BY category, name")) {
            while (rs.next()) {
                list.add(new Product(
                        rs.getInt("id"), rs.getString("code"), rs.getString("name"),
                        rs.getString("category"), rs.getString("unit"), rs.getString("status"),
                        rs.getDouble("price"), rs.getDouble("cost_price"), rs.getDouble("reorder_level")));
            }
        }
        return list;
    }

    public void add(Product p) throws SQLException {
        String sql = "INSERT INTO products(code, name, category, unit, status, price, cost_price, reorder_level) "
                   + "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getCode());
            ps.setString(2, p.getName());
            ps.setString(3, p.getCategory());
            ps.setString(4, p.getUnit());
            ps.setString(5, p.getStatus());
            ps.setDouble(6, p.getPrice());
            ps.setDouble(7, p.getCostPrice());
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

    public void delete(int id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}