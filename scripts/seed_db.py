"""
seed_db.py — MaintainTrack Pro
--------------------------------
Verifies the database schema and inserts sample data for development.
Run this from the project root:

    python scripts/seed_db.py

The script is safe to re-run — it checks before inserting
so you won't get duplicate seed data.
"""

import sqlite3
import os
from datetime import date, timedelta

# ── Config ────────────────────────────────────────────────────────────────
DB_PATH = os.path.join(os.path.dirname(__file__), '..', 'data', 'maintaintrack.db')


def get_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn


def verify_schema(conn):
    """Check all 6 tables exist."""
    expected = {
        'SUPPLIER', 'EQUIPMENT', 'PART',
        'MAINTENANCE_LOG', 'BREAKDOWN_LOG', 'ISSUE_RECORD'
    }
    cursor = conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table';"
    )
    existing = {row[0] for row in cursor.fetchall()}
    missing  = expected - existing

    if missing:
        print(f"[ERROR] Missing tables: {missing}")
        print("        Run the Java app once first to create the schema.")
        return False

    print(f"[OK] All 6 tables found: {', '.join(sorted(existing))}")
    return True


def already_seeded(conn):
    """Return True if seed data already exists."""
    count = conn.execute("SELECT COUNT(*) FROM EQUIPMENT;").fetchone()[0]
    return count > 0


def seed(conn):
    today = date.today()

    # ── 1. Suppliers ──────────────────────────────────────────────────
    suppliers = [
        ("FastParts Ltd",   "Rahul Sharma",  "+91-9876543210", "rahul@fastparts.in"),
        ("BoltWorld India", "Priya Mehta",   "+91-9123456789", "priya@boltworld.in"),
        ("TechSupply Co",   "Arjun Kapoor",  "+91-9988776655", "arjun@techsupply.in"),
    ]
    conn.executemany(
        "INSERT INTO SUPPLIER (name, contact_name, phone, email) VALUES (?,?,?,?);",
        suppliers
    )
    print("[OK] Inserted 3 suppliers")

    # ── 2. Equipment ──────────────────────────────────────────────────
    equipment_data = [
        ("CNC Machine A1",    "Shop Floor 1",  "Operational",      str(today + timedelta(days=5)),  30),
        ("Hydraulic Press B2","Shop Floor 2",  "Operational",      str(today - timedelta(days=2)),  15),
        ("Conveyor Belt C3",  "Warehouse",     "Under Maintenance", str(today + timedelta(days=12)), 60),
        ("Air Compressor D4", "Utility Room",  "Operational",      str(today + timedelta(days=20)), 45),
        ("Lathe Machine E5",  "Shop Floor 1",  "Operational",      str(today - timedelta(days=1)),  30),
    ]
    conn.executemany(
        """INSERT INTO EQUIPMENT
           (name, location, status, next_maintenance_date, interval_days)
           VALUES (?,?,?,?,?);""",
        equipment_data
    )
    print("[OK] Inserted 5 equipment records")

    # ── 3. Parts ──────────────────────────────────────────────────────
    parts = [
        (1, "Hydraulic Oil Filter",   20,  5,  "pcs",  450.00),
        (1, "V-Belt Drive",           8,   3,  "pcs",  320.00),
        (2, "M12 Hex Bolt Set",       150, 20, "set",  85.00),
        (2, "Bearing 6205-2RS",       30,  10, "pcs",  210.00),
        (3, "Control Relay 24V",      6,   5,  "pcs",  1200.00),
        (3, "Air Pressure Gauge",     4,   2,  "pcs",  675.00),
        (1, "Coolant Fluid 5L",       12,  5,  "can",  560.00),
    ]
    conn.executemany(
        """INSERT INTO PART
           (supplier_id, name, qty_on_hand, min_qty, unit, unit_cost)
           VALUES (?,?,?,?,?,?);""",
        parts
    )
    print("[OK] Inserted 7 parts")

    # ── 4. Maintenance Logs ───────────────────────────────────────────
    maintenance_logs = [
        (1, str(today - timedelta(days=30)), "Full lubrication and belt check",      "Harshit"),
        (2, str(today - timedelta(days=15)), "Hydraulic fluid replaced",              "Ravi"),
        (3, str(today - timedelta(days=60)), "Conveyor tension adjusted",             "Harshit"),
        (4, str(today - timedelta(days=45)), "Filter cleaned, pressure checked",      "Ankit"),
        (5, str(today - timedelta(days=30)), "Tool alignment and lubrication done",   "Harshit"),
    ]
    conn.executemany(
        """INSERT INTO MAINTENANCE_LOG
           (equipment_id, done_on, notes, done_by)
           VALUES (?,?,?,?);""",
        maintenance_logs
    )
    print("[OK] Inserted 5 maintenance logs")

    # ── 5. Breakdown Logs ─────────────────────────────────────────────
    breakdown_logs = [
        (2, str(today - timedelta(days=10)), "Hydraulic leak — seal worn out",     "Ravi"),
        (5, str(today - timedelta(days=5)),  "Motor overheating — coolant low",    "Harshit"),
    ]
    conn.executemany(
        """INSERT INTO BREAKDOWN_LOG
           (equipment_id, occurred_on, description, resolved_by)
           VALUES (?,?,?,?);""",
        breakdown_logs
    )
    print("[OK] Inserted 2 breakdown logs")

    # ── 6. Issue Records ──────────────────────────────────────────────
    issue_records = [
        (1, 2, str(today - timedelta(days=10)), 2, "Ravi",    "issue"),
        (4, 5, str(today - timedelta(days=5)),  1, "Harshit", "issue"),
        (7, 5, str(today - timedelta(days=5)),  1, "Harshit", "issue"),
        (3, 1, str(today - timedelta(days=30)), 5, "Harshit", "issue"),
        (3, 1, str(today - timedelta(days=1)),  2, "Ankit",   "return"),
    ]
    conn.executemany(
        """INSERT INTO ISSUE_RECORD
           (part_id, equipment_id, issued_on, qty, issued_by, type)
           VALUES (?,?,?,?,?,?);""",
        issue_records
    )
    print("[OK] Inserted 5 issue records")

    conn.commit()


