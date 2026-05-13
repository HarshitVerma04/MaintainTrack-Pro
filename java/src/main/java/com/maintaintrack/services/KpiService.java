package com.maintaintrack.services;

import com.maintaintrack.dao.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * KpiService — deep KPI calculations for the KPIs screen.
 *
 * Days 21-23:
 *   Day 21 — Uptime %      (this file, implemented today)
 *   Day 22 — MTBF           (implemented tomorrow)
 *   Day 23 — Cost per asset (implemented Day 23)
 *
 * ── Uptime % Methodology ─────────────────────────────────────────────────
 * We don't store a resolved_on date in BREAKDOWN_LOG, so we approximate:
 *
 *   period            = ANALYSIS_DAYS (90 days rolling window)
 *   downtime_days     = breakdown_count × AVG_DOWNTIME_PER_BREAKDOWN
 *   uptime_days       = period - downtime_days   (floor 0)
 *   uptime_pct        = (uptime_days / period) × 100
 *
 * Both constants are clearly labelled and easy to adjust.
 */
public class KpiService {

    // ── Configurable constants ────────────────────────────────────────────
    /** Rolling window for all KPI calculations (days). */
    public static final int    ANALYSIS_DAYS              = 90;

    /** Assumed average downtime per breakdown incident (days). */
    public static final double AVG_DOWNTIME_PER_BREAKDOWN = 2.0;

    // ── Uptime record ─────────────────────────────────────────────────────

    /**
     * Holds uptime data for one equipment row.
     */
    public static class UptimeRecord {
        public final int    equipmentId;
        public final String equipmentName;
        public final String location;
        public final String status;
        public final int    breakdownCount;
        public final double downtimeDays;
        public final double uptimePct;

        public UptimeRecord(int equipmentId, String equipmentName,
                            String location, String status,
                            int breakdownCount) {
            this.equipmentId    = equipmentId;
            this.equipmentName  = equipmentName;
            this.location       = location;
            this.status         = status;
            this.breakdownCount = breakdownCount;
            this.downtimeDays   = Math.min(
                    breakdownCount * AVG_DOWNTIME_PER_BREAKDOWN,
                    ANALYSIS_DAYS
            );
            double uptimeDays   = ANALYSIS_DAYS - this.downtimeDays;
            this.uptimePct      = Math.max(0, (uptimeDays / ANALYSIS_DAYS) * 100.0);
        }

        /** Colour hint for the UI: green ≥ 95, amber ≥ 80, red < 80 */
        public String getRating() {
            if (uptimePct >= 95) return "good";
            if (uptimePct >= 80) return "warn";
            return "danger";
        }
    }

    // ── MTBF record ───────────────────────────────────────────────────────

    /**
     * Holds MTBF data for one equipment row.
     * MTBF = analysis_period / breakdown_count  (in days)
     */
    public static class MtbfRecord {
        public final int    equipmentId;
        public final String equipmentName;
        public final String location;
        public final int    breakdownCount;
        public final int    maintenanceCount;
        public final double mtbfDays;

        public MtbfRecord(int equipmentId, String equipmentName,
                          String location, int breakdownCount,
                          int maintenanceCount) {
            this.equipmentId      = equipmentId;
            this.equipmentName    = equipmentName;
            this.location         = location;
            this.breakdownCount   = breakdownCount;
            this.maintenanceCount = maintenanceCount;
            // If no breakdowns, MTBF = full analysis period (best case)
            this.mtbfDays = breakdownCount == 0
                    ? ANALYSIS_DAYS
                    : (double) ANALYSIS_DAYS / breakdownCount;
        }

        public String getRating() {
            if (mtbfDays >= 45) return "good";
            if (mtbfDays >= 20) return "warn";
            return "danger";
        }
    }

    // ── Cost record ───────────────────────────────────────────────────────

    public static class CostRecord {
        public final String equipmentName;
        public final String location;
        public final double totalCost;
        public final int    issueCount;
        public final double costPerDay;

