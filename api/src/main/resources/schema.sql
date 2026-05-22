-- ============================================================
--  MaintainTrack Pro — PostgreSQL Cloud Schema
--  Mirrors SQLite schema exactly + sync columns for hybrid mode
-- ============================================================

-- ── 1. SUPPLIER ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS supplier (
                                        id           SERIAL PRIMARY KEY,
                                        name         TEXT   NOT NULL,
                                        contact_name TEXT,
                                        phone        TEXT,
                                        email        TEXT,
                                        updated_at   TIMESTAMP DEFAULT NOW(),
    synced       BOOLEAN   DEFAULT FALSE
    );

-- ── 2. EQUIPMENT ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS equipment (
                                         id                    SERIAL PRIMARY KEY,
                                         name                  TEXT   NOT NULL,
                                         location              TEXT,
                                         status                TEXT   DEFAULT 'Operational',
                                         next_maintenance_date TEXT,
                                         interval_days         INTEGER DEFAULT 30,
                                         updated_at            TIMESTAMP DEFAULT NOW(),
    synced                BOOLEAN   DEFAULT FALSE
    );

-- ── 3. PART ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS part (
                                    id          SERIAL  PRIMARY KEY,
                                    supplier_id INTEGER REFERENCES supplier(id),
    name        TEXT    NOT NULL,
    qty_on_hand INTEGER DEFAULT 0,
    min_qty     INTEGER DEFAULT 5,
    unit        TEXT    DEFAULT 'pcs',
    unit_cost   NUMERIC DEFAULT 0.0,
    updated_at  TIMESTAMP DEFAULT NOW(),
    synced      BOOLEAN   DEFAULT FALSE
    );

-- ── 4. MAINTENANCE_LOG ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS maintenance_log (
                                               id           SERIAL  PRIMARY KEY,
                                               equipment_id INTEGER NOT NULL REFERENCES equipment(id),
    done_on      TEXT    NOT NULL,
    notes        TEXT,
    done_by      TEXT,
    updated_at   TIMESTAMP DEFAULT NOW(),
    synced       BOOLEAN   DEFAULT FALSE
    );

-- ── 5. BREAKDOWN_LOG ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS breakdown_log (
                                             id           SERIAL  PRIMARY KEY,
                                             equipment_id INTEGER NOT NULL REFERENCES equipment(id),
    occurred_on  TEXT    NOT NULL,
    description  TEXT,
    resolved_by  TEXT,
    updated_at   TIMESTAMP DEFAULT NOW(),
    synced       BOOLEAN   DEFAULT FALSE
    );

-- ── 6. ISSUE_RECORD ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS issue_record (
                                            id             SERIAL  PRIMARY KEY,
                                            part_id        INTEGER NOT NULL REFERENCES part(id),
    equipment_id   INTEGER NOT NULL REFERENCES equipment(id),
    breakdown_id   INTEGER          REFERENCES breakdown_log(id),
    maintenance_id INTEGER          REFERENCES maintenance_log(id),
    issued_on      TEXT    NOT NULL,
    qty            INTEGER NOT NULL,
    issued_by      TEXT,
    type           TEXT    CHECK(type IN ('issue','return')),
    updated_at     TIMESTAMP DEFAULT NOW(),
    synced         BOOLEAN   DEFAULT FALSE
    );

-- ── 7. APP_USER ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS app_user (
                                        id            SERIAL PRIMARY KEY,
                                        username      TEXT   NOT NULL UNIQUE,
                                        email         TEXT   NOT NULL UNIQUE,
                                        password_hash TEXT   NOT NULL,
                                        role          TEXT   NOT NULL CHECK(role IN ('ADMIN','MANAGER','TECHNICIAN')),
    created_at    TIMESTAMP DEFAULT NOW()
    );