def print_summary(conn):
    """Print a quick summary of what's in the DB."""
    print("\n── Database Summary ───────────────────────────────────")
    tables = ['SUPPLIER','EQUIPMENT','PART','MAINTENANCE_LOG','BREAKDOWN_LOG','ISSUE_RECORD']
    for t in tables:
        count = conn.execute(f"SELECT COUNT(*) FROM {t};").fetchone()[0]
        print(f"   {t:<22} → {count} rows")

    print("\n── Equipment with next maintenance date ───────────────")
    rows = conn.execute(
        "SELECT name, status, next_maintenance_date FROM EQUIPMENT ORDER BY next_maintenance_date;"
    ).fetchall()
    for name, status, nmd in rows:
        flag = " ⚠ OVERDUE" if nmd < str(date.today()) else ""
        print(f"   {name:<25} [{status}]  due: {nmd}{flag}")

    print("\n── Parts below minimum stock ───────────────────────────")
    rows = conn.execute(
        "SELECT name, qty_on_hand, min_qty FROM PART WHERE qty_on_hand < min_qty;"
    ).fetchall()
    if rows:
        for name, qty, min_qty in rows:
            print(f"   ⚠  {name:<30} qty={qty}  min={min_qty}")
    else:
        print("   All parts above minimum stock.")
    print("────────────────────────────────────────────────────────\n")


# ── Main ──────────────────────────────────────────────────────────────────
if __name__ == '__main__':
    print(f"\n[seed_db] Connecting to: {os.path.abspath(DB_PATH)}")

    if not os.path.exists(DB_PATH):
        print("[ERROR] Database file not found.")
        print("        Run the Java app once first — it creates the schema automatically.")
        exit(1)

    conn = get_connection()

    if not verify_schema(conn):
        conn.close()
        exit(1)

    if already_seeded(conn):
        print("[INFO] Seed data already present — skipping inserts.")
    else:
        print("[INFO] Inserting seed data...")
        seed(conn)
        print("[OK]  Seed complete.")

    print_summary(conn)
    conn.close()
