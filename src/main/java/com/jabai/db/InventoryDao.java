package com.jabai.db;

import com.jabai.model.Product;
import com.jabai.model.MovementReason;
import com.jabai.model.StockRow;
import com.jabai.model.StockedProduct;
import com.jabai.util.Format;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class InventoryDao {

    /** Subtracts stock and logs it. Throws InsufficientStockException if there isn't enough. */
    public void decrease(Connection c, int locationId, int productId, double qty,
                         MovementReason reason, Long referenceId, int userId) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity - ? "
                   + "WHERE location_id = ? AND product_id = ? AND quantity >= ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, qty);
            ps.setInt(2, locationId);
            ps.setInt(3, productId);
            ps.setDouble(4, qty);
            if (ps.executeUpdate() == 0) {
                throw new InsufficientStockException(productId,
                        describeShortage(c, locationId, productId, qty));
            }
        }
        logMovement(c, productId, locationId, -qty, reason, referenceId, userId);
    }

    /** Adds stock and logs it, creating the inventory row if this product has none here yet. */
    public void increase(Connection c, int locationId, int productId, double qty,
                         MovementReason reason, Long referenceId, int userId) throws SQLException {
        String sql = "INSERT INTO inventory(location_id, product_id, quantity) VALUES (?,?,?) "
                   + "ON CONFLICT(location_id, product_id) "
                   + "DO UPDATE SET quantity = quantity + excluded.quantity";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            ps.setInt(2, productId);
            ps.setDouble(3, qty);
            ps.executeUpdate();
        }
        logMovement(c, productId, locationId, qty, reason, referenceId, userId);
    }

    private void logMovement(Connection c, int productId, int locationId, double delta,
                             MovementReason reason, Long referenceId, int userId) throws SQLException {
        String sql = "INSERT INTO stock_movements(product_id, location_id, delta, reason, reference_id, user_id) "
                   + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, locationId);
            ps.setDouble(3, delta);
            ps.setString(4, reason.name());
            if (referenceId == null) ps.setNull(5, Types.INTEGER);
            else ps.setLong(5, referenceId);
            ps.setInt(6, userId);
            ps.executeUpdate();
        }
    }
    
    /** Every product with its quantity at one location (0 if it has no inventory row). */
    public List<StockRow> findStock(int locationId) throws SQLException {
        String sql = """
            SELECT p.id, p.code, p.name, p.category, p.unit, p.status, p.price_centavos,
                   COALESCE(i.quantity, 0) AS quantity, p.reorder_level
            FROM products p
            LEFT JOIN inventory i ON i.product_id = p.id AND i.location_id = ?
            ORDER BY p.category, p.name
            """;
        List<StockRow> rows = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new StockRow(
                            rs.getInt("id"), rs.getString("code"), rs.getString("name"),
                            rs.getString("category"), rs.getString("unit"), rs.getString("status"),
                            rs.getLong("price_centavos"), rs.getDouble("quantity"),
                            rs.getDouble("reorder_level")));
                }
            }
        }
        return rows;
    }
    
    private String describeShortage(Connection c, int locationId, int productId, double needed)
            throws SQLException {
        String sql = """
            SELECT p.name, p.unit, COALESCE(i.quantity, 0)
            FROM products p
            LEFT JOIN inventory i ON i.product_id = p.id AND i.location_id = ?
            WHERE p.id = ?
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "Not enough stock for product id " + productId;
                }
                return "Not enough stock for " + rs.getString(1) + ": needed "
                        + Format.qty(needed) + " " + rs.getString(2) + ", only "
                        + Format.qty(rs.getDouble(3)) + " available.";
            }
        }
    }
    

public List<StockedProduct> findStockedProducts(int locationId) throws SQLException {

    List<StockedProduct> products = new ArrayList<>();

    String sql = """
            SELECT
                p.id,
                p.code,
                p.name,
                p.category,
                p.unit,
                p.status,
                p.price_centavos,
                p.cost_price_centavos,
                p.reorder_level,
                i.quantity
            FROM inventory i
            JOIN products p
                ON i.product_id = p.id
            WHERE i.location_id = ?
            ORDER BY p.name
            """;

    try (Connection c = Database.connect();
         PreparedStatement ps = c.prepareStatement(sql)) {

        ps.setInt(1, locationId);

        try (ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Product product = new Product(
                    rs.getInt("id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getString("unit"),
                    rs.getString("status"),
                    rs.getLong("price_centavos"),
                    rs.getLong("cost_price_centavos"),
                    rs.getDouble("reorder_level")
                );

                StockedProduct stockedProduct = new StockedProduct(
                    product,
                    rs.getDouble("quantity")
                );

                products.add(stockedProduct);
            }
        }
    }

    return products;
}

}