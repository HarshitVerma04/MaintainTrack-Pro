package com.maintaintrack.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseInitializer — runs once on app startup.
 *
 * Creates all 6 tables if they don't already exist.
 * Safe to call every launch — uses CREATE TABLE IF NOT EXISTS.
 *
 * Call order matters because of foreign keys:
 *   1. SUPPLIER   (no dependencies)
 *   2. EQUIPMENT  (no dependencies)
 *   3. PART       (depends on SUPPLIER)
 *   4. MAINTENANCE_LOG  (depends on EQUIPMENT)
 *   5. BREAKDOWN_LOG    (depends on EQUIPMENT)
 *   6. ISSUE_RECORD     (depends on PART + EQUIPMENT)
 */
public class DatabaseInitializer {

    // ── 1. SUPPLIER ───────────────────────────────────────────────────────
    private static final String CREATE_SUPPLIER = """
            CREATE TABLE IF NOT EXISTS SUPPLIER (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                name         TEXT    NOT NULL,
                contact_name TEXT,
                phone        TEXT,
                email        TEXT
            );
            """;

    // ── 2. EQUIPMENT ──────────────────────────────────────────────────────
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

    // ── 3. PART ───────────────────────────────────────────────────────────
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

    // ── 4. MAINTENANCE_LOG ────────────────────────────────────────────────
    private static final String CREATE_MAINTENANCE_LOG = """
            CREATE TABLE IF NOT EXISTS MAINTENANCE_LOG (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                equipment_id INTEGER NOT NULL REFERENCES EQUIPMENT(id),
                done_on      TEXT    NOT NULL,
                notes        TEXT,
                done_by      TEXT
            );
            """;

    // ── 5. BREAKDOWN_LOG ──────────────────────────────────────────────────
    private static final String CREATE_BREAKDOWN_LOG = """
            CREATE TABLE IF NOT EXISTS BREAKDOWN_LOG (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                equipment_id INTEGER NOT NULL REFERENCES EQUIPMENT(id),
                occurred_on  TEXT    NOT NULL,
                description  TEXT,
                resolved_by  TEXT
            );
            """;

    // ── 6. ISSUE_RECORD ───────────────────────────────────────────────────
    private static final String CREATE_ISSUE_RECORD = """
            CREATE TABLE IF NOT EXISTS ISSUE_RECORD (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                part_id      INTEGER NOT NULL REFERENCES PART(id),
                equipment_id INTEGER NOT NULL REFERENCES EQUIPMENT(id),
                issued_on    TEXT    NOT NULL,
                qty          INTEGER NOT NULL,
                issued_by    TEXT,
                type         TEXT    CHECK(type IN ('issue','return'))
            );
            """;

    /**
     * Initializes the database — call this once from MainApp before
     * loading any FXML screen.
     */
    public static void initialize() {
        try (Connection conn = DBConnection.getConnection();
             Statement  stmt = conn.createStatement()) {

            // Enable FK enforcement (SQLite has it OFF by default)
            stmt.execute("PRAGMA foreign_keys = ON;");

            // Create tables in dependency order
            stmt.execute(CREATE_SUPPLIER);
            stmt.execute(CREATE_EQUIPMENT);
            stmt.execute(CREATE_PART);
            stmt.execute(CREATE_MAINTENANCE_LOG);
            stmt.execute(CREATE_BREAKDOWN_LOG);
            stmt.execute(CREATE_ISSUE_RECORD);

            System.out.println("[DB] Schema initialized successfully.");

        } catch (SQLException e) {
            System.err.println("[DB] Failed to initialize schema: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }
}
