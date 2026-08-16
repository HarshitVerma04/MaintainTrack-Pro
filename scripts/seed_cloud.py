"""
seed_cloud.py — MaintainTrack Pro
----------------------------------
Reads the local SQLite DB and pushes all data to the Spring Boot cloud API.
Run after the API is running:
    python scripts/seed_cloud.py
"""

import sqlite3, os, json, urllib.request, urllib.error

DB_PATH  = os.path.join(os.path.dirname(__file__), '..', 'data', 'maintaintrack.db')
BASE_URL = "http://localhost:8080"

# ── Auth ──────────────────────────────────────────────────────────────────────

def get_token():
    payload = json.dumps({"username": "harshit", "password": "Admin@1234"}).encode()
    req = urllib.request.Request(
        BASE_URL + "/auth/login",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())["token"]

def post(path, payload, token):
    data = json.dumps(payload).encode()
    req  = urllib.request.Request(
        BASE_URL + path,
        data=data,
        headers={
            "Content-Type":  "application/json",
            "Authorization": "Bearer " + token
        },
        method="POST"
    )
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"  [ERROR] {e.code} on POST {path}: {body[:120]}")
        return None

# ── Seed functions ────────────────────────────────────────────────────────────

def seed_suppliers(conn, token):
    print("\n── Suppliers ────────────────────────────────────────────────────")
    rows = conn.execute("SELECT id, name, contact_name, phone, email FROM SUPPLIER;").fetchall()
    id_map = {}
    for local_id, name, contact_name, phone, email in rows:
        result = post("/api/suppliers", {
            "name": name,
            "contactName": contact_name,
            "phone": phone,
            "email": email
        }, token)
        if result:
            id_map[local_id] = result["id"]
            print(f"  ✓ {name} → cloud id {result['id']}")
    return id_map

def seed_equipment(conn, token):
    print("\n── Equipment ─────────────────────────────────────────────────────")
    rows = conn.execute(
        "SELECT id, name, location, status, next_maintenance_date, interval_days FROM EQUIPMENT;"
    ).fetchall()
    id_map = {}
    for local_id, name, location, status, next_date, interval in rows:
        result = post("/api/equipment", {
            "name":                name,
            "location":            location,
            "status":              status,
            "nextMaintenanceDate": next_date,
            "intervalDays":        interval
        }, token)
        if result:
            id_map[local_id] = result["id"]
            print(f"  ✓ {name} → cloud id {result['id']}")
    return id_map

def seed_parts(conn, token, supplier_map):
    print("\n── Parts ─────────────────────────────────────────────────────────")
    rows = conn.execute(
        "SELECT id, supplier_id, name, qty_on_hand, min_qty, unit, unit_cost FROM PART;"
    ).fetchall()
    id_map = {}
    for local_id, sup_id, name, qty, min_qty, unit, cost in rows:
        cloud_sup_id = supplier_map.get(sup_id)
        url = f"/api/parts" + (f"?supplierId={cloud_sup_id}" if cloud_sup_id else "")
        result = post(url, {
            "name":       name,
            "qtyOnHand":  qty,
            "minQty":     min_qty,
            "unit":       unit,
            "unitCost":   cost
        }, token)
        if result:
            id_map[local_id] = result["id"]
            print(f"  ✓ {name} → cloud id {result['id']}")
    return id_map

def seed_maintenance(conn, token, equip_map):
    print("\n── Maintenance Logs ──────────────────────────────────────────────")
    rows = conn.execute(
        "SELECT equipment_id, done_on, notes, done_by FROM MAINTENANCE_LOG ORDER BY done_on;"
    ).fetchall()
    count = 0
    for eq_id, done_on, notes, done_by in rows:
        cloud_eq_id = equip_map.get(eq_id)
        if not cloud_eq_id:
            print(f"  [SKIP] equipment_id {eq_id} not in cloud")
            continue
        result = post("/api/maintenance/log", {
            "equipmentId": cloud_eq_id,
            "doneOn":      done_on,
            "notes":       notes or "",
            "doneBy":      done_by or "system"
        }, token)
        if result:
            count += 1
    print(f"  ✓ {count} maintenance logs seeded")

def seed_breakdowns(conn, token, equip_map):
    print("\n── Breakdown Logs ────────────────────────────────────────────────")
    rows = conn.execute(
        "SELECT equipment_id, occurred_on, description, resolved_by FROM BREAKDOWN_LOG ORDER BY occurred_on;"
    ).fetchall()
    id_map = {}
    local_rows = conn.execute(
        "SELECT id, equipment_id FROM BREAKDOWN_LOG ORDER BY occurred_on;"
    ).fetchall()
    count = 0
    for (local_id, _), (eq_id, occurred_on, description, resolved_by) in zip(local_rows, rows):
        cloud_eq_id = equip_map.get(eq_id)
        if not cloud_eq_id:
            print(f"  [SKIP] equipment_id {eq_id} not in cloud")
            continue
        result = post("/api/breakdowns", {
            "equipmentId": cloud_eq_id,
            "occurredOn":  occurred_on,
            "description": description or "",
            "resolvedBy":  resolved_by or ""
        }, token)
        if result:
            id_map[local_id] = result["id"]
            count += 1
    print(f"  ✓ {count} breakdown logs seeded")
    return id_map

def seed_issues(conn, token, part_map, equip_map):
    print("\n── Issue Records ─────────────────────────────────────────────────")
    rows = conn.execute(
        "SELECT part_id, equipment_id, issued_on, qty, issued_by, type FROM ISSUE_RECORD ORDER BY issued_on;"
    ).fetchall()
    count = 0
    for part_id, eq_id, issued_on, qty, issued_by, rec_type in rows:
        cloud_part_id = part_map.get(part_id)
        cloud_eq_id   = equip_map.get(eq_id)
        if not cloud_part_id or not cloud_eq_id:
            print(f"  [SKIP] part {part_id} or equip {eq_id} not in cloud")
            continue
        endpoint = "/api/parts/issue" if rec_type == "issue" else "/api/parts/return"
        result = post(endpoint, {
            "partId":      cloud_part_id,
            "equipmentId": cloud_eq_id,
            "qty":         qty,
            "issuedBy":    issued_by or "system",
            "issuedOn":    issued_on
        }, token)
        if result:
            count += 1
    print(f"  ✓ {count} issue records seeded")

# ── Main ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print(f"\n[seed_cloud] DB: {os.path.abspath(DB_PATH)}")

    if not os.path.exists(DB_PATH):
        print("[ERROR] DB not found.")
        exit(1)

    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA foreign_keys = ON;")

    print("[Auth] Logging in...")
    try:
        token = get_token()
        print(f"[Auth] Token acquired.")
    except Exception as e:
        print(f"[ERROR] Login failed: {e}")
        conn.close()
        exit(1)

    supplier_map = seed_suppliers(conn, token)
    equip_map    = seed_equipment(conn, token)
    part_map     = seed_parts(conn, token, supplier_map)
    seed_maintenance(conn, token, equip_map)
    seed_breakdowns(conn, token, equip_map)
    seed_issues(conn, token, part_map, equip_map)

    conn.close()
    print("\n[seed_cloud] Done. Cloud DB is now seeded with local data.")