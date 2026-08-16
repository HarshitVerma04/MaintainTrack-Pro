# 🏭 MaintainTrack Pro

> **The command center for industrial maintenance teams.**  
> Cloud + Desktop Hybrid · Real-time Asset Visibility · Proactive Maintenance Scheduling · Zero-gap Parts Management

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Release](https://img.shields.io/badge/Release-v2.0.0-brightgreen?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

[![Live API](https://img.shields.io/badge/API-Render-46E3B7?style=flat&logo=render)](https://maintaintrack-pro.onrender.com/actuator/health)
[![Live Web](https://img.shields.io/badge/Web-Vercel-black?style=flat&logo=vercel)](https://maintaintrack-pro.vercel.app)
[![API Docs](https://img.shields.io/badge/Docs-Swagger-85EA2D?style=flat&logo=swagger)](https://maintaintrack-pro.onrender.com/swagger-ui.html)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Features](#-features)
- [Screens](#-screens)
- [Roles & Permissions](#-roles--permissions)
- [Database Schema](#-database-schema)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [API Documentation](#-api-documentation)
- [Deployment](#-deployment)
- [Security](#-security)
- [Performance](#-performance)
- [Desktop Installer Build](#-desktop-installer-build)
- [Application Flow](#-application-flow)

---

## 🎯 Overview

MaintainTrack Pro is a **Cloud + Desktop hybrid** operations management platform built for industrial maintenance teams. V2 extends the original JavaFX desktop app with a full-stack web deployment — React frontend on Vercel, Spring Boot REST API on Render, and a shared PostgreSQL database on Supabase.

It replaces disconnected spreadsheets with a unified system for:

- Tracking every machine and its maintenance schedule
- Logging breakdowns and maintenance jobs with full traceability
- Managing spare parts inventory with low-stock alerting
- Linking issued parts to specific breakdown repairs or PM jobs
- Generating PDF and Excel reports without any manual data wrangling
- Role-based access control across web and desktop

**Target Industries:** Manufacturing · Facilities Management · Fleet & Transport · Defence

| Metric | Impact |
|--------|--------|
| Unplanned Downtime | ↓ 35% |
| Parts Overspend | ↓ 28% |
| PM Compliance Rate | ↑ 40% |
| Incident Closure Speed | 3× Faster |

| Feature | Web | Desktop |
|---|---|---|
| Equipment CRUD | ✅ | ✅ |
| Maintenance logs | ✅ | ✅ |
| Breakdown tracking | ✅ | ✅ |
| Parts inventory + issue | ✅ | ✅ |
| Work orders | ✅ | ✅ |
| Suppliers directory | ✅ | ✅ |
| Dashboard with charts | ✅ | ✅ |
| User management (RBAC) | ✅ | — |
| Works offline | — | ✅ |
| PDF / Excel reports | — | ✅ |

---

## 🏗 Architecture

```
┌─────────────────────┐         ┌──────────────────────────┐
│   React Frontend    │  HTTPS  │   Spring Boot REST API   │
│   (Vercel)          │◄───────►│   (Render)               │
└─────────────────────┘         └────────────┬─────────────┘
                                             │ JDBC
┌─────────────────────┐                      ▼
│   JavaFX Desktop    │  HTTPS  ┌──────────────────────────┐
│   (Windows .exe)    │◄───────►│   Supabase PostgreSQL    │
└─────────────────────┘         └──────────────────────────┘
```

- **Frontend** — React 18 + Vite + Tailwind CSS, deployed on Vercel
- **Backend** — Spring Boot 3.2, Spring Security (JWT), deployed on Render
- **Desktop** — JavaFX 21, packaged as a Windows installer via jpackage
- **Database** — PostgreSQL on Supabase (shared between web and desktop)

---

## 🛠 Tech Stack

| Layer | Technology | Role |
|-------|-----------|------|
| **Web Frontend** | React 18 + Vite + Tailwind CSS | All web screens, routing, forms |
| **Charts** | Recharts | Dashboard bar charts and doughnut chart |
| **REST API** | Spring Boot 3.2 | All endpoints, business logic, auth |
| **Security** | Spring Security + JWT (jjwt 0.12.5) | Stateless auth, RBAC, BCrypt |
| **Rate Limiting** | Bucket4j | 10 login attempts/min per IP |
| **Cache** | Caffeine | In-memory cache on dashboard + equipment + parts |
| **API Docs** | SpringDoc OpenAPI 3 | Auto-generated Swagger UI |
| **ORM** | JPA / Hibernate | Entity mapping, repository pattern |
| **Database** | PostgreSQL (Supabase) | Cloud-hosted, pooled connections |
| **Desktop UI** | JavaFX 21 | All desktop screens, FXML layouts |
| **Desktop DB** | SQLite | Local offline storage |
| **PDF Reports** | Python 3 + fpdf2 | Per-equipment maintenance reports |
| **Excel Export** | Python 3 + openpyxl | 4-sheet parts usage workbook |
| **Build** | Apache Maven | Dependency management, fat JAR |
| **Packaging** | jlink + jpackage + WiX 3.11 | Standalone Windows `.exe` installer |
| **CI/CD** | GitHub Actions | Build + test on every push |
| **Deployment** | Render (API) + Vercel (web) | Cloud hosting |

---

## ✨ Features

- **Asset Registry** — Searchable record of every machine with location, status, and maintenance schedule
- **Scheduled Maintenance** — Calendar-driven PMs with automatic `next_maintenance_date` recalculation
- **Breakdown Logging** — Incident capture with description, resolver, and unresolved status tracking
- **Parts & Stock Control** — Live inventory with issue/return transactions, reorder thresholds, and low-stock badges
- **Work Order System** — Full lifecycle: Open → In Progress → Completed, linked to equipment
- **Role-Based Access** — ADMIN / MANAGER / TECHNICIAN roles enforced at API and frontend level
- **Smart Alert Engine** — Background polling detects low-stock parts and overdue maintenance
- **Operations Dashboard** — Live KPI tiles: total equipment, overdue count, low stock count, open work orders
- **Charts** — Maintenance activity (last 6 months), breakdown frequency, equipment status doughnut
- **Recent Activity Feed** — Merged chronological view of maintenance and breakdown events
- **PDF Maintenance Report** — Formatted report per equipment (desktop)
- **Excel Parts Export** — 4-sheet workbook: Parts Summary, Issue History, Cost Per Asset, Low Stock Alerts (desktop)
- **Standalone Installer** — `.exe` installer bundles Java JRE, JavaFX, and Python scripts — zero prerequisites on target machine
- **Swagger UI** — Full interactive API documentation at `/swagger-ui.html`
- **Caffeine Cache** — 269× dashboard speedup (2300ms → 8ms) with automatic eviction on writes
- **Gzip Compression** — All JSON responses over 1KB compressed automatically

---

## 🖥 Screens

### Web

| Screen | Description |
|--------|-------------|
| **Dashboard** | KPI tiles, maintenance activity chart, breakdown frequency chart, equipment status doughnut, recent activity feed |
| **Equipment** | Full CRUD — add/edit/delete machines, colour-coded status |
| **Parts** | Full CRUD + Issue modal — stock levels, supplier link, Low Stock / Out of Stock badges |
| **Suppliers** | Full CRUD — contact details, linked to parts |
| **Maintenance** | Log PM jobs; equipment next-due date recalculates automatically |
| **Breakdowns** | Log incidents; resolve with one click |
| **Work Orders** | Full lifecycle management with status tracking |
| **Users** | ADMIN-only — view users, update roles, delete accounts |

### Desktop

| Screen | Description |
|--------|-------------|
| **Dashboard** | KPI tiles, alert feed, cost-per-asset bars, recent activity table |
| **Equipment** | Full CRUD — add/edit/delete machines |
| **Parts & Inventory** | Full CRUD — stock levels, supplier link, low-stock badge |
| **Suppliers** | Full CRUD — contact details |
| **KPIs & Analytics** | Uptime %, MTBF, Cost Per Asset — 3-tab screen with visual bars |
| **Activity Feed** | Merged feed with Type / Date / Equipment filters and keyword search |
| **Reports & Exports** | Generate PDF per equipment; export all parts data to Excel |
| **Maintenance Log** | Log PM jobs with automatic next-due recalculation |
| **Breakdowns** | Log incidents; unresolved shown in red |
| **Issues & Alerts** | Issue/return parts with optional work order link |

---

## 🔐 Roles & Permissions

| Action | ADMIN | MANAGER | TECHNICIAN |
|---|---|---|---|
| View all data | ✅ | ✅ | ✅ |
| Create records | ✅ | ✅ | ✅ |
| Edit records | ✅ | ✅ | ✅ |
| Delete records | ✅ | ✅ | ❌ |
| Manage users | ✅ | ❌ | ❌ |

Enforced at the API layer via `@PreAuthorize` and at the frontend via `canDelete()` in `AuthContext`.

---

## 🗄 Database Schema

Eight tables with `EQUIPMENT` as the central entity. `ISSUE_RECORD` links to both `BREAKDOWN_LOG` and `MAINTENANCE_LOG` for work order traceability. `APP_USER` is used for web auth.

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

APP_USER ── authenticates ──► all web endpoints (JWT)
WORK_ORDER ── linked to ──► EQUIPMENT
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
| `WORK_ORDER` | `id` | `equipment_id` | `title`, `status`, `priority`, `assigned_to` |
| `APP_USER` | `id` | — | `username`, `email`, `password_hash`, `role` |

\* Optional FK — null for standalone stock draws, set for work order issues.

### SQL Schema (core tables)

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

---

## 📁 Project Structure

```
maintaintrack-pro/
├── api/                              # Spring Boot REST API
│   ├── src/main/java/com/maintaintrack/api/
│   │   ├── config/                   # Security, CORS, JWT, cache, rate limit, Swagger
│   │   ├── controllers/              # REST controllers (one per domain)
│   │   ├── services/                 # Business logic
│   │   ├── models/                   # JPA entities
│   │   ├── repositories/             # Spring Data JPA repositories
│   │   └── dto/                      # Request/response DTOs
│   └── src/main/resources/
│       └── application.properties
├── web/                              # React frontend
│   ├── src/
│   │   ├── pages/                    # One component per page
│   │   ├── context/                  # AuthContext (JWT + role helpers)
│   │   └── api/                      # Axios instance + interceptors
│   └── .env                          # VITE_API_URL
├── java/                             # JavaFX desktop application
│   └── src/main/java/com/maintaintrack/
│       ├── controllers/              # FXML controllers
│       ├── services/                 # Business logic + API client
│       ├── dao/                      # SQLite data access
│       └── auth/                     # ApiAuthService, AuthContext, JWT
├── scripts/                          # Python report/export scripts
│   ├── generate_report.py            # PDF per-equipment report
│   ├── export_parts.py               # Excel parts workbook
│   └── seed_db.py                    # Sample data seeder
├── data/
│   └── maintaintrack.db              # SQLite database (local dev)
├── .github/workflows/                # GitHub Actions CI
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- Maven 3.9+
- Python 3.10+ (desktop scripts only)

### 1. Clone the repo

```bash
git clone https://github.com/HarshitVerma04/maintaintrack-pro.git
cd maintaintrack-pro
```

### 2. Run the API

```powershell
cd api
# Set environment variables (or use run.ps1):
$env:DB_URL="your-supabase-jdbc-url"
$env:DB_USER="your-db-user"
$env:DB_PASS="your-db-password"
$env:JWT_SECRET="your-256-bit-secret"
$env:JWT_EXPIRY_MS="28800000"
mvn spring-boot:run
```

API runs at `http://localhost:8080`  
Swagger UI at `http://localhost:8080/swagger-ui.html`

### 3. Run the web frontend

```bash
cd web
npm install
# Create .env:
# VITE_API_URL=http://localhost:8080
npm run dev
```

Frontend runs at `http://localhost:5173`

### 4. Run the desktop app

```bash
cd java
mvn clean javafx:run
```

> **IntelliJ:** Working Directory must be set to the project root (`maintaintrack-pro/`), not `java/`.  
> VM Options: `--module-path "C:\javafx-sdk-17.0.19\lib" --add-modules javafx.controls,javafx.fxml`

### 5. Seed the database with sample data (desktop / SQLite only)

```bash
python scripts/seed_db.py
# Reset with fresh data:
python scripts/seed_db.py --reset
```

---

## 🔑 Environment Variables

### API (Render environment variables)

| Variable | Description |
|---|---|
| `DB_URL` | Supabase JDBC connection string |
| `DB_USER` | Database username |
| `DB_PASS` | Database password |
| `JWT_SECRET` | 256-bit secret for signing JWTs |
| `JWT_EXPIRY_MS` | Token validity in ms (default: 28800000 = 8h) |

### Web frontend (Vercel environment variables)

| Variable | Description |
|---|---|
| `VITE_API_URL` | Full URL of the deployed API |

---

## 📖 API Documentation

Full interactive docs available at:  
**[https://maintaintrack-pro.onrender.com/swagger-ui.html](https://maintaintrack-pro.onrender.com/swagger-ui.html)**

Key endpoints:

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | None | Create a new user |
| POST | `/auth/login` | None | Get JWT token |
| GET | `/api/equipment` | JWT | List all equipment |
| POST | `/api/maintenance/log` | JWT | Log a maintenance event |
| GET | `/api/dashboard/kpis` | JWT | Dashboard stats (cached 60s) |
| GET | `/api/parts` | JWT | Parts inventory |
| GET | `/api/parts/low-stock` | JWT | Parts below minimum quantity |
| GET | `/api/work-orders` | JWT | All work orders |
| GET | `/actuator/health` | None | Health check |

---

## 📦 Deployment

| Service | Platform | URL |
|---|---|---|
| REST API | Render (free tier) | https://maintaintrack-pro.onrender.com |
| Web frontend | Vercel | https://maintaintrack-pro.vercel.app |
| Database | Supabase | PostgreSQL (pooled connection) |

> **Note:** Render free tier spins down after 15 minutes of inactivity. The first request after idle takes ~30 seconds to cold-start. Subsequent requests are instant.

---

## 🔒 Security

- Passwords hashed with BCrypt (strength 10)
- JWT tokens signed with HMAC-SHA384, expire after 8 hours
- Rate limiting on `/auth/login` — 10 attempts per minute per IP (Bucket4j)
- CORS locked to Vercel origin only in production
- Security headers on all responses (`X-Frame-Options: DENY`, `HSTS`, `X-Content-Type-Options`)
- Actuator locked down — only `/health` exposed publicly
- Role-based access enforced at both API (`@PreAuthorize`) and frontend (`canDelete()`) level
- Stack traces never returned to clients — full errors logged server-side only

---

## ⚡ Performance

- Caffeine in-memory cache on dashboard KPIs (60s TTL) and equipment/parts lists (5min TTL)
- Cache evicted automatically on any write operation
- Dashboard response time: ~2300ms uncached → ~8ms cached (**269× speedup**)
- Gzip compression enabled on all JSON responses over 1KB

---

## 🖥 Desktop Installer Build

The desktop app ships as a fully standalone Windows `.exe` installer — no Java, Python, or JavaFX required on the target machine.

### Production Database Location

```
C:\Users\<name>\AppData\Roaming\MaintainTrackPro\maintaintrack.db
```

`DBConnection.java` detects dev vs prod automatically. Database survives uninstalls and app updates.

### Build Steps

**Step 1 — Bundle Python scripts**

```bash
scripts\venv\Scripts\activate
pyinstaller --onefile scripts/generate_report.py --distpath bundled/
pyinstaller --onefile scripts/export_parts.py --distpath bundled/
```

**Step 2 — Build the fat JAR**

```bash
cd java && mvn clean package && cd ..
```

**Step 3 — Copy bundled exes into target**

```powershell
mkdir java\target\bundled
copy bundled\generate_report.exe java\target\bundled\generate_report.exe
copy bundled\export_parts.exe java\target\bundled\export_parts.exe
```

**Step 4 — Build custom JRE with JavaFX**

```powershell
& "C:\Program Files\Java\jdk-23\bin\jlink.exe" `
  --no-header-files --no-man-pages --compress=2 `
  --module-path "C:\Program Files\Java\jdk-23\jmods;C:\javafx-jmods-17.0.19" `
  --add-modules java.base,java.sql,java.desktop,java.logging,javafx.controls,javafx.fxml,javafx.graphics,javafx.base `
  --output custom-jre
```

**Step 5 — Run jpackage** (requires [WiX Toolset 3.11](https://github.com/wixtoolset/wix3/releases/tag/wix3112rtm))

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
  --app-version 2.0.0 `
  --vendor "MaintainTrack"
```

Pre-built installer available on the [Releases page](https://github.com/HarshitVerma04/MaintainTrack-Pro/releases).

---

## 🔄 Application Flow

```
Login (JWT auth)
├── Web App
│   ├── Dashboard — KPIs + charts + activity feed
│   ├── Equipment — CRUD, status tracking
│   ├── Maintenance — log PM jobs, auto next-date recalc
│   ├── Breakdowns — log and resolve incidents
│   ├── Parts — CRUD + issue modal (decrements stock)
│   ├── Work Orders — Open → In Progress → Completed
│   ├── Suppliers — directory linked to parts
│   └── Users (ADMIN only) — manage roles
│
└── Desktop App
    ├── Dashboard — KPI tiles, alert feed, recent activity
    ├── Equipment — CRUD + maintenance schedule
    ├── Parts — stock control with issue/return
    ├── Breakdowns — incident logging
    ├── KPIs — Uptime %, MTBF, Cost Per Asset
    ├── Activity Feed — merged chronological view
    └── Reports — PDF per equipment, Excel parts export
```

---

## 👥 Team

| Phase | Days | Developer     | Scope |
|-------|------|---------|-------|
| **Phase 1** | 1–5 | Harshit | Project scaffold, DB schema, Equipment/Parts/Suppliers CRUD |
| **Phase 2** | 6–9 | Harshit | Maintenance scheduler, Breakdown log, Work order system |
| **Phase 2** | 10–12 | Adarsh  | Issue/Return form, transactional stock update |
| **Phase 3** | 13–19 | Adarsh  | Alert engine, background polling, `AlertPollingService` |
| **Phase 4** | 20–28 | Harshit | Dashboard, KPIs, Activity Feed, PDF/Excel reports, desktop packaging |
| **Phase 5 (V2)** | 29–40 | Harshit | Spring Boot REST API, React web frontend, JWT auth, cloud deployment, security hardening, caching, Swagger docs |

---

## 📄 Licence

MIT Licence — see [LICENCE](LICENCE) for details.

---

<div align="center">
  <strong>MaintainTrack Pro v2.0.0</strong> — Built for the teams that keep everything running.<br/>
  <em>Harshit Verma, KIIT University · B.Tech CSE 2023–27 · </em>
</div>
