package com.maintaintrack.dao;

import com.maintaintrack.models.IssueRecord;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * IssueRecordDAO - all SQL for the ISSUE_RECORD table.
 *
 * Day 8: breakdown_id support + findByBreakdown + getWorkOrderCost
 * Day 9: maintenance_id support + findByMaintenance + getMaintenanceCost
 */
public class IssueRecordDAO {

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

    public void insert(IssueRecord r, Connection conn) throws SQLException {
        String sql = "INSERT INTO ISSUE_RECORD " +
            "(part_id, equipment_id, breakdown_id, maintenance_id, issued_on, qty, issued_by, type) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getPartId());
            ps.setInt(2, r.getEquipmentId());
            if (r.getBreakdownId() != null)  ps.setInt(3, r.getBreakdownId());
            else                              ps.setNull(3, Types.INTEGER);
            if (r.getMaintenanceId() != null) ps.setInt(4, r.getMaintenanceId());
            else                              ps.setNull(4, Types.INTEGER);
            ps.setString(5, r.getIssuedOn().toString());
            ps.setInt(6, r.getQty());
            ps.setString(7, r.getIssuedBy());
            ps.setString(8, r.getType());
            ps.executeUpdate();
        }
    }

    public void adjustPartQty(int partId, int qty,
                               String type, Connection conn) throws SQLException {
        String sql = "issue".equals(type)
            ? "UPDATE PART SET qty_on_hand = qty_on_hand - ? WHERE id = ?;"
            : "UPDATE PART SET qty_on_hand = qty_on_hand + ? WHERE id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty); ps.setInt(2, partId);
            ps.executeUpdate();
        }
    }

    private static final String SELECT_BASE = """
        SELECT ir.*,
               p.name  AS part_name,
               e.name  AS equipment_name,
               b.description  AS breakdown_desc,
               m.notes        AS maintenance_notes
        FROM ISSUE_RECORD ir
        JOIN  PART             p ON ir.part_id        = p.id
        JOIN  EQUIPMENT        e ON ir.equipment_id   = e.id
        LEFT JOIN BREAKDOWN_LOG   b ON ir.breakdown_id   = b.id
        LEFT JOIN MAINTENANCE_LOG m ON ir.maintenance_id = m.id
        """;

    public List<IssueRecord> findAll() throws SQLException {
        List<IssueRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(SELECT_BASE + " ORDER BY ir.issued_on DESC;")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<IssueRecord> findByEquipment(int id) throws SQLException {
        return queryWithId(SELECT_BASE + " WHERE ir.equipment_id = ? ORDER BY ir.issued_on DESC;", id);
    }

    public List<IssueRecord> findByBreakdown(int id) throws SQLException {
        return queryWithId(SELECT_BASE + " WHERE ir.breakdown_id = ? AND ir.type='issue' ORDER BY ir.issued_on ASC;", id);
    }

    public List<IssueRecord> findByMaintenance(int id) throws SQLException {
        return queryWithId(SELECT_BASE + " WHERE ir.maintenance_id = ? AND ir.type='issue' ORDER BY ir.issued_on ASC;", id);
    }

    public double getWorkOrderCost(int breakdownId) throws SQLException {
        return scalarDouble(
            "SELECT ROUND(COALESCE(SUM(ir.qty*p.unit_cost),0),2) " +
            "FROM ISSUE_RECORD ir JOIN PART p ON ir.part_id=p.id " +
            "WHERE ir.breakdown_id=? AND ir.type='issue';", breakdownId);
    }

    public double getMaintenanceCost(int maintenanceId) throws SQLException {
        return scalarDouble(
            "SELECT ROUND(COALESCE(SUM(ir.qty*p.unit_cost),0),2) " +
            "FROM ISSUE_RECORD ir JOIN PART p ON ir.part_id=p.id " +
            "WHERE ir.maintenance_id=? AND ir.type='issue';", maintenanceId);
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM ISSUE_RECORD WHERE id=?;")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    private List<IssueRecord> queryWithId(String sql, int id) throws SQLException {
        List<IssueRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private double scalarDouble(String sql, int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private IssueRecord mapRow(ResultSet rs) throws SQLException {
        IssueRecord r = new IssueRecord();
        r.setId(rs.getInt("id"));
        r.setPartId(rs.getInt("part_id"));
        r.setPartName(rs.getString("part_name"));
        r.setEquipmentId(rs.getInt("equipment_id"));
        r.setEquipmentName(rs.getString("equipment_name"));

        int bdId = rs.getInt("breakdown_id");
        r.setBreakdownId(rs.wasNull() ? null : bdId);
        r.setBreakdownDesc(rs.getString("breakdown_desc"));

        int mId = rs.getInt("maintenance_id");
        r.setMaintenanceId(rs.wasNull() ? null : mId);
        r.setMaintenanceNotes(rs.getString("maintenance_notes"));

        String d = rs.getString("issued_on");
        r.setIssuedOn(d != null ? LocalDate.parse(d) : null);
        r.setQty(rs.getInt("qty"));
        r.setIssuedBy(rs.getString("issued_by"));
        r.setType(rs.getString("type"));
        return r;
    }
}
