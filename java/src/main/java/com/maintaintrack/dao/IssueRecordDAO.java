package com.maintaintrack.dao;

import com.maintaintrack.models.IssueRecord;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * IssueRecordDAO — handles all SQL for the ISSUE_RECORD table.
 *
 * KEY DESIGN: insert() accepts an external Connection so the caller
 * (IssueRecordService) can wrap both the INSERT and the PART qty update
 * in a single transaction. Never opens its own connection for insert().
 *
 * findAll() joins PART and EQUIPMENT for display names.
 */
public class IssueRecordDAO {

    // ── INSERT (uses caller-supplied connection for transaction support) ───

    public void insert(IssueRecord record, Connection conn) throws SQLException {
        String sql = """
                INSERT INTO ISSUE_RECORD (part_id, equipment_id, issued_on, qty, issued_by, type)
                VALUES (?, ?, ?, ?, ?, ?);
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, record.getPartId());
            ps.setInt(2, record.getEquipmentId());
            ps.setString(3, record.getIssuedOn().toString());
            ps.setInt(4, record.getQty());
            ps.setString(5, record.getIssuedBy());
            ps.setString(6, record.getType());
            ps.executeUpdate();
        }
    }

    // ── UPDATE PART QTY (uses caller-supplied connection) ─────────────────

    /**
     * Adjusts PART.qty_on_hand atomically within the same transaction.
     * delta is negative for "issue" (stock out), positive for "return" (stock in).
     */
    public void adjustPartQty(int partId, int delta, Connection conn) throws SQLException {
        String sql = "UPDATE PART SET qty_on_hand = qty_on_hand + ? WHERE id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, partId);
            ps.executeUpdate();
        }
    }

    // ── FIND ALL (joined) ─────────────────────────────────────────────────

    public List<IssueRecord> findAll() throws SQLException {
        String sql = """
                SELECT ir.*, p.name AS part_name, e.name AS equipment_name
                FROM ISSUE_RECORD ir
                JOIN PART      p ON ir.part_id      = p.id
                JOIN EQUIPMENT e ON ir.equipment_id = e.id
                ORDER BY ir.issued_on DESC, ir.id DESC;
                """;
        List<IssueRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── FIND BY EQUIPMENT ─────────────────────────────────────────────────

    public List<IssueRecord> findByEquipment(int equipmentId) throws SQLException {
        String sql = """
                SELECT ir.*, p.name AS part_name, e.name AS equipment_name
                FROM ISSUE_RECORD ir
                JOIN PART      p ON ir.part_id      = p.id
                JOIN EQUIPMENT e ON ir.equipment_id = e.id
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

    // ── FIND BY PART ──────────────────────────────────────────────────────

    public List<IssueRecord> findByPart(int partId) throws SQLException {
        String sql = """
                SELECT ir.*, p.name AS part_name, e.name AS equipment_name
                FROM ISSUE_RECORD ir
                JOIN PART      p ON ir.part_id      = p.id
                JOIN EQUIPMENT e ON ir.equipment_id = e.id
                WHERE ir.part_id = ?
                ORDER BY ir.issued_on DESC;
                """;
        List<IssueRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, partId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
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
        IssueRecord r = new IssueRecord(
                rs.getInt("id"),
                rs.getInt("part_id"),
                rs.getInt("equipment_id"),
                LocalDate.parse(rs.getString("issued_on")),
                rs.getInt("qty"),
                rs.getString("issued_by"),
                rs.getString("type")
        );
        r.setPartName(rs.getString("part_name"));
        r.setEquipmentName(rs.getString("equipment_name"));
        return r;
    }
}
