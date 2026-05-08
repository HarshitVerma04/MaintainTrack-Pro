# 🏭 MaintainTrack Pro

> **The command center for industrial maintenance teams.**  
> Real-time asset visibility · Proactive maintenance scheduling · Zero-gap parts management

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-17-blue?style=for-the-badge&logo=java&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.10+-3776AB?style=for-the-badge&logo=python&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Tech Stack](#-tech-stack)
- [Features](#-features)
- [Database Schema](#-database-schema)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Implementation Roadmap](#-implementation-roadmap)
  - [Phase 1 — Setup & Data Layer (Days 1–5)](#phase-1--setup--data-layer-days-15)
  - [Phase 2 — Core Business Logic (Days 6–12)](#phase-2--core-business-logic-days-612)
  - [Phase 3 — Alert Engine (Days 13–19)](#phase-3--alert-engine-days-1319)
  - [Phase 4 — Dashboard, Reports & Launch (Days 20–28)](#phase-4--dashboard-reports--launch-days-2028)
- [Application Flow](#-application-flow)
- [Auth Roadmap (V2)](#-auth-roadmap-v2)
- [Contributing](#-contributing)

---

## 🎯 Overview

MaintainTrack Pro is a **desktop-first, Java + SQLite** operations management platform built for industrial maintenance teams. It replaces disconnected spreadsheets with a unified system for:

- Tracking every machine and its maintenance schedule
- Logging breakdowns and maintenance jobs with full traceability
- Managing spare parts inventory with low-stock alerting
- Generating PDF and Excel reports without any manual data wrangling

**Target Industries:** Manufacturing · Facilities Management · Fleet & Transport

| Metric | Impact |
|--------|--------|
| Unplanned Downtime | ↓ 35% |
| Parts Overspend | ↓ 28% |
| PM Compliance Rate | ↑ 40% |
| Incident Closure Speed | 3× Faster |

---

## 🛠 Tech Stack

| Layer | Technology | Role |
|-------|-----------|------|
| **UI Framework** | Java 17 + JavaFX 17 | All screens, navigation, forms (MVC pattern) |
| **Business Logic** | Java (Service layer) | Maintenance scheduling, stock logic, work orders |
| **Database Access** | Java JDBC (DAO pattern) | `EquipmentDAO`, `PartDAO`, `IssueRecordDAO`, etc. |
| **Database** | SQLite (`.db` file) | Single shared file — bridge between Java and Python |
| **Reporting** | Python 3 + pandas | Called via `ProcessBuilder`; no API required |
| **Excel Export** | Python 3 + openpyxl | Parts usage, maintenance history spreadsheets |
| **PDF Export** | Python 3 + fpdf2 | Formatted maintenance report per equipment |
| **Build Tool** | Apache Maven | Dependency management, packaging, jar output |
| **Java IDE** | IntelliJ IDEA | Primary Java development environment |
| **Python IDE** | VS Code | Python scripts in `/scripts` directory |

---

## ✨ Features

- **Asset Registry** — Searchable record of every machine with full maintenance history
- **Scheduled Maintenance** — Calendar-driven PMs with automatic next-due-date recalculation
- **Breakdown Logging** — Incident capture with description and resolver tracking
- **Parts & Stock Control** — Live inventory with issue/return transactions and reorder thresholds
- **Smart Alerts** — Automatic low-stock and overdue-maintenance flags on the dashboard
- **Operations Dashboard** — Live KPIs: uptime %, MTBF, cost per asset, parts spend
- **Activity Feed** — Merged chronological view of all recent actions across all tables
- **Data Export** — PDF maintenance reports and Excel parts-usage workbooks

---

## 🗄 Database Schema

Six tables with `EQUIPMENT` as the central entity:

```
SUPPLIER ─────────── supplies ──────────► PART
                                           │
                                        tracks
                                           │
EQUIPMENT ──── has ──► MAINTENANCE_LOG    ▼
    │                                 ISSUE_RECORD ◄── uses ── EQUIPMENT
    └──── has ──► BREAKDOWN_LOG
```

### Tables

| Table | Primary Key | Foreign Keys | Key Fields |
|-------|------------|-------------|-----------|
| `EQUIPMENT` | `id` | — | `name`, `location`, `status`, `next_maintenance_date`, `interval_days` |
| `MAINTENANCE_LOG` | `id` | `equipment_id` | `done_on`, `notes`, `done_by` |
| `BREAKDOWN_LOG` | `id` | `equipment_id` | `occurred_on`, `description`, `resolved_by` |
| `PART` | `id` | `supplier_id` | `name`, `qty_on_hand`, `min_qty`, `unit` |
| `ISSUE_RECORD` | `id` | `part_id`, `equipment_id` | `issued_on`, `qty`, `issued_by`, `type` |
| `SUPPLIER` | `id` | — | `name`, `contact_name`, `phone`, `email` |

> **Auth note:** `done_by` and `issued_by` are plain strings in the MVP. When auth is added in V2, they auto-populate from the logged-in session.

### SQL Schema

```sql
CREATE TABLE SUPPLIER (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT    NOT NULL,
    contact_name TEXT,
    phone        TEXT,
    email        TEXT
);

CREATE TABLE EQUIPMENT (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    name                  TEXT    NOT NULL,
    location              TEXT,
    status                TEXT    DEFAULT 'Operational',
    next_maintenance_date DATE,
    interval_days         INTEGER DEFAULT 30
);

CREATE TABLE PART (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    supplier_id INTEGER REFERENCES SUPPLIER(id),
    name        TEXT    NOT NULL,
    qty_on_hand INTEGER DEFAULT 0,
    min_qty     INTEGER DEFAULT 5,
    unit        TEXT    DEFAULT 'pcs'
);

CREATE TABLE MAINTENANCE_LOG (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    equipment_id INTEGER NOT NULL REFERENCES EQUIPMENT(id),
    done_on      DATE    NOT NULL,
    notes        TEXT,
    done_by      TEXT
);

CREATE TABLE BREAKDOWN_LOG (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    equipment_id INTEGER NOT NULL REFERENCES EQUIPMENT(id),
    occurred_on  DATE    NOT NULL,
    description  TEXT,
    resolved_by  TEXT
);

CREATE TABLE ISSUE_RECORD (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    part_id      INTEGER NOT NULL REFERENCES PART(id),
    equipment_id INTEGER NOT NULL REFERENCES EQUIPMENT(id),
    issued_on    DATE    NOT NULL,
    qty          INTEGER NOT NULL,
    issued_by    TEXT,
    type         TEXT    CHECK(type IN ('issue', 'return'))
);
```

---

## 📁 Project Structure

```
maintaintrack-pro/
│
├── java/                              ← Maven project (IntelliJ IDEA)
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/maintaintrack/
│           │   ├── MainApp.java              ← JavaFX entry point
│           │   ├── controllers/
│           │   │   ├── DashboardController.java
│           │   │   ├── EquipmentController.java
│           │   │   ├── PartController.java
│           │   │   ├── SupplierController.java
│           │   │   ├── MaintenanceLogController.java
│           │   │   ├── BreakdownLogController.java
│           │   │   └── IssueRecordController.java
│           │   ├── services/
│           │   │   ├── EquipmentService.java
│           │   │   ├── PartService.java
│           │   │   ├── AlertService.java     ← Phase 3
│           │   │   └── ReportService.java    ← Phase 4
│           │   ├── dao/
│           │   │   ├── DBConnection.java
│           │   │   ├── EquipmentDAO.java
│           │   │   ├── PartDAO.java
│           │   │   ├── SupplierDAO.java
│           │   │   ├── MaintenanceLogDAO.java
│           │   │   ├── BreakdownLogDAO.java
│           │   │   └── IssueRecordDAO.java
│           │   └── models/
│           │       ├── Equipment.java
│           │       ├── Part.java
│           │       ├── Supplier.java
│           │       ├── MaintenanceLog.java
│           │       ├── BreakdownLog.java
│           │       └── IssueRecord.java
│           └── resources/
│               ├── fxml/
│               │   ├── Dashboard.fxml
│               │   ├── Equipment.fxml
│               │   ├── Parts.fxml
│               │   └── ...
│               └── styles/
│                   └── app.css
│
├── scripts/                           ← Python utilities (VS Code)
│   ├── requirements.txt
│   ├── generate_report.py             ← PDF maintenance report
│   ├── export_parts.py                ← Excel parts usage export
│   └── seed_db.py                     ← Dev seed data
│
├── data/
│   └── maintaintrack.db               ← Shared SQLite file
│
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java JDK | 17+ | [adoptium.net](https://adoptium.net) |
| JavaFX SDK | 17+ | [gluonhq.com/products/javafx](https://gluonhq.com/products/javafx) |
| Apache Maven | 3.8+ | [maven.apache.org](https://maven.apache.org) |
| Python | 3.10+ | [python.org](https://python.org) |
| IntelliJ IDEA | Latest | [jetbrains.com](https://jetbrains.com/idea) |
| VS Code | Latest | [code.visualstudio.com](https://code.visualstudio.com) |

### Setup

```bash
# 1. Clone the repository
git clone https://github.com/your-org/maintaintrack-pro.git
cd maintaintrack-pro

# 2. Install Python dependencies
pip install -r scripts/requirements.txt

# 3. Initialise the database
python3 scripts/seed_db.py

# 4. Build and run the Java app
cd java
mvn clean javafx:run
```

### Python dependencies (`scripts/requirements.txt`)

```
pandas==2.2.0
openpyxl==3.1.2
fpdf2==2.7.9
```

---

## 🗺 Implementation Roadmap

> **Total: 28 days across 4 phases.**  
> Each phase has clear exit criteria — do not advance until the current phase is stable.

---

### Phase 1 — Setup & Data Layer (Days 1–5)

**Goal:** Get the schema right before touching any logic. All downstream work depends on this.

| Day | Task | Deliverable | Tech |
|-----|------|-------------|------|
| 1 | Project scaffold — Maven `pom.xml`, folder structure, Python venv | Both IDEs set up and building | Maven + pip |
| 2 | Database schema — all 6 tables with FK constraints | `maintaintrack.db` created, schema verified with test inserts | SQLite + JDBC |
| 3 | Equipment CRUD screen — add, edit, delete, list machines | Working JavaFX form writing to `EQUIPMENT` table | JavaFX + JDBC |
| 4 | Parts CRUD screen — add, edit, delete, list spare parts | Working JavaFX form writing to `PART` table | JavaFX + JDBC |
| 5 | Suppliers CRUD screen — add, edit, delete, list suppliers | Working JavaFX form writing to `SUPPLIER` table | JavaFX + JDBC |

**Rules:**
- No business logic this phase — pure reads and writes only
- Validate all FK constraints with test inserts before Day 3
- Use a DB viewer (e.g. DB Browser for SQLite) to inspect data visually

**Exit Criteria:**
- [ ] All 6 tables exist with correct types and FK constraints
- [ ] Equipment, Parts, Suppliers screens allow full CRUD without errors
- [ ] At least 3 equipment records and 5 parts can be inserted and retrieved cleanly

---

### Phase 2 — Core Business Logic (Days 6–12)

**Goal:** Make the workflows come alive — scheduling, stock tracking, and full traceability.

| Day | Task | Deliverable | Tech |
|-----|------|-------------|------|
| 6 | Maintenance log form — capture date, notes, done_by | `MAINTENANCE_LOG` row created on submit | Java + JDBC |
| 7 | Next-due date recalculation — `done_on + interval_days` | `EQUIPMENT.next_maintenance_date` auto-updates after logging | Java (date arithmetic) |
| 8 | Breakdown log form — capture occurred_on, description, resolved_by | `BREAKDOWN_LOG` row created; optional entry | Java + JDBC |
| 9 | Work order system — link breakdown to equipment + parts used | Work order stored with `equipment_id` FK | Java + JDBC |
| 10 | Issue / return form — log part transactions with quantity | `ISSUE_RECORD` row created; `type` field captures direction | Java + JDBC |
| 11 | Stock quantity update — subtract issued qty, add returned qty | `PART.qty_on_hand` reflects every transaction correctly | Java (transactional) |
| 12 | Parts-link logic — connect `ISSUE_RECORD` to equipment + maintenance | Full traceability: part → job → equipment | Java + JDBC joins |

**Key Logic:**
- **Next-due formula:** `next_maintenance_date = done_on + interval_days`
- **Stock update is transactional** — if `ISSUE_RECORD` insert fails, `qty_on_hand` must NOT change
- **`type` field** in `ISSUE_RECORD` — `'issue'` decrements, `'return'` increments
- Breakdown form is **optional** — not every equipment check finds a fault

**Exit Criteria:**
- [ ] Logging a maintenance job auto-updates `next_maintenance_date` correctly
- [ ] Issuing a part decrements `qty_on_hand`; returning increments it
- [ ] `ISSUE_RECORD` correctly links to both `part_id` and `equipment_id`
- [ ] Stock updates are atomic — partial failures leave inventory unchanged

---

### Phase 3 — Alert Engine (Days 13–19)

**Goal:** Build the intelligence layer — proactive detection of problems before users notice them.

| Day | Task | Deliverable | Tech |
|-----|------|-------------|------|
| 13 | Low-stock detection — compare `qty_on_hand` vs `min_qty` for all parts | List of below-threshold parts generated on demand | Java + JDBC |
| 14 | Overdue maintenance detection — compare `next_maintenance_date` vs today | List of overdue equipment generated | Java (`LocalDate.now()`) |
| 15 | Alert data model — represent active alerts with type, severity, timestamp | `Alert` model class + in-memory alert store | Java |
| 16 | Dashboard alert feed — surface active alerts on the home screen | Alerts visible on dashboard, colour-coded by severity | JavaFX + CSS |
| 17 | Background polling — schedule alert checks every N minutes | Poll runs without user interaction | `ScheduledExecutorService` |
| 18 | UI notification dispatch — push alert messages into the dashboard feed | Alert text shown in alert pane in real time | `Platform.runLater()` |
| 19 | Alert resolution — mark alert as resolved when condition clears | Alert disappears when stock is refilled or job is logged | Condition re-check on DB write |

**Background Thread Pattern (Java Desktop):**

```java
// AlertService.java
ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
scheduler.scheduleAtFixedRate(() -> {
    List<Alert> alerts = checkAllConditions();  // DB reads
    Platform.runLater(() -> dashboardController.updateAlerts(alerts));
}, 0, 5, TimeUnit.MINUTES);
```

> ⚠️ **Never update JavaFX UI nodes from a background thread.** Always wrap UI updates in `Platform.runLater()`.

**Exit Criteria:**
- [ ] A part with `qty_on_hand < min_qty` automatically appears as an alert on the dashboard
- [ ] Equipment with `next_maintenance_date < today` shows as overdue
- [ ] Alerts self-resolve when the underlying condition is fixed
- [ ] Background polling runs without user interaction and does not block the UI

---

### Phase 4 — Dashboard, Reports & Launch (Days 20–28)

**Goal:** Assembly and quality. No new features — surface existing data, export it, and ship it.

| Day | Task | Deliverable | Tech |
|-----|------|-------------|------|
| 20 | Dashboard layout — KPI tiles, alert feed, navigation bar | Dashboard renders with live data | JavaFX FXML |
| 21 | KPI: Uptime % — calculated from `BREAKDOWN_LOG` downtimes | Uptime % shown per machine | SQL aggregation |
| 22 | KPI: MTBF — mean time between failures per equipment | MTBF shown alongside each asset | Java date math |
| 23 | KPI: Cost per asset — total parts cost from `ISSUE_RECORD` | Cost per equipment displayed on dashboard | `SQL GROUP BY` |
| 24 | Activity feed — merged view of maintenance, breakdowns, issues | Single sorted list from 3 tables chronologically | `SQL UNION` |
| 25 | PDF maintenance report — formatted report per equipment | Downloadable PDF generated by Python | Python + fpdf2 |
| 26 | Excel parts usage export — parts spend and usage over a time period | Downloadable XLSX | Python + openpyxl |
| 27 | Full QA pass — end-to-end test all workflows | No critical bugs; all exit criteria across all phases met | Manual + JUnit |
| 28 | Packaging & deployment — build fat JAR, ship to first team | App running on target machine | `mvn package` |

**Python Bridge Pattern:**

```java
// ReportService.java — calls Python from Java
ProcessBuilder pb = new ProcessBuilder(
    "python3", "scripts/generate_report.py",
    "--equipment-id", String.valueOf(equipmentId),
    "--output", outputPath
);
pb.directory(new File(System.getProperty("user.dir")));
pb.start().waitFor();
```

**Activity Feed SQL:**

```sql
SELECT 'maintenance' AS type, done_on AS event_date, notes AS detail, equipment_id
FROM MAINTENANCE_LOG
UNION ALL
SELECT 'breakdown', occurred_on, description, equipment_id
FROM BREAKDOWN_LOG
UNION ALL
SELECT 'issue', issued_on, CAST(qty AS TEXT) || ' × ' || part_id, equipment_id
FROM ISSUE_RECORD
ORDER BY event_date DESC
LIMIT 50;
```

**Launch Checklist:**
- [ ] Dashboard shows live KPIs: uptime, MTBF, cost per asset, parts spend
- [ ] Activity feed shows merged timeline of all recent actions
- [ ] PDF and Excel exports download correctly with accurate data
- [ ] All Phase 1–3 workflows pass end-to-end manual testing
- [ ] No unresolved critical bugs
- [ ] Fat JAR builds cleanly via `mvn package`

---

## 🔄 Application Flow

```
Open Dashboard
├── Equipment Side
│   ├── View equipment list (all machines + status)
│   └── Select equipment → Open profile
│       ├── Maintenance due?
│       │   ├── YES → Log maintenance (date, notes, done_by)
│       │   │         → Auto-update next_maintenance_date
│       │   └── NO  → Optionally log breakdown
│       └── Dashboard updated
│
└── Parts Side
    ├── View parts list (stock levels + low-stock alerts)
    └── Select part → Open details
        ├── Stock below minimum?
        │   ├── YES → Flag alert on dashboard
        │   └── NO  → Issue / return part
        │               → Update qty_on_hand
        └── Dashboard updated
```

---

## 🔐 Auth Roadmap (V2)

Auth was deliberately cut from the MVP — it alone consumes a full development week and is unnecessary for a small trusted internal team.

| Component | MVP (Now) | V2 (With Auth) |
|-----------|-----------|----------------|
| `done_by` / `issued_by` | Typed manually | Auto-filled from session |
| Access control | None — open to all | Role-based: Technician vs Manager |
| Login screen | Not present | Username + password form |
| Session management | Not present | Java session object |
| Dashboard scope | All data visible | Filtered to assigned equipment |
| Report access | Anyone | Manager role only |

**When to add auth:** When the team grows beyond ~5 people, or when you need audit trails tied to specific individuals rather than typed names.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit with a clear message: `git commit -m "feat: add low-stock alert polling"`
4. Push: `git push origin feature/your-feature-name`
5. Open a Pull Request — include which phase the change belongs to

**Branch naming convention:**
- `phase1/equipment-crud`
- `phase2/maintenance-scheduler`
- `phase3/alert-engine`
- `phase4/dashboard-kpis`

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

<div align="center">
  <strong>MaintainTrack Pro</strong> — Built for the teams that keep everything running.
</div>
