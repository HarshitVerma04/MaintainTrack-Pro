package com.maintaintrack.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Runs schema migrations on the local SQLite DB.
 * Safe to run every startup — each migration checks before applying.
 *
 * Migration history:
 *   V2.0  — added updated_at, synced, server_id to all tables
 *   Day 41 — added sync_id (UUID) to all tables for cloud sync
 */
public class MigrationRunner {

    private static final String[] TABLES = {
            "SUPPLIER", "EQUIPMENT", "PART",
            "MAINTENANCE_LOG", "BREAKDOWN_LOG", "ISSUE_RECORD"
    };

    public static void run() {
        try (Connection conn = DBConnection.getConnection()) {
            addSyncColumns(conn);
            addSyncIdColumn(conn);   // ← Day 41 addition
            System.out.println("[Migration] All migrations applied successfully.");
        } catch (SQLException e) {
            System.err.println("[Migration] Failed: " + e.getMessage());
            throw new RuntimeException("Migration failed", e);
        }
    }

    // ── Existing V2.0 migration ───────────────────────────────────────────────
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

    // ── Day 41 — sync_id UUID column ─────────────────────────────────────────
    // Every record gets a UUID that is the same on both SQLite and PostgreSQL.
    // This is how the two sides recognise the same record even though their
    // auto-increment IDs are different numbers.
    private static void addSyncIdColumn(Connection conn) throws SQLException {
        for (String table : TABLES) {
            // Add the column if it doesn't exist yet
            addColumnIfMissing(conn, table, "sync_id", "TEXT");

            // Backfill any existing rows that have a NULL sync_id
            // so every record has a UUID before the first sync attempt
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                        "SELECT id FROM " + table + " WHERE sync_id IS NULL"
                );
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String uuid = UUID.randomUUID().toString();
                    conn.createStatement().execute(
                            "UPDATE " + table + " SET sync_id = '" + uuid +
                                    "' WHERE id = " + id
                    );
                }
            }
        }
        System.out.println("[Migration] sync_id backfill complete.");
    }

    // ── Helper ────────────────────────────────────────────────────────────────
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