package com.jabai.db;

import com.jabai.model.Clerk;
import com.jabai.model.Manager;
import com.jabai.model.Role;
import com.jabai.model.User;
import com.jabai.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDao {

    public Optional<User> authenticate(Role role, Integer expectedLocationId,
                                       String username, String password) throws SQLException {
        String sql = """
            SELECT u.id, u.username, u.password_hash, u.full_name, u.role,
                   u.location_id, u.status, l.name AS location_name
            FROM users u
            LEFT JOIN locations l ON u.location_id = l.id
            WHERE u.username = ?
            """;

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                if (!PasswordUtil.verify(password, rs.getString("password_hash"))) {
                    return Optional.empty();
                }

                Role actual = Role.valueOf(rs.getString("role"));
                if (actual != role) return Optional.empty();

                User user = map(rs, actual);
                if (!user.isActive()) return Optional.empty();

                if (user instanceof Clerk clerk) {
                    if (expectedLocationId == null || !clerk.getLocationId().equals(expectedLocationId)) {
                        return Optional.empty();
                    }
                }

                return Optional.of(user);
            }
        }
    }

    private User map(ResultSet rs, Role role) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String fullName = rs.getString("full_name");
        String status = rs.getString("status");

        return switch (role) {
            case MANAGER -> new Manager(id, username, fullName, status);
            case CLERK -> new Clerk(id, username, fullName, status,
                    rs.getInt("location_id"), rs.getString("location_name"));
        };
    }
}