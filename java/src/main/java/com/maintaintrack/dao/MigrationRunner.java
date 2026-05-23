package com.maintaintrack.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * Runs schema migrations on the local SQLite DB.
 * Safe to run every startup — each migration checks before applying.
 */
public class MigrationRunner {

    private static final String[] TABLES = {
            "SUPPLIER", "EQUIPMENT", "PART",
            "MAINTENANCE_LOG", "BREAKDOWN_LOG", "ISSUE_RECORD"
    };

    public static void run() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            addSyncColumns(conn);
            System.out.println("[Migration] All migrations applied successfully.");

        } catch (SQLException e) {
            System.err.println("[Migration] Failed: " + e.getMessage());
            throw new RuntimeException("Migration failed", e);
        }
    }

    private static void addSyncColumns(Connection conn) throws SQLException {
        for (String table : TABLES) {
            addColumnIfMissing(conn, table, "updated_at",
                    "TEXT DEFAULT '" + LocalDateTime.now() + "'");
            addColumnIfMissing(conn, table, "synced",
                    "INTEGER DEFAULT 0");
            addColumnIfMissing(conn, table, "server_id",
                    "INTEGER");
        }
    }

    private static void addColumnIfMissing(Connection conn,
                                           String table,
                                           String column,
                                           String definition)
            throws SQLException {

        boolean exists = false;
        try (ResultSet rs = conn.createStatement()
                .executeQuery("PRAGMA table_info(" + table + ");")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }

        if (!exists) {
            conn.createStatement().execute(
                    "ALTER TABLE " + table +
                            " ADD COLUMN " + column + " " + definition + ";"
            );
            System.out.println("[Migration] Added " + column + " to " + table);
        }
    }
}
