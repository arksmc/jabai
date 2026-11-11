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
                    code TEXT NOT NULL UNIQUE,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    unit TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'Active',
                    price REAL NOT NULL,
                    cost_price REAL,
                    reorder_level REAL
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
            st.executeUpdate("INSERT INTO locations(name, type) VALUES "
                    + "('Main Warehouse', 'WAREHOUSE'), ('Outlet 1', 'OUTLET')");
        }

        // {category, name, unit, price}
        String[][] items = {
            {"Rice Grains", "Sinandomeng", "kg", "42"}, {"Rice Grains", "Jasmine", "kg", "55"},
            {"Rice Grains", "Pandan", "kg", "50"}, {"Rice Grains", "Dinorado", "kg", "55"},
            {"Rice Grains", "Brown Rice", "kg", "65"}, {"Rice Grains", "Black Rice", "kg", "100"},

            {"Fruits", "Rambutan", "kg", "80"}, {"Fruits", "Banana", "kg", "45"},
            {"Fruits", "Lanzones", "kg", "100"}, {"Fruits", "Guyabano", "kg", "80"},
            {"Fruits", "Dayap", "kg", "100"}, {"Fruits", "Papaya", "kg", "35"},

            {"Root Crops", "Ube", "kg", "80"}, {"Root Crops", "Kamote", "kg", "45"},

            {"Vegetables", "Talong", "kg", "50"}, {"Vegetables", "Upo", "kg", "35"},
            {"Vegetables", "Bawang", "kg", "120"}, {"Vegetables", "Sibuyas", "kg", "100"},
            {"Vegetables", "Ampalaya", "kg", "60"}, {"Vegetables", "Kamatis", "kg", "60"},
            {"Vegetables", "Sitaw", "kg", "60"},

            {"Fish", "Bangus", "kg", "100"}, {"Fish", "Tilapia", "kg", "140"},
            {"Fish", "Galunggong", "kg", "160"}, {"Fish", "Maya-maya", "kg", "350"},
            {"Fish", "Lapu-lapu", "kg", "400"}, {"Fish", "Tulingan", "kg", "180"},

            {"Dairy Products", "Cheese", "kg", "250"}, {"Dairy Products", "Milk", "L", "90"},
            {"Dairy Products", "Butter", "kg", "300"}, {"Dairy Products", "Yogurt", "500g", "100"},

            {"Planting Seeds", "Sunflower Seeds", "packet", "40"},
            {"Planting Seeds", "Sili Labuyo Seeds", "packet", "30"},
            {"Planting Seeds", "Pechay Seeds", "packet", "25"},
            {"Planting Seeds", "Ampalaya Seeds", "packet", "35"},
            {"Planting Seeds", "Kangkong Seeds", "packet", "25"},
            {"Planting Seeds", "Okra Seeds", "packet", "30"},
            {"Planting Seeds", "Kalabasa Seeds", "packet", "30"},

            {"Seafood", "Hipon", "kg", "350"}, {"Seafood", "Oyster", "kg", "250"},
            {"Seafood", "Crab", "kg", "450"}, {"Seafood", "Lobster", "kg", "1200"},
            {"Seafood", "Halaan", "kg", "180"}, {"Seafood", "Tulya", "kg", "200"},
            {"Seafood", "Kabibe", "kg", "180"},

            {"Poultry", "Eggs", "pc", "10"}, {"Poultry", "Chicken Meat", "kg", "190"},
            {"Poultry", "Duck", "kg", "280"}, {"Poultry", "Goose", "kg", "450"},
            {"Poultry", "Turkey", "kg", "400"}, {"Poultry", "Quail", "kg", "350"},

            {"Pork", "Liempo", "kg", "380"}, {"Pork", "Porkchop", "kg", "350"},
            {"Pork", "Pata", "kg", "300"}, {"Pork", "Tenga", "kg", "220"},
            {"Pork", "Mukha", "kg", "200"}, {"Pork", "Pork Feet", "kg", "180"},
            {"Pork", "Pork Skin", "kg", "150"},

            {"Beef", "Shank", "kg", "350"}, {"Beef", "T-bone", "kg", "650"},
            {"Beef", "Brisket", "kg", "450"}, {"Beef", "Loin", "kg", "600"},
            {"Beef", "Sirloin", "kg", "550"}, {"Beef", "Tenderloin", "kg", "900"}
        };

        String sql = "INSERT INTO products(code, name, category, unit, status, price, cost_price, reorder_level) "
                   + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < items.length; i++) {
                String[] it = items[i];
                double price = Double.parseDouble(it[3]);
                ps.setString(1, String.format("PRD-%03d", i + 1));
                ps.setString(2, it[1]);
                ps.setString(3, it[0]);
                ps.setString(4, it[2]);
                ps.setString(5, "Active");
                ps.setDouble(6, price);
                ps.setDouble(7, Math.round(price * 0.8 * 100) / 100.0); // placeholder cost = 80% of price
                ps.setDouble(8, 50);                                     // placeholder reorder level
                ps.executeUpdate();
            }
        }
    }

    private static void add(PreparedStatement ps, String code, String name, String cat,
                            String unit, double price, double cost, double reorder) throws SQLException {
        ps.setString(1, code);
        ps.setString(2, name);
        ps.setString(3, cat);
        ps.setString(4, unit);
        ps.setString(5, "Active");
        ps.setDouble(6, price);
        ps.setDouble(7, cost);
        ps.setDouble(8, reorder);
        ps.executeUpdate();
    }
}