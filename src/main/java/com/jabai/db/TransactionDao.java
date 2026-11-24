package com.jabai.db;

import com.jabai.model.TransactionRow;
import com.jabai.util.Format;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class TransactionDao {

    public List<TransactionRow> findRecent(int locationId, int limit) throws SQLException {
        return findAll(locationId, limit);
    }

    public List<TransactionRow> findAll(Integer locationId, int limit) throws SQLException {
        String sql = """
            SELECT 'SALE' AS kind, s.id AS id, s.created_at AS created_at, u.full_name AS recorded_by,
                   s.total_centavos AS amount, s.status AS status, s.void_reason AS void_reason,
                   (SELECT COUNT(*) FROM sale_items si WHERE si.sale_id = s.id) AS item_count,
                   NULL AS donor_name, NULL AS product_name, NULL AS unit, 0 AS quantity,
                   l.name AS location_name
            FROM sales s
            JOIN users u ON u.id = s.user_id
            JOIN locations l ON l.id = s.location_id
            WHERE (? IS NULL OR s.location_id = ?)
            UNION ALL
            SELECT d.type, d.id, d.created_at, u.full_name,
                   d.amount_centavos, d.status, d.void_reason,
                   0, d.donor_name, p.name, p.unit, d.quantity,
                   l.name
            FROM donations d
            JOIN users u ON u.id = d.user_id
            JOIN locations l ON l.id = d.location_id
            LEFT JOIN products p ON p.id = d.product_id
            WHERE (? IS NULL OR d.location_id = ?)
            ORDER BY created_at DESC, id DESC
            LIMIT ?
            """;
        List<TransactionRow> rows = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindLocation(ps, 1, locationId);
            bindLocation(ps, 3, locationId);
            ps.setInt(5, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String kind = rs.getString("kind");
                    String details;
                    if ("SALE".equals(kind)) {
                        int n = rs.getInt("item_count");
                        details = n + (n == 1 ? " item" : " items");
                    } else if ("CASH".equals(kind)) {
                        details = "Cash from " + rs.getString("donor_name");
                    } else {
                        details = Format.qty(rs.getDouble("quantity")) + " " + rs.getString("unit") + " "
                                + rs.getString("product_name") + " from " + rs.getString("donor_name");
                    }
                    rows.add(new TransactionRow(kind, rs.getLong("id"), rs.getString("created_at"),
                            rs.getString("recorded_by"), details, rs.getLong("amount"),
                            rs.getString("status"), rs.getString("void_reason"),
                            rs.getString("location_name")));
                }
            }
        }
        return rows;
    }

    private void bindLocation(PreparedStatement ps, int index, Integer locationId) throws SQLException {
        if (locationId == null) {
            ps.setNull(index, Types.INTEGER);
            ps.setNull(index + 1, Types.INTEGER);
        } else {
            ps.setInt(index, locationId);
            ps.setInt(index + 1, locationId);
        }
    }
}