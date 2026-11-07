package com.jabai.db;

import java.sql.*;

public class Database {
    private static final String URL = "jdbc:sqlite:bagsakan.db";

    public static Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    public static void init() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    category TEXT NOT NULL,
                    description TEXT,
                    price REAL NOT NULL,
                    unit TEXT NOT NULL
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS locations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    type TEXT NOT NULL
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS inventory (
                    location_id INTEGER NOT NULL REFERENCES locations(id),
                    product_id INTEGER NOT NULL REFERENCES products(id),
                    quantity REAL NOT NULL DEFAULT 0,
                    PRIMARY KEY (location_id, product_id)
                )""");
            seed(c);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static void seed(Connection c) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM products")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }

        try (Statement st = c.createStatement()) {
            st.executeUpdate("INSERT INTO locations(name, type) VALUES ('Main Warehouse', 'WAREHOUSE'), ('Outlet 1', 'OUTLET')");
        }

        String sql = "INSERT INTO products(name, category, description, price, unit) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            add(ps, "Sinandomeng", "Rice Grains", "White rice variety commonly consumed in Filipino households.", 42.00, "kg");
            add(ps, "Jasmine", "Rice Grains", "Aromatic rice known for its fragrant aroma and soft texture.", 55.00, "kg");
            add(ps, "Rambutan", "Fruits", "Tropical fruit with a hairy outer skin and sweet, juicy flesh.", 80.00, "kg");
            add(ps, "Bangus", "Fish", "Milkfish commonly prepared through frying, grilling, or in soups.", 100.00, "kg");
        }
    }

    private static void add(PreparedStatement ps, String n, String cat, String d, double p, String u) throws SQLException {
        ps.setString(1, n); ps.setString(2, cat); ps.setString(3, d);
        ps.setDouble(4, p); ps.setString(5, u);
        ps.executeUpdate();
    }
}