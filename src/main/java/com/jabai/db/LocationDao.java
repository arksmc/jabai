package com.jabai.db;

import com.jabai.model.Location;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LocationDao {

    public List<Location> findOutlets() throws SQLException {
        List<Location> list = new ArrayList<>();
        String sql = "SELECT id, name, type FROM locations WHERE type = 'OUTLET' ORDER BY id";
        try (Connection c = Database.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Location(rs.getInt("id"), rs.getString("name"), rs.getString("type")));
            }
        }
        return list;
    }
}