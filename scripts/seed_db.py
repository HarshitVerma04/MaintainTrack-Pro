"""
seed_db.py — MaintainTrack Pro
--------------------------------
Rich seed data for Phase 4 development and testing.
Run from project root:
    python scripts/seed_db.py
    python scripts/seed_db.py --reset   (wipe and reseed)
"""

import sqlite3, os, sys
from datetime import date, timedelta

DB_PATH = os.path.join(os.path.dirname(__file__), '..', 'data', 'maintaintrack.db')
RESET   = '--reset' in sys.argv
TODAY   = date.today()

def get_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn

def verify_schema(conn):
    expected = {'SUPPLIER','EQUIPMENT','PART','MAINTENANCE_LOG','BREAKDOWN_LOG','ISSUE_RECORD'}
    existing = {r[0] for r in conn.execute("SELECT name FROM sqlite_master WHERE type='table';")}
    missing  = expected - existing
    if missing:
        print(f"[ERROR] Missing tables: {missing}. Run Java app first.")
        return False
    print("[OK] All 6 tables present.")
    return True

def reset_data(conn):
    for t in ['ISSUE_RECORD','MAINTENANCE_LOG','BREAKDOWN_LOG','PART','EQUIPMENT','SUPPLIER']:
        conn.execute(f"DELETE FROM {t};")
        conn.execute(f"DELETE FROM sqlite_sequence WHERE name='{t}';")
    conn.commit()
    print("[RESET] All tables cleared.")

def already_seeded(conn):
    return conn.execute("SELECT COUNT(*) FROM EQUIPMENT;").fetchone()[0] > 0

