# 🏭 MaintainTrack Pro

> **The command center for industrial maintenance teams.**  
> Real-time asset visibility · Proactive maintenance scheduling · Zero-gap parts management

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-17-blue?style=for-the-badge&logo=java&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.10+-3776AB?style=for-the-badge&logo=python&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![Release](https://img.shields.io/badge/Release-v1.0.1-brightgreen?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Tech Stack](#-tech-stack)
- [Features](#-features)
- [Screens](#-screens)
- [Database Schema](#-database-schema)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Deployment](#-deployment)
- [Application Flow](#-application-flow)
- [Team & Phase Split](#-team--phase-split)
- [V2 Roadmap](#-v2-roadmap)
- [Contributing](#-contributing)

---

## 🎯 Overview

MaintainTrack Pro is a **desktop-first, Java + SQLite** operations management platform built for industrial maintenance teams. It replaces disconnected spreadsheets with a unified system for:

- Tracking every machine and its maintenance schedule
- Logging breakdowns and maintenance jobs with full traceability
- Managing spare parts inventory with low-stock alerting
- Linking issued parts to specific breakdown repairs or PM jobs (work order system)
- Generating PDF and Excel reports without any manual data wrangling

**Target Industries:** Manufacturing · Facilities Management · Fleet & Transport · Defence

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
| **Business Logic** | Java (Service layer) | Maintenance scheduling, stock logic, work orders, KPI calculations |
| **Database Access** | Java JDBC (DAO pattern) | All SQL via typed DAO classes |
| **Database** | SQLite (`.db` file) | Local file — shared between Java and Python scripts |
| **Alert Engine** | Java `ScheduledExecutorService` | Background polling every 30s; `Platform.runLater()` for thread-safe UI updates |
| **PDF Reports** | Python 3 + fpdf2 | Per-equipment maintenance reports; called via `ProcessBuilder` |
| **Excel Export** | Python 3 + openpyxl | 4-sheet parts usage workbook; called via `ProcessBuilder` |
| **Build Tool** | Apache Maven | Dependency management, fat JAR packaging |
| **Packaging** | jlink + jpackage + WiX 3.11 | Standalone Windows `.exe` installer — no Java or Python required on target machine |
| **Python Bundling** | PyInstaller | Python scripts compiled to `.exe` and bundled inside the installer |
| **Java IDE** | IntelliJ IDEA | Primary Java development environment |
| **Python IDE** | VS Code | Python scripts in `/scripts` directory |

---

## ✨ Features

- **Asset Registry** — Searchable record of every machine with location, status, and maintenance schedule
- **Scheduled Maintenance** — Calendar-driven PMs with automatic `next_maintenance_date` recalculation (`done_on + interval_days`)
- **Breakdown Logging** — Incident capture with description, resolver, and unresolved status tracking
- **Parts & Stock Control** — Live inventory with issue/return transactions, reorder thresholds, and low-stock badges
- **Work Order System** — Issue records optionally linked to a specific breakdown repair or maintenance job for full cost traceability
- **Smart Alert Engine** — Background polling detects low-stock parts and overdue maintenance; alerts self-resolve when conditions clear
- **Operations Dashboard** — Live KPI tiles: total equipment, maintenance job count, total parts spend, active alert count
- **KPIs Screen** — Three-tab analytics: Uptime % per machine (90-day window), MTBF per machine, Cost per asset with proportional bars
- **Activity Feed** — Filterable merged chronological view of all events across all three log tables (`UNION ALL`)
- **PDF Maintenance Report** — Formatted report per equipment: profile, maintenance history table, breakdown table, parts cost summary
- **Excel Parts Export** — 4-sheet workbook: Parts Summary, Issue History, Cost Per Asset, Low Stock Alerts
- **Standalone Installer** — `.exe` installer bundles Java JRE, JavaFX, and Python scripts — zero prerequisites on target machine

---

## 🖥 Screens

| Screen | Description |
|--------|-------------|
| **Dashboard** | KPI tiles, alert feed, cost-per-asset bars, recent activity table |
| **Equipment** | Full CRUD — add/edit/delete machines, colour-coded status and overdue dates |
| **Parts & Inventory** | Full CRUD — stock levels, supplier link, low-stock badge, colour-coded qty |
| **Suppliers** | Full CRUD — contact details, linked to parts |
| **KPIs & Analytics** | Uptime %, MTBF, Cost Per Asset — all in a 3-tab screen with visual bars |
| **Activity Feed** | Merged feed with Type / Date / Equipment filters and keyword search |
| **Reports & Exports** | Generate PDF per equipment; export all parts data to Excel |
| **Maintenance Log** | Log PM jobs; equipment next-due date recalculates automatically on save |
| **Breakdowns** | Log incidents; unresolved shown in red; total breakdown badge in header |
| **Issues & Alerts** | Issue/return parts with optional work order link; real-time alert feed sidebar |

---

## 🗄 Database Schema

Six tables with `EQUIPMENT` as the central entity. `ISSUE_RECORD` links to both `BREAKDOWN_LOG` and `MAINTENANCE_LOG` for work order traceability.

```
SUPPLIER ────── supplies ──────► PART
                                  │
                               tracks
                                  │
EQUIPMENT ── has ──► MAINTENANCE_LOG    ▼
    │                            ISSUE_RECORD ◄── uses ── EQUIPMENT
    └──── has ──► BREAKDOWN_LOG      ▲               ▲
                       │             │               │
                       └── work order link ──────────┘
                           (breakdown_id / maintenance_id)
```

### Tables

| Table | Primary Key | Foreign Keys | Key Fields |
|-------|------------|-------------|-----------|
| `EQUIPMENT` | `id` | — | `name`, `location`, `status`, `next_maintenance_date`, `interval_days` |
| `MAINTENANCE_LOG` | `id` | `equipment_id` | `done_on`, `notes`, `done_by` |
| `BREAKDOWN_LOG` | `id` | `equipment_id` | `occurred_on`, `description`, `resolved_by` |
| `PART` | `id` | `supplier_id` | `name`, `qty_on_hand`, `min_qty`, `unit`, `unit_cost` |
| `ISSUE_RECORD` | `id` | `part_id`, `equipment_id`, `breakdown_id`\*, `maintenance_id`\* | `issued_on`, `qty`, `issued_by`, `type` |
| `SUPPLIER` | `id` | — | `name`, `contact_name`, `phone`, `email` |

\* Optional FK — null for standalone stock draws, set for work order issues.

> **Note:** `done_by` and `issued_by` are plain strings in the MVP. When auth is added in V2, they auto-populate from the logged-in user session.

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
    next_maintenance_date TEXT,
    interval_days         INTEGER DEFAULT 30
);

CREATE TABLE PART (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    supplier_id INTEGER REFERENCES SUPPLIER(id),
    name        TEXT    NOT NULL,
    qty_on_hand INTEGER DEFAULT 0,
    min_qty     INTEGER DEFAULT 5,
    unit        TEXT    DEFAULT 'pcs',
    unit_cost   REAL    DEFAULT 0.0
);

CREATE TABLE MAINTENANCE_LOG (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    equipment_id INTEGER NOT NULL REFERENCES EQUIPMENT(id),
    done_on      TEXT    NOT NULL,
    notes        TEXT,
    done_by      TEXT
);

CREATE TABLE BREAKDOWN_LOG (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    equipment_id INTEGER NOT NULL REFERENCES EQUIPMENT(id),
    occurred_on  TEXT    NOT NULL,
    description  TEXT,
    resolved_by  TEXT
);

CREATE TABLE ISSUE_RECORD (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    part_id        INTEGER NOT NULL REFERENCES PART(id),
    equipment_id   INTEGER NOT NULL REFERENCES EQUIPMENT(id),
    breakdown_id   INTEGER          REFERENCES BREAKDOWN_LOG(id),
    maintenance_id INTEGER          REFERENCES MAINTENANCE_LOG(id),
    issued_on      TEXT    NOT NULL,
    qty            INTEGER NOT NULL,
    issued_by      TEXT,
    type           TEXT    CHECK(type IN ('issue', 'return'))
);
```

`breakdown_id` and `maintenance_id` are added via `ALTER TABLE` migration on first launch if they don't exist — existing databases are upgraded automatically without a reseed.

---

## 📁 Project Structure

```
maintaintrack-pro/
│
├── java/                                   ← Maven project (IntelliJ IDEA)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/maintaintrack/
│       │   ├── MainApp.java                ← JavaFX entry point + screen bounds fix
│       │   ├── controllers/
│       │   │   ├── MainLayoutController.java   ← sidebar nav + alert polling lifecycle
│       │   │   ├── DashboardController.java    ← KPI tiles, alert feed, activity table
│       │   │   ├── KpisController.java         ← 3-tab KPI screen (uptime/MTBF/cost)
│       │   │   ├── ActivityController.java     ← filtered UNION feed
│       │   │   ├── ReportsController.java      ← PDF + Excel triggers
│       │   │   ├── EquipmentController.java
│       │   │   ├── PartController.java
│       │   │   ├── SupplierController.java
│       │   │   ├── MaintenanceController.java
│       │   │   ├── BreakdownController.java
│       │   │   └── IssueController.java        ← work order dropdowns + alert feed
│       │   ├── services/
│       │   │   ├── EquipmentService.java
│       │   │   ├── PartService.java
│       │   │   ├── SupplierService.java
│       │   │   ├── MaintenanceLogService.java  ← next-due recalculation logic
│       │   │   ├── BreakdownLogService.java
│       │   │   ├── IssueRecordService.java     ← transactional stock update
│       │   │   ├── WorkOrderService.java       ← traceability aggregator
│       │   │   ├── AlertService.java           ← low-stock + overdue detection
│       │   │   ├── AlertPollingService.java    ← ScheduledExecutorService wrapper
│       │   │   ├── DashboardService.java       ← KPI SQL queries
│       │   │   ├── KpiService.java             ← uptime%, MTBF, cost per asset
│       │   │   └── ReportService.java          ← ProcessBuilder bridge to Python
│       │   ├── dao/
│       │   │   ├── DBConnection.java           ← dev/prod path resolution
│       │   │   ├── DatabaseInitializer.java    ← schema creation + migrations
│       │   │   ├── EquipmentDAO.java
│       │   │   ├── PartDAO.java
│       │   │   ├── SupplierDAO.java
│       │   │   ├── MaintenanceLogDAO.java
│       │   │   ├── BreakdownLogDAO.java
│       │   │   └── IssueRecordDAO.java         ← findByBreakdown + findByMaintenance
│       │   └── models/
│       │       ├── Equipment.java
│       │       ├── Part.java
│       │       ├── Supplier.java
│       │       ├── MaintenanceLog.java
│       │       ├── BreakdownLog.java
│       │       ├── IssueRecord.java            ← breakdownId + maintenanceId fields
│       │       └── Alert.java                  ← type, severity, icon, timestamp
│       └── resources/
│           ├── fxml/
│           │   ├── MainLayout.fxml             ← sidebar + content area shell
│           │   ├── Dashboard.fxml
│           │   ├── Kpis.fxml
│           │   ├── Activity.fxml
│           │   ├── Reports.fxml
│           │   ├── Equipment.fxml
│           │   ├── Parts.fxml
│           │   ├── Suppliers.fxml
│           │   ├── Maintenance.fxml
│           │   ├── Breakdown.fxml
│           │   └── Issues.fxml
│           └── styles/
│               └── app.css
│
├── scripts/                                ← Python utilities
│   ├── requirements.txt                    ← fpdf2, openpyxl, pandas
│   ├── generate_report.py                  ← PDF report (fpdf2); PyInstaller-safe paths
│   ├── export_parts.py                     ← Excel export (openpyxl); 4 sheets
│   └── seed_db.py                          ← dev seed data with --reset flag
│
├── bundled/                                ← PyInstaller output (gitignored)
│   ├── generate_report.exe
│   └── export_parts.exe
│
├── data/
│   └── maintaintrack.db                    ← dev SQLite file (gitignored)
│
├── .gitignore
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java JDK | 17+ | [adoptium.net](https://adoptium.net) |
| JavaFX SDK | 17.0.19 | [gluonhq.com/products/javafx](https://gluonhq.com/products/javafx) |
| Apache Maven | 3.8+ | [maven.apache.org](https://maven.apache.org) |
| Python | 3.10+ | [python.org](https://python.org) |
| IntelliJ IDEA | Latest | [jetbrains.com](https://jetbrains.com/idea) |
| VS Code | Latest | [code.visualstudio.com](https://code.visualstudio.com) |

### Setup

```bash
# 1. Clone the repository
git clone https://github.com/HarshitVerma04/MaintainTrack-Pro.git
cd MaintainTrack-Pro

# 2. Create Python venv and install dependencies
cd scripts
python -m venv venv
venv\Scripts\activate        # Windows
pip install -r requirements.txt
cd ..

# 3. Run the Java app once to create the database schema
# (Use IntelliJ Run button or:)
# cd java && mvn clean javafx:run

# 4. Seed the database with sample data
python scripts/seed_db.py

# 5. Run the app
# IntelliJ: press the green ▶ Run button (MainApp run config)
# Terminal: cd java && mvn clean javafx:run
```

### IntelliJ Run Configuration

The run configuration must have **Working Directory** set to the project root (`maintaintrack-pro/`), not the `java/` subfolder. This ensures `data/maintaintrack.db` and `scripts/` resolve correctly.

VM Options required:
```
--module-path "C:\javafx-sdk-17.0.19\lib" --add-modules javafx.controls,javafx.fxml
```

### Reseed with fresh data

```bash
python scripts/seed_db.py --reset
```

---

## 📦 Deployment

The app ships as a fully standalone Windows `.exe` installer — no Java, Python, or JavaFX required on the target machine.

### Production Database Location

In production the database lives at:
```
C:\Users\<name>\AppData\Roaming\MaintainTrackPro\maintaintrack.db
```

`DBConnection.java` detects dev vs prod automatically — if `scripts/` folder doesn't exist (installed build), it uses AppData. Database survives uninstalls and app updates.

### Building the Installer

**Step 1 — Bundle Python scripts with PyInstaller**

```bash
scripts\venv\Scripts\activate
pyinstaller --onefile scripts/generate_report.py --distpath bundled/
pyinstaller --onefile scripts/export_parts.py --distpath bundled/
```

**Step 2 — Build the fat JAR**

```bash
cd java
mvn clean package
cd ..
```

**Step 3 — Copy bundled exes into target**

```powershell
mkdir java\target\bundled
copy bundled\generate_report.exe java\target\bundled\generate_report.exe
copy bundled\export_parts.exe java\target\bundled\export_parts.exe
```

**Step 4 — Build custom JRE with JavaFX baked in**

Download JavaFX 17 jmods from gluonhq.com, extract to `C:\javafx-jmods-17.0.19`, then:

```powershell
& "C:\Program Files\Java\jdk-23\bin\jlink.exe" `
  --no-header-files --no-man-pages --compress=2 `
  --module-path "C:\Program Files\Java\jdk-23\jmods;C:\javafx-jmods-17.0.19" `
  --add-modules java.base,java.sql,java.desktop,java.logging,javafx.controls,javafx.fxml,javafx.graphics,javafx.base `
  --output custom-jre
```

**Step 5 — Run jpackage**

Requires [WiX Toolset 3.11](https://github.com/wixtoolset/wix3/releases/tag/wix3112rtm) installed and on PATH.

```powershell
& "C:\Program Files\Java\jdk-23\bin\jpackage.exe" `
  --input java/target `
  --name "MaintainTrack Pro" `
  --main-jar maintaintrack-pro-1.0.0.jar `
  --main-class com.maintaintrack.MainApp `
  --type exe `
  --dest installer `
  --runtime-image custom-jre `
  --win-shortcut --win-menu --win-dir-chooser `
  --app-version 1.0.1 `
  --vendor "MaintainTrack"
```

> **Note:** Windows SmartScreen will warn on first run since the exe is unsigned. Click "More info → Run anyway". This is expected for software without a code signing certificate.

### Download

Pre-built installer available on the [Releases page](https://github.com/HarshitVerma04/MaintainTrack-Pro/releases).

---

## 🔄 Application Flow

```
Open Dashboard
├── Equipment Side
│   ├── View equipment list (all machines + status + overdue flag)
│   └── Select equipment → Maintenance Log
│       ├── Log PM job (date, notes, done_by)
│       │   → next_maintenance_date auto-recalculates
│       └── Log breakdown (description, resolved_by)
│
├── Parts Side
│   ├── View parts list (stock levels + low-stock badges)
│   └── Issues & Alerts screen
│       ├── Issue / return a part (transactional qty update)
│       ├── Link to breakdown → repair work order
│       └── Link to maintenance job → PM consumables tracking
│
└── Dashboard & Analytics
    ├── KPI tiles (equipment count, maintenance jobs, spend, alerts)
    ├── Alert feed (overdue maintenance + low stock — auto-polling)
    ├── KPIs screen (uptime %, MTBF, cost per asset)
    ├── Activity feed (merged chronological view, filterable)
    └── Reports (PDF per equipment, Excel parts export)
```

---

## 👥 Team & Phase Split

| Phase | Days | Owner | Scope |
|-------|------|-------|-------|
| **Phase 1** | 1–5 | Harshit | Project scaffold, DB schema, Equipment/Parts/Suppliers CRUD |
| **Phase 2** | 6–9 | Harshit | Maintenance scheduler, Breakdown log, Work order system (Days 8-9) |
| **Phase 2** | 10–12 | Adarsh | Issue/Return form, transactional stock update |
| **Phase 3** | 13–19 | Adarsh | Alert engine, background polling, `AlertPollingService` |
| **Phase 4** | 20–28 | Harshit | Dashboard, KPIs, Activity Feed, PDF/Excel reports, deployment |

**Branch convention used:**
- `AdarshVishwaraj-phase3-complete-set-of-files-with-previous-phases-combined` — Adarsh
- `phase4/dashboard-kpis` — Harshit
- `test/full-merge` — integration testing branch before merging to `main`

---

## 🗺 V2 Roadmap

### Authentication

Auth was deliberately excluded from the MVP — it consumes a full development week and is unnecessary for a small trusted internal team.

| Component | MVP (Now) | V2 (With Auth) |
|-----------|-----------|----------------|
| `done_by` / `issued_by` | Typed manually | Auto-filled from session |
| Access control | None | Role-based: Technician vs Manager |
| Login screen | Not present | Username + password + JWT |
| Dashboard scope | All data visible | Filtered to assigned equipment |
| Report access | Anyone | Manager role only |

### Cloud Sync + Web Version

The planned V2 architecture:

```
Desktop App (JavaFX)     Web App (React)
       │                       │
       ▼                       ▼
  Local SQLite  ◄─ sync ─►  Cloud PostgreSQL
                   REST API
                (Spring Boot)
```

**Phase plan:**
- **V2.1** — Spring Boot REST API wrapping existing service/DAO layer
- **V2.2** — JWT auth, User table, login screen in JavaFX
- **V2.3** — Cloud sync (push-on-save with offline queue)
- **V2.4** — React web frontend consuming the same API
- **V2.5** — Deploy to cloud (Vercel + Railway + Supabase)

The existing Java service and DAO classes are reused without modification — only the controllers change (JavaFX → `@RestController`).

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit with a clear message: `git commit -m "feat: add low-stock alert polling"`
4. Push: `git push origin feature/your-feature-name`
5. Open a Pull Request — include which phase the change belongs to

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

<div align="center">
  <strong>MaintainTrack Pro v1.0.1</strong> — Built for the teams that keep everything running.
</div>
