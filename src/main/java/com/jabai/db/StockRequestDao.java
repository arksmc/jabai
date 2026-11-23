package com.jabai.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.jabai.model.StockRequestSummary;
import com.jabai.model.RequestItemView;

public class StockRequestDao {

    public static List<StockRequestSummary> findAll() throws SQLException {

        List<StockRequestSummary> requests = new ArrayList<>();

        String sql = """
                SELECT
                    sr.id,
                    l.name AS location_name,
                    requester.full_name AS requested_by,
                    sr.status,
                    sr.note,
                    sr.response_note,
                    sr.created_at,
                    sr.resolved_at,
                    resolver.full_name AS resolved_by,
                    COUNT(sri.id) AS item_count
                FROM stock_requests sr
                JOIN locations l
                    ON sr.location_id = l.id
                JOIN users requester
                    ON sr.requested_by = requester.id
                LEFT JOIN users resolver
                    ON sr.resolved_by = resolver.id
                LEFT JOIN stock_request_items sri
                    ON sr.id = sri.request_id
                GROUP BY sr.id
                ORDER BY sr.created_at DESC
                """;

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                StockRequestSummary request = new StockRequestSummary(
                    rs.getLong("id"),
                    rs.getString("location_name"),
                    rs.getString("requested_by"),
                    rs.getString("status"),
                    rs.getString("note"),
                    rs.getString("response_note"),
                    rs.getString("created_at"),
                    rs.getString("resolved_at"),
                    rs.getString("resolved_by"),
                    rs.getInt("item_count")
                );

                requests.add(request);
            }
        }

        return requests;
    }

    public static List<StockRequestSummary> findByLocation(int locationId)
            throws SQLException {

        List<StockRequestSummary> requests = new ArrayList<>();

        String sql = """
                SELECT
                    sr.id,
                    l.name AS location_name,
                    requester.full_name AS requested_by,
                    sr.status,
                    sr.note,
                    sr.response_note,
                    sr.created_at,
                    sr.resolved_at,
                    resolver.full_name AS resolved_by,
                    COUNT(sri.id) AS item_count
                FROM stock_requests sr
                JOIN locations l
                    ON sr.location_id = l.id
                JOIN users requester
                    ON sr.requested_by = requester.id
                LEFT JOIN users resolver
                    ON sr.resolved_by = resolver.id
                LEFT JOIN stock_request_items sri
                    ON sr.id = sri.request_id
                WHERE sr.location_id = ?
                GROUP BY sr.id
                ORDER BY sr.created_at DESC
                """;

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, locationId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    StockRequestSummary request = new StockRequestSummary(
                        rs.getLong("id"),
                        rs.getString("location_name"),
                        rs.getString("requested_by"),
                        rs.getString("status"),
                        rs.getString("note"),
                        rs.getString("response_note"),
                        rs.getString("created_at"),
                        rs.getString("resolved_at"),
                        rs.getString("resolved_by"),
                        rs.getInt("item_count")
                    );

                    requests.add(request);
                }
            }
        }

        return requests;
    }

    public static List<RequestItemView> findItems(long requestId)
            throws SQLException {

        List<RequestItemView> items = new ArrayList<>();

        String sql = """
                SELECT
                    p.id AS product_id,
                    p.code AS product_code,
                    p.name AS product_name,
                    p.unit,
                    sri.quantity_requested,
                    sri.quantity_sent
                FROM stock_request_items sri
                JOIN products p
                    ON sri.product_id = p.id
                WHERE sri.request_id = ?
                ORDER BY p.name
                """;

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, requestId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    RequestItemView item = new RequestItemView(
                        rs.getInt("product_id"),
                        rs.getString("product_code"),
                        rs.getString("product_name"),
                        rs.getString("unit"),
                        rs.getDouble("quantity_requested"),
                        rs.getDouble("quantity_sent")
                    );

                    items.add(item);
                }
            }
        }

        return items;
    }
}