def seed(conn):
    # 1. SUPPLIERS
    conn.executemany("INSERT INTO SUPPLIER (name,contact_name,phone,email) VALUES (?,?,?,?);", [
        ("FastParts Ltd",      "Rahul Sharma",  "+91-9876543210", "rahul@fastparts.in"),
        ("BoltWorld India",    "Priya Mehta",   "+91-9123456789", "priya@boltworld.in"),
        ("TechSupply Co",      "Arjun Kapoor",  "+91-9988776655", "arjun@techsupply.in"),
        ("HydroMech Supplies", "Sneha Rao",     "+91-9871234560", "sneha@hydromech.in"),
        ("ElectroParts India", "Vivek Nair",    "+91-9765432100", "vivek@electroparts.in"),
    ])
    print("[OK] 5 suppliers")

    # 2. EQUIPMENT
    conn.executemany("INSERT INTO EQUIPMENT (name,location,status,next_maintenance_date,interval_days) VALUES (?,?,?,?,?);", [
        ("CNC Machine A1",     "Shop Floor 1", "Operational",       str(TODAY+timedelta(days=5)),   30),
        ("Hydraulic Press B2", "Shop Floor 2", "Operational",       str(TODAY-timedelta(days=2)),   15),
        ("Conveyor Belt C3",   "Warehouse",    "Under Maintenance", str(TODAY+timedelta(days=12)),  60),
        ("Air Compressor D4",  "Utility Room", "Operational",       str(TODAY+timedelta(days=20)),  45),
        ("Lathe Machine E5",   "Shop Floor 1", "Operational",       str(TODAY-timedelta(days=1)),   30),
        ("Welding Robot F6",   "Shop Floor 3", "Operational",       str(TODAY+timedelta(days=8)),   21),
        ("Forklift G7",        "Warehouse",    "Operational",       str(TODAY+timedelta(days=3)),   14),
        ("Drill Press H8",     "Shop Floor 2", "Out of Service",    str(TODAY-timedelta(days=5)),   30),
    ])
    print("[OK] 8 equipment")

    # 3. PARTS
    conn.executemany("INSERT INTO PART (supplier_id,name,qty_on_hand,min_qty,unit,unit_cost) VALUES (?,?,?,?,?,?);", [
        (1,"Hydraulic Oil Filter",   20, 5,"pcs", 450.00),
        (1,"V-Belt Drive",            8, 3,"pcs", 320.00),
        (2,"M12 Hex Bolt Set",      150,20,"set",  85.00),
        (2,"Bearing 6205-2RS",       30,10,"pcs", 210.00),
        (3,"Control Relay 24V",       4, 5,"pcs",1200.00),  # LOW STOCK
        (3,"Air Pressure Gauge",      2, 2,"pcs", 675.00),  # LOW STOCK (at min)
        (1,"Coolant Fluid 5L",       12, 5,"can", 560.00),
        (4,"Hydraulic Seal Kit",      6, 4,"set", 890.00),
        (4,"O-Ring Pack (50pcs)",    40,10,"pack",120.00),
        (5,"Motor Drive Belt",        3, 5,"pcs", 480.00),  # LOW STOCK
        (5,"3-Phase Contactor",       8, 3,"pcs",1450.00),
        (2,"Grease Cartridge 400g",  25, 8,"pcs",  95.00),
        (1,"Air Filter Element",      9, 4,"pcs", 220.00),
        (3,"Proximity Sensor NPN",    5, 3,"pcs", 780.00),
    ])
    print("[OK] 14 parts (3 low-stock)")

    # 4. MAINTENANCE LOGS
    conn.executemany("INSERT INTO MAINTENANCE_LOG (equipment_id,done_on,notes,done_by) VALUES (?,?,?,?);", [
        (1,str(TODAY-timedelta(days=150)),"Full lubrication and belt tension check","Harshit"),
        (1,str(TODAY-timedelta(days=120)),"Coolant flush, tool alignment verified","Ravi"),
        (1,str(TODAY-timedelta(days=90)), "Filter replaced, spindle check OK","Harshit"),
        (1,str(TODAY-timedelta(days=60)), "Belt replaced, lubrication done","Ankit"),
        (1,str(TODAY-timedelta(days=30)), "Full PM — all systems operational","Harshit"),
        (2,str(TODAY-timedelta(days=130)),"Hydraulic fluid replaced, seals inspected","Ravi"),
        (2,str(TODAY-timedelta(days=100)),"Fluid top-up, hose inspection done","Ankit"),
        (2,str(TODAY-timedelta(days=70)), "Full PM — ram alignment checked","Ravi"),
        (2,str(TODAY-timedelta(days=55)), "Seal kit replaced after leak detection","Ravi"),
        (2,str(TODAY-timedelta(days=40)), "Emergency PM after breakdown","Ravi"),
        (3,str(TODAY-timedelta(days=180)),"Belt tension adjusted, motor lubricated","Sneha"),
        (3,str(TODAY-timedelta(days=120)),"Full inspection — rollers OK","Sneha"),
        (3,str(TODAY-timedelta(days=60)), "Motor bearings replaced","Ankit"),
        (4,str(TODAY-timedelta(days=135)),"Filter cleaned, pressure relief checked","Ankit"),
        (4,str(TODAY-timedelta(days=90)), "Air filter replaced, oil level OK","Harshit"),
        (4,str(TODAY-timedelta(days=45)), "Full PM — compressor serviced","Ankit"),
        (5,str(TODAY-timedelta(days=120)),"Tool alignment and lubrication","Harshit"),
        (5,str(TODAY-timedelta(days=90)), "Coolant flush, chuck cleaned","Priya"),
        (5,str(TODAY-timedelta(days=60)), "Motor brushes inspected — OK","Harshit"),
        (5,str(TODAY-timedelta(days=30)), "Full PM after overheating incident","Ravi"),
        (6,str(TODAY-timedelta(days=63)), "Servo motor checked, calibration done","Ankit"),
        (6,str(TODAY-timedelta(days=42)), "Wire harness inspection, gripper lubed","Sneha"),
        (6,str(TODAY-timedelta(days=21)), "Full PM — weld quality test passed","Ankit"),
        (7,str(TODAY-timedelta(days=28)), "Tyre pressure, fork alignment, brake check","Priya"),
        (7,str(TODAY-timedelta(days=14)), "Battery topped up, horn and lights tested","Priya"),
        (8,str(TODAY-timedelta(days=90)), "Spindle lubricated, belt tension OK","Ravi"),
        (8,str(TODAY-timedelta(days=60)), "Full PM — motor inspection done","Harshit"),
    ])
    print("[OK] 27 maintenance logs")

    # 5. BREAKDOWN LOGS
    conn.executemany("INSERT INTO BREAKDOWN_LOG (equipment_id,occurred_on,description,resolved_by) VALUES (?,?,?,?);", [
        (1,str(TODAY-timedelta(days=135)),"Coolant pump failure — no coolant flow","Harshit"),
        (1,str(TODAY-timedelta(days=75)), "X-axis encoder fault — position loss","Ravi"),
        (2,str(TODAY-timedelta(days=120)),"Hydraulic leak at ram cylinder seal","Ravi"),
        (2,str(TODAY-timedelta(days=80)), "Pressure build-up failure — relief valve stuck","Ravi"),
        (2,str(TODAY-timedelta(days=42)), "Hydraulic hose burst — emergency stop","Ravi"),
        (3,str(TODAY-timedelta(days=90)), "Drive motor overload trip — belt slippage","Sneha"),
        (3,str(TODAY-timedelta(days=30)), "Roller bearing failure — vibration detected","Ankit"),
        (5,str(TODAY-timedelta(days=35)), "Motor overheating — coolant reservoir empty","Harshit"),
        (6,str(TODAY-timedelta(days=55)), "Servo drive fault — position error excessive","Ankit"),
        (8,str(TODAY-timedelta(days=50)), "Spindle bearing failure — machine locked up","Ravi"),
        (8,str(TODAY-timedelta(days=20)), "Control board fault — machine non-responsive",None),  # unresolved
    ])
    print("[OK] 11 breakdown logs (1 unresolved)")

    # 6. ISSUE RECORDS
    conn.executemany("INSERT INTO ISSUE_RECORD (part_id,equipment_id,issued_on,qty,issued_by,type) VALUES (?,?,?,?,?,?);", [
        (7, 1,str(TODAY-timedelta(days=90)), 3,"Harshit","issue"),
        (12,1,str(TODAY-timedelta(days=60)), 2,"Ankit",  "issue"),
        (13,1,str(TODAY-timedelta(days=30)), 1,"Harshit","issue"),
        (1, 2,str(TODAY-timedelta(days=120)),2,"Ravi",   "issue"),
        (8, 2,str(TODAY-timedelta(days=80)), 1,"Ravi",   "issue"),
        (9, 2,str(TODAY-timedelta(days=80)), 1,"Ravi",   "issue"),
        (1, 2,str(TODAY-timedelta(days=42)), 2,"Ravi",   "issue"),
        (8, 2,str(TODAY-timedelta(days=42)), 2,"Ravi",   "issue"),
        (9, 2,str(TODAY-timedelta(days=1)),  1,"Ankit",  "return"),
        (4, 3,str(TODAY-timedelta(days=60)), 2,"Sneha",  "issue"),
        (2, 3,str(TODAY-timedelta(days=30)), 1,"Ankit",  "issue"),
        (13,4,str(TODAY-timedelta(days=90)), 2,"Ankit",  "issue"),
        (6, 4,str(TODAY-timedelta(days=45)), 1,"Harshit","issue"),
        (4, 5,str(TODAY-timedelta(days=35)), 1,"Harshit","issue"),
        (7, 5,str(TODAY-timedelta(days=35)), 1,"Harshit","issue"),
        (10,5,str(TODAY-timedelta(days=20)), 2,"Ravi",   "issue"),
        (11,6,str(TODAY-timedelta(days=55)), 1,"Ankit",  "issue"),
        (14,6,str(TODAY-timedelta(days=21)), 2,"Sneha",  "issue"),
        (4, 8,str(TODAY-timedelta(days=50)), 2,"Ravi",   "issue"),
        (5, 8,str(TODAY-timedelta(days=50)), 1,"Ravi",   "issue"),
        (3, 8,str(TODAY-timedelta(days=20)), 6,"Harshit","issue"),
    ])
    print("[OK] 21 issue records")
    conn.commit()

