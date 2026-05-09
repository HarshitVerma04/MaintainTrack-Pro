-- ============================================================
-- MaintainTrack Pro — SQLite schema
-- Day 2: 6 tables + foreign keys
-- Run via DatabaseManager.initSchema() on first launch
-- ============================================================

PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;

----- 1. EQUIPMENT -------------------------------------------
CREATE TABLE IF NOT EXISTS equipment (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,
    model           TEXT,
    serial_number   TEXT    UNIQUE,
    location        TEXT,
    purchase_date   TEXT,                       -- ISO-8601 date string
    status          TEXT    NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE','INACTIVE','RETIRED')),
    next_maint_date TEXT,                       -- ISO-8601 date, recalculated on Day 7
    created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

----- 2. PARTS ----------------------------------------------
CREATE TABLE IF NOT EXISTS parts (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,
    part_number     TEXT    UNIQUE,
    description     TEXT,
    qty_on_hand     INTEGER NOT NULL DEFAULT 0   CHECK (qty_on_hand >= 0),
    min_qty         INTEGER NOT NULL DEFAULT 0,  -- low-stock threshold (Day 13)
    unit_cost       REAL    NOT NULL DEFAULT 0.0,
    supplier_id     INTEGER REFERENCES suppliers(id) ON DELETE SET NULL,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

----- 3. SUPPLIERS -------------------------------------------
CREATE TABLE IF NOT EXISTS suppliers (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,
    contact_name    TEXT,
    phone           TEXT,
    email           TEXT,
    address         TEXT,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

----- 4. MAINTENANCE_LOG -----------------------------------
CREATE TABLE IF NOT EXISTS maintenance_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    equipment_id    INTEGER NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
    done_on         TEXT    NOT NULL,            -- ISO-8601 date performed
    interval_days   INTEGER NOT NULL DEFAULT 30, -- used to compute next_maint_date
    notes           TEXT,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

----- 5. BREAKDOWN_LOG -----------------------------------
CREATE TABLE IF NOT EXISTS breakdown_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    equipment_id    INTEGER NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
    reported_on     TEXT    NOT NULL DEFAULT (date('now')),
    description     TEXT,
    severity        TEXT    NOT NULL DEFAULT 'MEDIUM'
                            CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    resolved_on     TEXT,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

----- 6. ISSUE_RECORD ---------------------------------------
--   Tracks parts issued to / returned from a job
CREATE TABLE IF NOT EXISTS issue_record (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    part_id         INTEGER NOT NULL REFERENCES parts(id) ON DELETE RESTRICT,
    equipment_id    INTEGER          REFERENCES equipment(id) ON DELETE SET NULL,
    action          TEXT    NOT NULL CHECK (action IN ('ISSUE','RETURN')),
    qty             INTEGER NOT NULL CHECK (qty > 0),
    issued_on       TEXT    NOT NULL DEFAULT (date('now')),
    notes           TEXT,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

----- Indexes for common look-ups -----------------------------------
CREATE INDEX IF NOT EXISTS idx_mlog_equipment   ON maintenance_log(equipment_id);
CREATE INDEX IF NOT EXISTS idx_blog_equipment   ON breakdown_log(equipment_id);
CREATE INDEX IF NOT EXISTS idx_issue_part       ON issue_record(part_id);
CREATE INDEX IF NOT EXISTS idx_issue_equipment  ON issue_record(equipment_id);
CREATE INDEX IF NOT EXISTS idx_equip_next_maint ON equipment(next_maint_date);
CREATE INDEX IF NOT EXISTS idx_parts_qty        ON parts(qty_on_hand);
