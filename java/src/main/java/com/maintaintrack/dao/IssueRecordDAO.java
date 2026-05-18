package com.maintaintrack.dao;

import com.maintaintrack.models.IssueRecord;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * IssueRecordDAO — handles all SQL for the ISSUE_RECORD table.
 *
 * Day 8 additions:
 *   - insert() and insert(conn) now include breakdown_id
 *   - adjustPartQty() accepts caller-supplied connection for atomicity
 *   - findByBreakdown() returns all parts used in a specific breakdown (work order view)
 *   - getWorkOrderCost() returns total cost of parts for one breakdown
 */
public class IssueRecordDAO {

    // ── INSERT (own connection — single standalone transaction) ───────────

    public void insert(IssueRecord r) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                insert(r, conn);
                adjustPartQty(r.getPartId(), r.getQty(), r.getType(), conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ── INSERT (caller-supplied connection — used by service layer) ────────

    public void insert(IssueRecord r, Connection conn) throws SQLException {
        String sql = """
                INSERT INTO ISSUE_RECORD
                    (part_id, equipment_id, breakdown_id, issued_on, qty, issued_by, type)
                VALUES (?, ?, ?, ?, ?, ?, ?);
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getPartId());
            ps.setInt(2, r.getEquipmentId());
            // breakdown_id is nullable — use setNull when not set
            if (r.getBreakdownId() != null) {
                ps.setInt(3, r.getBreakdownId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, r.getIssuedOn().toString());
            ps.setInt(5, r.getQty());
            ps.setString(6, r.getIssuedBy());
            ps.setString(7, r.getType());
            ps.executeUpdate();
        }
    }

    // ── ADJUST PART QTY (must run inside same connection as insert) ───────

    /**
     * Adjusts qty_on_hand for a part.
     * issue  → subtract qty
     * return → add qty
     * Runs within the caller's connection so it's part of the same transaction.
     */
    public void adjustPartQty(int partId, int qty,
                               String type, Connection conn) throws SQLException {
        String sql = "issue".equals(type)
                ? "UPDATE PART SET qty_on_hand = qty_on_hand - ? WHERE id = ?;"
                : "UPDATE PART SET qty_on_hand = qty_on_hand + ? WHERE id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, partId);
            ps.executeUpdate();
        }
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────

    public List<IssueRecord> findAll() throws SQLException {
        String sql = """
                SELECT ir.*,
                       p.name  AS part_name,
                       e.name  AS equipment_name,
                       b.description AS breakdown_desc
                FROM ISSUE_RECORD ir
                JOIN PART p       ON ir.part_id      = p.id
                JOIN EQUIPMENT e  ON ir.equipment_id = e.id
                LEFT JOIN BREAKDOWN_LOG b ON ir.breakdown_id = b.id
                ORDER BY ir.issued_on DESC;
                """;
        return query(sql);
    }

    // ── FIND BY EQUIPMENT ─────────────────────────────────────────────────

    public List<IssueRecord> findByEquipment(int equipmentId) throws SQLException {
        String sql = """
                SELECT ir.*,
                       p.name  AS part_name,
                       e.name  AS equipment_name,
                       b.description AS breakdown_desc
                FROM ISSUE_RECORD ir
                JOIN PART p       ON ir.part_id      = p.id
                JOIN EQUIPMENT e  ON ir.equipment_id = e.id
                LEFT JOIN BREAKDOWN_LOG b ON ir.breakdown_id = b.id
                WHERE ir.equipment_id = ?
                ORDER BY ir.issued_on DESC;
                """;
        List<IssueRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── FIND BY BREAKDOWN (Day 8 — work order view) ───────────────────────

    /**
     * Returns all parts issued for a specific breakdown incident.
     * This is the "work order" view — what was consumed to fix this breakdown.
     */
    public List<IssueRecord> findByBreakdown(int breakdownId) throws SQLException {
        String sql = """
                SELECT ir.*,
                       p.name AS part_name,
                       e.name AS equipment_name,
                       b.description AS breakdown_desc
                FROM ISSUE_RECORD ir
                JOIN PART p       ON ir.part_id      = p.id
                JOIN EQUIPMENT e  ON ir.equipment_id = e.id
                LEFT JOIN BREAKDOWN_LOG b ON ir.breakdown_id = b.id
                WHERE ir.breakdown_id = ?
                  AND ir.type = 'issue'
                ORDER BY ir.issued_on ASC;
                """;
        List<IssueRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, breakdownId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── WORK ORDER COST (Day 8) ───────────────────────────────────────────

    /**
     * Returns the total parts cost for fixing one breakdown incident.
     * Cost = SUM(qty × unit_cost) for all issues linked to this breakdown.
     */
    public double getWorkOrderCost(int breakdownId) throws SQLException {
        String sql = """
                SELECT ROUND(COALESCE(SUM(ir.qty * p.unit_cost), 0), 2) AS total
                FROM ISSUE_RECORD ir
                JOIN PART p ON ir.part_id = p.id
                WHERE ir.breakdown_id = ?
                  AND ir.type = 'issue';
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, breakdownId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble("total") : 0.0;
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM ISSUE_RECORD WHERE id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── ROW MAPPER ────────────────────────────────────────────────────────

    private IssueRecord mapRow(ResultSet rs) throws SQLException {
        IssueRecord r = new IssueRecord();
        r.setId(rs.getInt("id"));
        r.setPartId(rs.getInt("part_id"));
        r.setPartName(rs.getString("part_name"));
        r.setEquipmentId(rs.getInt("equipment_id"));
        r.setEquipmentName(rs.getString("equipment_name"));

        // breakdown_id is nullable
        int bdId = rs.getInt("breakdown_id");
        r.setBreakdownId(rs.wasNull() ? null : bdId);
        r.setBreakdownDesc(rs.getString("breakdown_desc"));

        String dateStr = rs.getString("issued_on");
        r.setIssuedOn(dateStr != null ? LocalDate.parse(dateStr) : null);
        r.setQty(rs.getInt("qty"));
        r.setIssuedBy(rs.getString("issued_by"));
        r.setType(rs.getString("type"));
        return r;
    }

    // ── SHARED QUERY HELPER ───────────────────────────────────────────────

    private List<IssueRecord> query(String sql) throws SQLException {
        List<IssueRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }
}
