package com.maintaintrack.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseInitializer — runs once on app startup.
 *
 * Creates all 6 tables if they don't already exist.
 * Runs schema migrations safely so existing databases
 * get new columns without needing a reseed.
 *
 * Migration history:
 *   Day 8  — added breakdown_id FK to ISSUE_RECORD
 *   Day 9  — added maintenance_id FK to ISSUE_RECORD
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
     * Day 8: breakdown_id — links an issue to a specific breakdown incident.
     * Day 9: maintenance_id — links an issue to a specific maintenance job.
     * Both are optional (nullable) — a standalone stock draw leaves both NULL.
     */
    private static final String CREATE_ISSUE_RECORD = """
            CREATE TABLE IF NOT EXISTS ISSUE_RECORD (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                part_id        INTEGER NOT NULL REFERENCES PART(id),
                equipment_id   INTEGER NOT NULL REFERENCES EQUIPMENT(id),
                breakdown_id   INTEGER          REFERENCES BREAKDOWN_LOG(id),
                maintenance_id INTEGER          REFERENCES MAINTENANCE_LOG(id),
                issued_on      TEXT    NOT NULL,
                qty            INTEGER NOT NULL,
                issued_by      TEXT,
                type           TEXT    CHECK(type IN ('issue','return'))
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

            // Run migrations for existing databases
            migrateIssueRecord(conn);

            System.out.println("[DB] Schema initialized successfully.");

        } catch (SQLException e) {
            System.err.println("[DB] Failed to initialize schema: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Adds breakdown_id and maintenance_id to ISSUE_RECORD
     * if they don't already exist.
     * SQLite doesn't support ADD COLUMN IF NOT EXISTS,
     * so we check PRAGMA table_info first.
     */
    private static void migrateIssueRecord(Connection conn) throws SQLException {
        boolean hasBreakdownId   = false;
        boolean hasMaintenanceId = false;

        try (ResultSet rs = conn.createStatement()
                .executeQuery("PRAGMA table_info(ISSUE_RECORD);")) {
            while (rs.next()) {
                String col = rs.getString("name");
                if ("breakdown_id".equals(col))   hasBreakdownId   = true;
                if ("maintenance_id".equals(col))  hasMaintenanceId = true;
            }
        }

        if (!hasBreakdownId) {
            conn.createStatement().execute(
                "ALTER TABLE ISSUE_RECORD ADD COLUMN " +
                "breakdown_id INTEGER REFERENCES BREAKDOWN_LOG(id);"
            );
            System.out.println("[DB] Migration: added breakdown_id to ISSUE_RECORD.");
        }

        if (!hasMaintenanceId) {
            conn.createStatement().execute(
                "ALTER TABLE ISSUE_RECORD ADD COLUMN " +
                "maintenance_id INTEGER REFERENCES MAINTENANCE_LOG(id);"
            );
            System.out.println("[DB] Migration: added maintenance_id to ISSUE_RECORD.");
        }
    }
}
