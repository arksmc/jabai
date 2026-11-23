package com.jabai.db;

import com.jabai.util.PasswordUtil;
import com.jabai.util.Barcode;

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
	        s.execute("PRAGMA busy_timeout = 5000");
	    }
	    return c;
	}

	public static void init() {
	    try (Connection c = connect(); Statement s = c.createStatement()) {
	    	s.execute("PRAGMA journal_mode = WAL");
	        s.execute("""
	            CREATE TABLE IF NOT EXISTS products (
	                id INTEGER PRIMARY KEY AUTOINCREMENT,
	                code TEXT NOT NULL UNIQUE COLLATE NOCASE,
	                name TEXT NOT NULL,
	                category TEXT NOT NULL,
	                unit TEXT NOT NULL,
	                status TEXT NOT NULL DEFAULT 'Active' CHECK (status IN ('Active', 'Inactive')),
	                price_centavos INTEGER NOT NULL CHECK (price_centavos >= 0),
	                cost_price_centavos INTEGER NOT NULL DEFAULT 0 CHECK (cost_price_centavos >= 0),
	                reorder_level REAL NOT NULL DEFAULT 0 CHECK (reorder_level >= 0)
	            )""");
	        s.execute("""
	            CREATE TABLE IF NOT EXISTS locations (
	                id INTEGER PRIMARY KEY AUTOINCREMENT,
	                name TEXT NOT NULL UNIQUE COLLATE NOCASE,
	                type TEXT NOT NULL CHECK (type IN ('WAREHOUSE', 'OUTLET'))
	            )""");
	        s.execute("""
	            CREATE TABLE IF NOT EXISTS inventory (
	                location_id INTEGER NOT NULL REFERENCES locations(id),
	                product_id INTEGER NOT NULL REFERENCES products(id),
	                quantity REAL NOT NULL DEFAULT 0 CHECK (quantity >= 0),
	                PRIMARY KEY (location_id, product_id)
	            )""");
	        s.execute("""
	            CREATE TABLE IF NOT EXISTS users (
	                id INTEGER PRIMARY KEY AUTOINCREMENT,
	                username TEXT NOT NULL UNIQUE COLLATE NOCASE,
	                password_hash TEXT NOT NULL,
	                full_name TEXT NOT NULL,
	                role TEXT NOT NULL CHECK (role IN ('MANAGER', 'CLERK')),
	                location_id INTEGER REFERENCES locations(id),
	                status TEXT NOT NULL DEFAULT 'Active' CHECK (status IN ('Active', 'Inactive')),
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
	                total_centavos INTEGER NOT NULL CHECK (total_centavos >= 0),
	                cash_tendered_centavos INTEGER NOT NULL,
	                change_given_centavos INTEGER NOT NULL,
	                created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
	                CHECK (cash_tendered_centavos >= total_centavos),
	                CHECK (change_given_centavos = cash_tendered_centavos - total_centavos)
	            )""");
	        s.execute("""
	            CREATE TABLE IF NOT EXISTS sale_items (
	                id INTEGER PRIMARY KEY AUTOINCREMENT,
	                sale_id INTEGER NOT NULL REFERENCES sales(id),
	                product_id INTEGER NOT NULL REFERENCES products(id),
	                quantity REAL NOT NULL CHECK (quantity > 0),
	                unit_price_centavos INTEGER NOT NULL CHECK (unit_price_centavos >= 0)
	            )""");
	        s.execute("""
	        	    CREATE TABLE IF NOT EXISTS stock_movements (
	        	        id INTEGER PRIMARY KEY AUTOINCREMENT,
	        	        product_id INTEGER NOT NULL REFERENCES products(id),
	        	        location_id INTEGER NOT NULL REFERENCES locations(id),
	        	        delta REAL NOT NULL CHECK (delta <> 0),
	        	        reason TEXT NOT NULL,
	        	        reference_id INTEGER,
	        	        user_id INTEGER NOT NULL REFERENCES users(id),
	        	        created_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
	        	    )""");
	        	s.execute("""
	        	    CREATE TABLE IF NOT EXISTS stock_orders (
	        	        id INTEGER PRIMARY KEY AUTOINCREMENT,
	        	        supplier TEXT NOT NULL,
	        	        status TEXT NOT NULL DEFAULT 'Ordered' CHECK (status IN ('Ordered', 'Received', 'Cancelled')),
	        	        ordered_by INTEGER NOT NULL REFERENCES users(id),
	        	        notes TEXT,
	        	        created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
	        	        received_at TEXT
	        	    )""");
	        	s.execute("""
	        	    CREATE TABLE IF NOT EXISTS stock_order_items (
	        	        id INTEGER PRIMARY KEY AUTOINCREMENT,
	        	        order_id INTEGER NOT NULL REFERENCES stock_orders(id),
	        	        product_id INTEGER NOT NULL REFERENCES products(id),
	        	        quantity REAL NOT NULL CHECK (quantity > 0),
	        	        unit_cost_centavos INTEGER NOT NULL DEFAULT 0 CHECK (unit_cost_centavos >= 0)
	        	    )""");
	        	s.execute("""
	        	    CREATE TABLE IF NOT EXISTS stock_requests (
	        	        id INTEGER PRIMARY KEY AUTOINCREMENT,
	        	        location_id INTEGER NOT NULL REFERENCES locations(id),
	        	        requested_by INTEGER NOT NULL REFERENCES users(id),
	        	        status TEXT NOT NULL DEFAULT 'Pending' CHECK (status IN ('Pending', 'Fulfilled', 'Rejected')),
	        	        note TEXT,
	        	        response_note TEXT,
	        	        created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
	        	        resolved_at TEXT,
	        	        resolved_by INTEGER REFERENCES users(id)
	        	    )""");
	        	s.execute("""
	        	    CREATE TABLE IF NOT EXISTS stock_request_items (
	        	        id INTEGER PRIMARY KEY AUTOINCREMENT,
	        	        request_id INTEGER NOT NULL REFERENCES stock_requests(id),
	        	        product_id INTEGER NOT NULL REFERENCES products(id),
	        	        quantity_requested REAL NOT NULL CHECK (quantity_requested > 0),
	        	        quantity_sent REAL NOT NULL DEFAULT 0 CHECK (quantity_sent >= 0),
	        	        UNIQUE (request_id, product_id)
	        	    )""");
	        s.execute("CREATE INDEX IF NOT EXISTS idx_movements_lookup ON stock_movements(product_id, location_id, created_at)");
	        s.execute("CREATE INDEX IF NOT EXISTS idx_order_items_order ON stock_order_items(order_id)");
	        s.execute("CREATE INDEX IF NOT EXISTS idx_request_items_request ON stock_request_items(request_id)");
	        s.execute("CREATE INDEX IF NOT EXISTS idx_requests_status ON stock_requests(status, location_id)");   
	        s.execute("CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items(sale_id)");
	        s.execute("CREATE INDEX IF NOT EXISTS idx_sale_items_product ON sale_items(product_id)");
	        s.execute("CREATE INDEX IF NOT EXISTS idx_sales_location_date ON sales(location_id, created_at)");
	        s.execute("CREATE INDEX IF NOT EXISTS idx_inventory_product ON inventory(product_id)");

	        seed(c);
	        seedStock(c);
	        seedUsers(c);
	    } catch (SQLException e) {
	        throw new IllegalStateException("Database setup failed", e);
	    }
	}

	private static void seed(Connection c) throws SQLException {
		try (Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM products")) {
			if (rs.next() && rs.getInt(1) > 0) return;
		}

		try (Statement st = c.createStatement()) {
			st.executeUpdate("INSERT OR IGNORE INTO locations(name, type) VALUES "
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

		String sql = "INSERT INTO products(code, name, category, unit, status, price_centavos, cost_price_centavos, reorder_level) "
		        + "VALUES (?,?,?,?,?,?,?,?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
		    for (int i = 0; i < items.length; i++) {
		        String[] it = items[i];
		        long priceCentavos = Long.parseLong(it[3]) * 100;
		        ps.setString(1, Barcode.ean13(i + 1));
		        ps.setString(2, it[1]);
		        ps.setString(3, it[0]);
		        ps.setString(4, it[2]);
		        ps.setString(5, "Active");
		        ps.setLong(6, priceCentavos);
		        ps.setLong(7, priceCentavos * 80 / 100); // placeholder cost = 80% of price
		        ps.setDouble(8, 50);                     // placeholder reorder level
		        ps.executeUpdate();
		    }
		}
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