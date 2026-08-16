package com.maintaintrack.dao;

import java.sql.*;

/**
 * Key-value settings store in SQLite.
 * Used to persist last_sync_time across sessions.
 */
public class SettingsDAO {

    public static void createTableIfMissing() {
        String sql = """
                CREATE TABLE IF NOT EXISTS SETTINGS (
                    key   TEXT PRIMARY KEY,
                    value TEXT
                );
                """;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            System.err.println("[Settings] Table creation failed: " + e.getMessage());
        }
    }

    public static String get(String key, String defaultValue) {
        String sql = "SELECT value FROM SETTINGS WHERE key = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("value");
        } catch (Exception e) {
            System.err.println("[Settings] Read failed: " + e.getMessage());
        }
        return defaultValue;
    }

    public static void set(String key, String value) {
        // INSERT OR REPLACE handles both insert and update in one statement
        String sql = "INSERT OR REPLACE INTO SETTINGS (key, value) VALUES (?, ?);";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[Settings] Write failed: " + e.getMessage());
        }
    }
}