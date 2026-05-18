package com.maintaintrack.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseInitializer — runs once on app startup.
 *
 * Creates all 6 tables if they don't already exist.
 * Also runs schema migrations safely using ALTER TABLE IF NOT EXISTS pattern
 * so existing databases get new columns without needing a reseed.
 *
 * Call order matters because of foreign keys:
 *   1. SUPPLIER        (no dependencies)
 *   2. EQUIPMENT       (no dependencies)
 *   3. PART            (depends on SUPPLIER)
 *   4. MAINTENANCE_LOG (depends on EQUIPMENT)
 *   5. BREAKDOWN_LOG   (depends on EQUIPMENT)
 *   6. ISSUE_RECORD    (depends on PART + EQUIPMENT + BREAKDOWN_LOG)
 */
public class DatabaseInitializer {

    private static final String CREATE_SUPPLIER = """
            CREATE TABLE IF NOT EXISTS SUPPLIER (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                name         TEXT    NOT NULL,
                contact_name TEXT,
                phone        TEXT,
                email        TEXT
            );
            """;

    private static final String CREATE_EQUIPMENT = """
            CREATE TABLE IF NOT EXISTS EQUIPMENT (
                id                    INTEGER PRIMARY KEY AUTOINCREMENT,
                name                  TEXT    NOT NULL,
                location              TEXT,
                status                TEXT    DEFAULT 'Operational',
                next_maintenance_date TEXT,
                interval_days         INTEGER DEFAULT 30
            );
            """;

    private static final String CREATE_PART = """
            CREATE TABLE IF NOT EXISTS PART (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                supplier_id INTEGER REFERENCES SUPPLIER(id),
                name        TEXT    NOT NULL,
                qty_on_hand INTEGER DEFAULT 0,
                min_qty     INTEGER DEFAULT 5,
                unit        TEXT    DEFAULT 'pcs',
                unit_cost   REAL    DEFAULT 0.0
            );
            """;

    private static final String CREATE_MAINTENANCE_LOG = """
            CREATE TABLE IF NOT EXISTS MAINTENANCE_LOG (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                equipment_id INTEGER NOT NULL REFERENCES EQUIPMENT(id),
                done_on      TEXT    NOT NULL,
                notes        TEXT,
                done_by      TEXT
            );
            """;

    private static final String CREATE_BREAKDOWN_LOG = """
            CREATE TABLE IF NOT EXISTS BREAKDOWN_LOG (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                equipment_id INTEGER NOT NULL REFERENCES EQUIPMENT(id),
                occurred_on  TEXT    NOT NULL,
                description  TEXT,
                resolved_by  TEXT
            );
            """;

    /**
     * Day 8 — breakdown_id FK added to ISSUE_RECORD.
     * Links a part transaction to the specific breakdown it was used to fix.
     * Optional (nullable) — issues not tied to a breakdown leave it NULL.
     */
    private static final String CREATE_ISSUE_RECORD = """
            CREATE TABLE IF NOT EXISTS ISSUE_RECORD (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                part_id      INTEGER NOT NULL REFERENCES PART(id),
                equipment_id INTEGER NOT NULL REFERENCES EQUIPMENT(id),
                breakdown_id INTEGER          REFERENCES BREAKDOWN_LOG(id),
                issued_on    TEXT    NOT NULL,
                qty          INTEGER NOT NULL,
                issued_by    TEXT,
                type         TEXT    CHECK(type IN ('issue','return'))
            );
            """;

    public static void initialize() {
        try (Connection conn = DBConnection.getConnection();
             Statement  stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON;");

            stmt.execute(CREATE_SUPPLIER);
            stmt.execute(CREATE_EQUIPMENT);
            stmt.execute(CREATE_PART);
            stmt.execute(CREATE_MAINTENANCE_LOG);
            stmt.execute(CREATE_BREAKDOWN_LOG);
            stmt.execute(CREATE_ISSUE_RECORD);

            // ── Day 8 migration: add breakdown_id to existing databases ───────
            // ALTER TABLE ADD COLUMN is safe to call even if the column exists
            // because we check the schema first.
            migrateAddBreakdownId(conn);

            System.out.println("[DB] Schema initialized successfully.");

        } catch (SQLException e) {
            System.err.println("[DB] Failed to initialize schema: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Adds breakdown_id to ISSUE_RECORD if it doesn't already exist.
     * SQLite doesn't support "ALTER TABLE ADD COLUMN IF NOT EXISTS",
     * so we check PRAGMA table_info first.
     */
    private static void migrateAddBreakdownId(Connection conn) throws SQLException {
        boolean columnExists = false;
        try (ResultSet rs = conn.createStatement()
                .executeQuery("PRAGMA table_info(ISSUE_RECORD);")) {
            while (rs.next()) {
                if ("breakdown_id".equals(rs.getString("name"))) {
                    columnExists = true;
                    break;
                }
            }
        }
        if (!columnExists) {
            conn.createStatement().execute(
                "ALTER TABLE ISSUE_RECORD ADD COLUMN " +
                "breakdown_id INTEGER REFERENCES BREAKDOWN_LOG(id);"
            );
            System.out.println("[DB] Migration: added breakdown_id to ISSUE_RECORD.");
        }
    }
}