def print_summary(conn):
    print("\n── Table row counts ────────────────────────────────────────────")
    for t in ['SUPPLIER','EQUIPMENT','PART','MAINTENANCE_LOG','BREAKDOWN_LOG','ISSUE_RECORD']:
        n = conn.execute(f"SELECT COUNT(*) FROM {t};").fetchone()[0]
        print(f"   {t:<22} → {n:>3} rows")

    print("\n── Cost per asset (Phase 4 KPI preview) ────────────────────────")
    rows = conn.execute("""
        SELECT e.name, ROUND(SUM(ir.qty * p.unit_cost),2) AS cost
        FROM ISSUE_RECORD ir
        JOIN EQUIPMENT e ON ir.equipment_id = e.id
        JOIN PART p ON ir.part_id = p.id
        WHERE ir.type = 'issue'
        GROUP BY e.id ORDER BY cost DESC;
    """).fetchall()
    for name, cost in rows:
        print(f"   {name:<25} Rs {cost:>10,.2f}")

    print("\n── Low stock alerts ─────────────────────────────────────────────")
    rows = conn.execute("SELECT name,qty_on_hand,min_qty FROM PART WHERE qty_on_hand <= min_qty;").fetchall()
    for name, qty, mn in rows:
        print(f"   ⚠  {name:<35} qty={qty}  min={mn}")

    print("────────────────────────────────────────────────────────────────\n")

if __name__ == '__main__':
    print(f"\n[seed_db] DB: {os.path.abspath(DB_PATH)}")
    if not os.path.exists(DB_PATH):
        print("[ERROR] DB not found. Run Java app first.")
        exit(1)
    conn = get_connection()
    if not verify_schema(conn):
        conn.close(); exit(1)
    if RESET:
        reset_data(conn)
    if already_seeded(conn) and not RESET:
        print("[INFO] Already seeded. Use --reset to reseed.")
    else:
        seed(conn)
    print_summary(conn)
    conn.close()
