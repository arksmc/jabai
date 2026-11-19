package com.jabai.db;

import com.jabai.util.PasswordUtil;

import java.sql.*;

public class Database {
	private static final String URL = "jdbc:sqlite:bagsakan.db";
	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

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
			s.execute("""
					CREATE TABLE IF NOT EXISTS users (
						id INTEGER PRIMARY KEY AUTOINCREMENT,
						username TEXT NOT NULL UNIQUE,
						password_hash TEXT NOT NULL,
						full_name TEXT NOT NULL,
						role TEXT NOT NULL CHECK (role IN ('MANAGER', 'CLERK')),
						location_id INTEGER REFERENCES locations(id),
						status TEXT NOT NULL DEFAULT 'Active',
						CHECK (
							(role = 'MANAGER' AND location_id IS NULL) OR
							(role = 'CLERK' AND location_id IS NOT NULL)
						)
				)""");
			s.execute("""
					CREATE TABLE IF NOT EXISTS sales (
						id INTEGER PRIMARY KEY AUTOINCREMENT,
						location_id INTEGER NOT NULL REFERENCES locations(id),
						user_id INTEGER NOT NULL REFERENCES users(id),
						total REAL NOT NULL,
						cash_tendered REAL NOT NULL,
						change_given REAL NOT NULL,
						created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
				)""");
			s.execute("""
					CREATE TABLE IF NOT EXISTS sale_items (
						id INTEGER PRIMARY KEY AUTOINCREMENT,
						sale_id INTEGER NOT NULL REFERENCES sales(id),
						product_id INTEGER NOT NULL REFERENCES products(id),
						quantity REAL NOT NULL,
						unit_price REAL NOT NULL
				)""");
			seed(c);
			seedStock(c);
			seedUsers(c);
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

	private static void add(PreparedStatement ps, String code, String name, String cat, String unit, double price, double cost, double reorder) throws SQLException {
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
	
	private static void seedStock(Connection c) throws SQLException {
		try (Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM inventory")) {
			if (rs.next() && rs.getInt(1) > 0) return;
		}
		try (Statement st = c.createStatement()) {
			// warehouse: 200 of everything
			st.executeUpdate("INSERT INTO inventory(location_id, product_id, quantity) "
					+ "SELECT (SELECT id FROM locations WHERE type = 'WAREHOUSE' LIMIT 1), id, 200 FROM products");
			// outlet: mostly 60, some low (10), some out of stock (0)
			st.executeUpdate("INSERT INTO inventory(location_id, product_id, quantity) "
					+ "SELECT (SELECT id FROM locations WHERE type = 'OUTLET' LIMIT 1), id, "
					+ "CASE WHEN id % 5 = 0 THEN 10 WHEN id % 7 = 0 THEN 0 ELSE 60 END FROM products");
		}
	}

	private static void seedUsers(Connection c) throws SQLException {
		try (Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users")) {
			if (rs.next() && rs.getInt(1) > 0) return;
		}

		Integer outletId = null;
		try (Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("SELECT id FROM locations WHERE type = 'OUTLET' ORDER BY id LIMIT 1")) {
			if (rs.next()) outletId = rs.getInt(1);
		}

		String sql = "INSERT INTO users(username, password_hash, full_name, role, location_id, status) "
				+ "VALUES (?,?,?,?,?,?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, "manager");
			ps.setString(2, PasswordUtil.hash("manager123"));
			ps.setString(3, "Store Manager");
			ps.setString(4, "MANAGER");
			ps.setNull(5, Types.INTEGER);
			ps.setString(6, "Active");
			ps.executeUpdate();

			if (outletId != null) {
				ps.setString(1, "clerk1");
				ps.setString(2, PasswordUtil.hash("clerk123"));
				ps.setString(3, "Outlet 1 Clerk");
				ps.setString(4, "CLERK");
				ps.setInt(5, outletId);
				ps.setString(6, "Active");
				ps.executeUpdate();
			}
		}
	}
}