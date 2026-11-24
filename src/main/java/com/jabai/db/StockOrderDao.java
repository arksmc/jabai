/*
 * this file saves stock orders placed with a supplier and their items, and reads them back for the stock orders screen. it is used by stockservice when the manager restocks the warehouse.
 */

package com.jabai.db;

import com.jabai.model.OrderItemView;
import com.jabai.model.StockOrderSummary;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockOrderDao {

    public List<StockOrderSummary> findAll() throws SQLException {
        String sql = """
            SELECT o.id, o.supplier, o.status, u.full_name AS ordered_by,
                   o.created_at, o.received_at,
                   COUNT(i.id) AS item_count,
                   COALESCE(SUM(ROUND(i.quantity * i.unit_cost_centavos)), 0) AS total
            FROM stock_orders o
            JOIN users u ON u.id = o.ordered_by
            LEFT JOIN stock_order_items i ON i.order_id = o.id
            GROUP BY o.id
            ORDER BY o.id DESC
            """;
        List<StockOrderSummary> list = new ArrayList<>();
        try (Connection c = Database.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new StockOrderSummary(
                        rs.getLong("id"), rs.getString("supplier"), rs.getString("status"),
                        rs.getString("ordered_by"), rs.getString("created_at"),
                        rs.getString("received_at"), rs.getInt("item_count"), rs.getLong("total")));
            }
        }
        return list;
    }

    public List<OrderItemView> findItems(long orderId) throws SQLException {
        String sql = """
            SELECT p.code, p.name, p.unit, i.quantity, i.unit_cost_centavos
            FROM stock_order_items i
            JOIN products p ON p.id = i.product_id
            WHERE i.order_id = ?
            ORDER BY p.name
            """;
        List<OrderItemView> list = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OrderItemView(
                            rs.getString("code"), rs.getString("name"), rs.getString("unit"),
                            rs.getDouble("quantity"), rs.getLong("unit_cost_centavos")));
                }
            }
        }
        return list;
    }
}