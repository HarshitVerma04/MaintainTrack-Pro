package com.maintaintrack.services;

import com.maintaintrack.dao.DBConnection;
import com.maintaintrack.models.Equipment;
import com.maintaintrack.models.Part;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardService — read-only KPI queries for the dashboard.
 *
 * All methods return plain values or simple lists — no business logic,
 * just SQL aggregations that the DashboardController displays.
 *
 * KPIs covered:
 *  - Total equipment count + breakdown by status
 *  - Overdue maintenance list
 *  - Low stock parts list
 *  - Total parts cost (spend) from ISSUE_RECORD
 *  - Cost per asset (top 5)
 *  - Recent activity feed (merged from 3 tables)
 *  - Breakdown count (for MTBF — Phase 4 Day 22)
 */
public class DashboardService {

    // ── Equipment KPIs ────────────────────────────────────────────────────

    public int getTotalEquipmentCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM EQUIPMENT;";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int getOperationalCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM EQUIPMENT WHERE status = 'Operational';";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Equipment where next_maintenance_date < today.
     * Used for the overdue alert feed.
     */
    public List<Equipment> getOverdueEquipment() throws SQLException {
        String sql = """
                SELECT * FROM EQUIPMENT
                WHERE next_maintenance_date < DATE('now')
                ORDER BY next_maintenance_date ASC;
                """;
        List<Equipment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String dateStr = rs.getString("next_maintenance_date");
                Equipment e = new Equipment(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getString("status"),
                        dateStr != null ? LocalDate.parse(dateStr) : null,
                        rs.getInt("interval_days")
                );
                list.add(e);
            }
        }
        return list;
    }

    // ── Parts KPIs ────────────────────────────────────────────────────────

    /**
     * Parts where qty_on_hand <= min_qty.
     * Used for the low-stock alert feed.
     */
    public List<Part> getLowStockParts() throws SQLException {
        String sql = """
                SELECT p.*, s.name AS supplier_name
                FROM PART p
                LEFT JOIN SUPPLIER s ON p.supplier_id = s.id
                WHERE p.qty_on_hand <= p.min_qty
                ORDER BY p.qty_on_hand ASC;
                """;
        List<Part> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Part p = new Part(
                        rs.getInt("id"),
                        rs.getInt("supplier_id"),
                        rs.getString("name"),
                        rs.getInt("qty_on_hand"),
                        rs.getInt("min_qty"),
                        rs.getString("unit"),
                        rs.getDouble("unit_cost")
                );
                p.setSupplierName(rs.getString("supplier_name"));
                list.add(p);
            }
        }
        return list;
    }

    /**
     * Total parts spend — sum of (qty × unit_cost) for all 'issue' records.
     */
    public double getTotalPartsSpend() throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(ir.qty * p.unit_cost), 0)
                FROM ISSUE_RECORD ir
                JOIN PART p ON ir.part_id = p.id
                WHERE ir.type = 'issue';
                """;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    /**
     * Total maintenance jobs logged.
     */
    public int getTotalMaintenanceJobs() throws SQLException {
        String sql = "SELECT COUNT(*) FROM MAINTENANCE_LOG;";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Total breakdown incidents logged.
     */
    public int getTotalBreakdowns() throws SQLException {
        String sql = "SELECT COUNT(*) FROM BREAKDOWN_LOG;";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Activity Feed ─────────────────────────────────────────────────────

    /**
     * Merged chronological feed from MAINTENANCE_LOG, BREAKDOWN_LOG,
     * and ISSUE_RECORD — last 15 events across all tables.
     *
     * Returns list of String[4]: {type, date, description, equipment/part}
     */
    public List<String[]> getRecentActivity(int limit) throws SQLException {
        String sql = """
                SELECT 'Maintenance' AS type,
                       m.done_on     AS event_date,
                       m.notes       AS detail,
                       e.name        AS subject,
                       m.done_by     AS actor
                FROM MAINTENANCE_LOG m
                JOIN EQUIPMENT e ON m.equipment_id = e.id

                UNION ALL

                SELECT 'Breakdown',
                       b.occurred_on,
                       b.description,
                       e.name,
                       COALESCE(b.resolved_by, 'Unresolved')
                FROM BREAKDOWN_LOG b
                JOIN EQUIPMENT e ON b.equipment_id = e.id

                UNION ALL

                SELECT CASE ir.type WHEN 'issue' THEN 'Part Issued' ELSE 'Part Returned' END,
                       ir.issued_on,
                       p.name || ' × ' || ir.qty,
                       e.name,
                       ir.issued_by
                FROM ISSUE_RECORD ir
                JOIN PART p      ON ir.part_id      = p.id
                JOIN EQUIPMENT e ON ir.equipment_id = e.id

                ORDER BY event_date DESC
                LIMIT ?;
                """;
        List<String[]> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("type"),
                        rs.getString("event_date"),
                        rs.getString("detail"),
                        rs.getString("subject"),
                        rs.getString("actor")
                });
            }
        }
        return list;
    }

    // ── Cost per asset (top 5) ────────────────────────────────────────────

    public Map<String, Double> getCostPerAsset() throws SQLException {
        String sql = """
                SELECT e.name,
                       ROUND(SUM(ir.qty * p.unit_cost), 2) AS total_cost
                FROM ISSUE_RECORD ir
                JOIN EQUIPMENT e ON ir.equipment_id = e.id
                JOIN PART p      ON ir.part_id      = p.id
                WHERE ir.type = 'issue'
                GROUP BY e.id
                ORDER BY total_cost DESC
                LIMIT 5;
                """;
        Map<String, Double> map = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("name"), rs.getDouble("total_cost"));
            }
        }
        return map;
    }
}