        public CostRecord(String equipmentName, String location,
                          double totalCost, int issueCount) {
            this.equipmentName = equipmentName;
            this.location      = location;
            this.totalCost     = totalCost;
            this.issueCount    = issueCount;
            this.costPerDay    = totalCost / ANALYSIS_DAYS;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // DAY 21 — UPTIME %
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns uptime % for every equipment based on breakdowns
     * in the last ANALYSIS_DAYS window.
     * Ordered worst → best so problem machines appear first.
     */
    public List<UptimeRecord> getUptimePerEquipment() throws SQLException {
        String sql = """
                SELECT e.id,
                       e.name,
                       e.location,
                       e.status,
                       COUNT(b.id) AS breakdown_count
                FROM EQUIPMENT e
                LEFT JOIN BREAKDOWN_LOG b
                       ON b.equipment_id = e.id
                      AND b.occurred_on >= DATE('now', '-' || ? || ' days')
                GROUP BY e.id
                ORDER BY breakdown_count DESC, e.name ASC;
                """;
        List<UptimeRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ANALYSIS_DAYS);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new UptimeRecord(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getString("status"),
                        rs.getInt("breakdown_count")
                ));
            }
        }
        return list;
    }

    /**
     * Fleet-wide uptime average — weighted equally across all machines.
     */
    public double getFleetUptimeAverage() throws SQLException {
        List<UptimeRecord> records = getUptimePerEquipment();
        if (records.isEmpty()) return 100.0;
        return records.stream()
                .mapToDouble(r -> r.uptimePct)
                .average()
                .orElse(100.0);
    }

    // ══════════════════════════════════════════════════════════════════════
    // DAY 22 — MTBF
    // ══════════════════════════════════════════════════════════════════════

    /**
     * MTBF = ANALYSIS_DAYS / breakdown_count per equipment.
     * Also pulls maintenance count for context.
     */
    public List<MtbfRecord> getMtbfPerEquipment() throws SQLException {
        String sql = """
                SELECT e.id,
                       e.name,
                       e.location,
                       COUNT(DISTINCT b.id) AS breakdown_count,
                       COUNT(DISTINCT m.id) AS maintenance_count
                FROM EQUIPMENT e
                LEFT JOIN BREAKDOWN_LOG b
                       ON b.equipment_id = e.id
                      AND b.occurred_on >= DATE('now', '-' || ? || ' days')
                LEFT JOIN MAINTENANCE_LOG m
                       ON m.equipment_id = e.id
                      AND m.done_on >= DATE('now', '-' || ? || ' days')
                GROUP BY e.id
                ORDER BY breakdown_count DESC, e.name ASC;
                """;
        List<MtbfRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ANALYSIS_DAYS);
            ps.setInt(2, ANALYSIS_DAYS);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new MtbfRecord(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getInt("breakdown_count"),
                        rs.getInt("maintenance_count")
                ));
            }
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════════════
    // DAY 23 — COST PER ASSET
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Total parts cost issued per equipment in the analysis window.
     * Ordered highest cost first.
     */
    public List<CostRecord> getCostPerEquipment() throws SQLException {
        String sql = """
                SELECT e.name,
                       e.location,
                       ROUND(SUM(ir.qty * p.unit_cost), 2) AS total_cost,
                       COUNT(ir.id) AS issue_count
                FROM ISSUE_RECORD ir
                JOIN EQUIPMENT e ON ir.equipment_id = e.id
                JOIN PART p      ON ir.part_id      = p.id
                WHERE ir.type    = 'issue'
                  AND ir.issued_on >= DATE('now', '-' || ? || ' days')
                GROUP BY e.id
                ORDER BY total_cost DESC;
                """;
        List<CostRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ANALYSIS_DAYS);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new CostRecord(
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getDouble("total_cost"),
                        rs.getInt("issue_count")
                ));
            }
        }
        return list;
    }

    /**
     * Max cost across all equipment — used for bar scaling in the UI.
     */
    public double getMaxCost() throws SQLException {
        List<CostRecord> records = getCostPerEquipment();
        return records.stream()
                .mapToDouble(r -> r.totalCost)
                .max()
                .orElse(1.0);
    }
}
