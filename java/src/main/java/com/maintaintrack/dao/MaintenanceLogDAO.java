package com.maintaintrack.dao;

import com.maintaintrack.models.MaintenanceLog;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MaintenanceLogDAO — handles all SQL for the MAINTENANCE_LOG table.
 * findAll() joins EQUIPMENT to bring in the equipment name for display.
 */
public class MaintenanceLogDAO {

    // ── INSERT (own connection) ───────────────────────────────────────────

    public void insert(MaintenanceLog log) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            insert(log, conn);
        }
    }

    // ── INSERT (caller-supplied connection — for transaction support) ─────

    public void insert(MaintenanceLog log, Connection conn) throws SQLException {
        String sql = """
                INSERT INTO MAINTENANCE_LOG (equipment_id, done_on, notes, done_by)
                VALUES (?, ?, ?, ?);
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, log.getEquipmentId());
            ps.setString(2, log.getDoneOn().toString());
            ps.setString(3, log.getNotes());
            ps.setString(4, log.getDoneBy());
            ps.executeUpdate();
        }
    }

    // ── FIND ALL (joined with EQUIPMENT name) ─────────────────────────────

    public List<MaintenanceLog> findAll() throws SQLException {
        String sql = """
                SELECT m.*, e.name AS equipment_name
                FROM MAINTENANCE_LOG m
                JOIN EQUIPMENT e ON m.equipment_id = e.id
                ORDER BY m.done_on DESC;
                """;
        List<MaintenanceLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── FIND BY EQUIPMENT ─────────────────────────────────────────────────

    public List<MaintenanceLog> findByEquipment(int equipmentId) throws SQLException {
        String sql = """
                SELECT m.*, e.name AS equipment_name
                FROM MAINTENANCE_LOG m
                JOIN EQUIPMENT e ON m.equipment_id = e.id
                WHERE m.equipment_id = ?
                ORDER BY m.done_on DESC;
                """;
        List<MaintenanceLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM MAINTENANCE_LOG WHERE id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── ROW MAPPER ────────────────────────────────────────────────────────

    private MaintenanceLog mapRow(ResultSet rs) throws SQLException {
        MaintenanceLog log = new MaintenanceLog(
                rs.getInt("id"),
                rs.getInt("equipment_id"),
                LocalDate.parse(rs.getString("done_on")),
                rs.getString("notes"),
                rs.getString("done_by")
        );
        log.setEquipmentName(rs.getString("equipment_name"));
        return log;
    }
}
