package com.maintaintrack.dao;

import com.maintaintrack.models.BreakdownLog;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * BreakdownLogDAO — handles all SQL for the BREAKDOWN_LOG table.
 * findAll() joins EQUIPMENT to bring in the equipment name for display.
 */
public class BreakdownLogDAO {

    // ── INSERT ────────────────────────────────────────────────────────────

    public void insert(BreakdownLog log) throws SQLException {
        String sql = """
                INSERT INTO BREAKDOWN_LOG (equipment_id, occurred_on, description, resolved_by)
                VALUES (?, ?, ?, ?);
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, log.getEquipmentId());
            ps.setString(2, log.getOccurredOn().toString());
            ps.setString(3, log.getDescription());
            ps.setString(4, log.getResolvedBy());
            ps.executeUpdate();
        }
    }

    // ── FIND ALL (joined with EQUIPMENT name) ─────────────────────────────

    public List<BreakdownLog> findAll() throws SQLException {
        String sql = """
                SELECT b.*, e.name AS equipment_name
                FROM BREAKDOWN_LOG b
                JOIN EQUIPMENT e ON b.equipment_id = e.id
                ORDER BY b.occurred_on DESC;
                """;
        List<BreakdownLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── FIND BY EQUIPMENT ─────────────────────────────────────────────────

    public List<BreakdownLog> findByEquipment(int equipmentId) throws SQLException {
        String sql = """
                SELECT b.*, e.name AS equipment_name
                FROM BREAKDOWN_LOG b
                JOIN EQUIPMENT e ON b.equipment_id = e.id
                WHERE b.equipment_id = ?
                ORDER BY b.occurred_on DESC;
                """;
        List<BreakdownLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── COUNT BY EQUIPMENT (used for MTBF in Phase 4) ─────────────────────

    public int countByEquipment(int equipmentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM BREAKDOWN_LOG WHERE equipment_id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM BREAKDOWN_LOG WHERE id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── ROW MAPPER ────────────────────────────────────────────────────────

    private BreakdownLog mapRow(ResultSet rs) throws SQLException {
        BreakdownLog log = new BreakdownLog(
                rs.getInt("id"),
                rs.getInt("equipment_id"),
                LocalDate.parse(rs.getString("occurred_on")),
                rs.getString("description"),
                rs.getString("resolved_by")
        );
        log.setEquipmentName(rs.getString("equipment_name"));
        return log;
    }
}
