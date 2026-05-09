package com.maintaintrack.dao;

import com.maintaintrack.model.Equipment;
import com.maintaintrack.model.Equipment.Status;
import com.maintaintrack.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Day 3 — Equipment CRUD.
 *
 * All methods obtain the shared {@link Connection} from
 * {@link DatabaseManager} — no connection management needed at call sites.
 */
public class EquipmentDAO {

    // ── CREATE ───────────────────────────────────────────────

    /**
     * Inserts a new equipment record and populates {@code eq.id} with the
     * generated key.
     *
     * @return the newly assigned {@code id}
     */
    public int insert(Equipment eq) throws SQLException {
        String sql = """
            INSERT INTO equipment
              (name, model, serial_number, location, purchase_date, status, next_maint_date)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, eq.getName());
            ps.setString(2, eq.getModel());
            ps.setString(3, eq.getSerialNumber());
            ps.setString(4, eq.getLocation());
            ps.setString(5, dateStr(eq.getPurchaseDate()));
            ps.setString(6, eq.getStatus() != null ? eq.getStatus().name() : Status.ACTIVE.name());
            ps.setString(7, dateStr(eq.getNextMaintDate()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    eq.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Insert failed — no generated key returned.");
    }

    // ── READ ─────────────────────────────────────────────────

    /** Returns all equipment rows, ordered by name. */
    public List<Equipment> findAll() throws SQLException {
        String sql = "SELECT * FROM equipment ORDER BY name";
        List<Equipment> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Looks up a single record by PK. */
    public Optional<Equipment> findById(int id) throws SQLException {
        String sql = "SELECT * FROM equipment WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    /** Returns equipment whose next_maint_date is on or before {@code cutoff}. */
    public List<Equipment> findOverdue(LocalDate cutoff) throws SQLException {
        String sql = """
            SELECT * FROM equipment
            WHERE next_maint_date IS NOT NULL
              AND next_maint_date <= ?
              AND status = 'ACTIVE'
            ORDER BY next_maint_date
            """;
        List<Equipment> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, cutoff.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ── UPDATE ───────────────────────────────────────────────

    /** Updates all mutable fields of an existing equipment record. */
    public void update(Equipment eq) throws SQLException {
        String sql = """
            UPDATE equipment
               SET name            = ?,
                   model           = ?,
                   serial_number   = ?,
                   location        = ?,
                   purchase_date   = ?,
                   status          = ?,
                   next_maint_date = ?
             WHERE id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, eq.getName());
            ps.setString(2, eq.getModel());
            ps.setString(3, eq.getSerialNumber());
            ps.setString(4, eq.getLocation());
            ps.setString(5, dateStr(eq.getPurchaseDate()));
            ps.setString(6, eq.getStatus().name());
            ps.setString(7, dateStr(eq.getNextMaintDate()));
            ps.setInt(8, eq.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Convenience method used by Day 7 next-due recalculation.
     * Only touches {@code next_maint_date} — avoids re-sending all fields.
     */
    public void updateNextMaintDate(int equipmentId, LocalDate date) throws SQLException {
        String sql = "UPDATE equipment SET next_maint_date = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, dateStr(date));
            ps.setInt(2, equipmentId);
            ps.executeUpdate();
        }
    }

    // ── DELETE ───────────────────────────────────────────────

    /** Hard-deletes a record (cascades to maintenance_log and breakdown_log). */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM equipment WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── Private helpers ──────────────────────────────────────

    private Connection conn() {
        return DatabaseManager.getConnection();
    }

    private Equipment map(ResultSet rs) throws SQLException {
        return new Equipment(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("model"),
            rs.getString("serial_number"),
            rs.getString("location"),
            parseDate(rs.getString("purchase_date")),
            Status.valueOf(rs.getString("status")),
            parseDate(rs.getString("next_maint_date"))
        );
    }

    private static String    dateStr(LocalDate d) { return d == null ? null : d.toString(); }
    private static LocalDate parseDate(String s)  { return s == null ? null : LocalDate.parse(s); }
}
