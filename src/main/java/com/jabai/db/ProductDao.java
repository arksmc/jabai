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
                list.add(new Product(rs.getInt("id"), rs.getString("name"), rs.getString("category"),
                        rs.getString("description"), rs.getDouble("price"), rs.getString("unit")));
            }
        }
        return list;
    }

    public void add(String name, String category, String description, double price, String unit) throws SQLException {
        String sql = "INSERT INTO products(name, category, description, price, unit) VALUES (?,?,?,?,?)";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setString(3, description);
            ps.setDouble(4, price);
            ps.setString(5, unit);
            ps.executeUpdate();
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