package com.maintaintrack.dao;

import com.maintaintrack.models.Equipment;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * EquipmentDAO — handles all SQL for the EQUIPMENT table.
 * No business logic here — pure database reads and writes.
 */
public class EquipmentDAO {

    // ── INSERT ────────────────────────────────────────────────────────────

    public void insert(Equipment e) throws SQLException {
        String sql = """
                INSERT INTO EQUIPMENT (name, location, status, next_maintenance_date, interval_days)
                VALUES (?, ?, ?, ?, ?);
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, e.getName());
            ps.setString(2, e.getLocation());
            ps.setString(3, e.getStatus());
            ps.setString(4, e.getNextMaintenanceDate() != null
                    ? e.getNextMaintenanceDate().toString() : null);
            ps.setInt(5, e.getIntervalDays());
            ps.executeUpdate();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    public void update(Equipment e) throws SQLException {
        String sql = """
                UPDATE EQUIPMENT
                SET name = ?, location = ?, status = ?,
                    next_maintenance_date = ?, interval_days = ?
                WHERE id = ?;
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, e.getName());
            ps.setString(2, e.getLocation());
            ps.setString(3, e.getStatus());
            ps.setString(4, e.getNextMaintenanceDate() != null
                    ? e.getNextMaintenanceDate().toString() : null);
            ps.setInt(5, e.getIntervalDays());
            ps.setInt(6, e.getId());
            ps.executeUpdate();
        }
    }

    // ── UPDATE next_maintenance_date only (used by Phase 2 scheduler) ─────

    public void updateNextMaintenanceDate(int equipmentId, LocalDate date) throws SQLException {
        String sql = "UPDATE EQUIPMENT SET next_maintenance_date = ? WHERE id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setInt(2, equipmentId);
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM EQUIPMENT WHERE id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────

    public List<Equipment> findAll() throws SQLException {
        String sql = "SELECT * FROM EQUIPMENT ORDER BY name;";
        List<Equipment> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────

    public Equipment findById(int id) throws SQLException {
        String sql = "SELECT * FROM EQUIPMENT WHERE id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ── SEARCH by name or location ────────────────────────────────────────

    public List<Equipment> search(String keyword) throws SQLException {
        String sql = """
                SELECT * FROM EQUIPMENT
                WHERE name LIKE ? OR location LIKE ?
                ORDER BY name;
                """;
        List<Equipment> list = new ArrayList<>();
        String pattern = "%" + keyword + "%";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── ROW MAPPER ────────────────────────────────────────────────────────

    private Equipment mapRow(ResultSet rs) throws SQLException {
        String dateStr = rs.getString("next_maintenance_date");
        LocalDate date = (dateStr != null && !dateStr.isBlank())
                ? LocalDate.parse(dateStr) : null;

        return new Equipment(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("location"),
                rs.getString("status"),
                date,
                rs.getInt("interval_days")
        );
    }
}
