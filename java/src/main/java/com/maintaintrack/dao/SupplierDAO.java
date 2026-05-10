package com.maintaintrack.dao;

import com.maintaintrack.models.Supplier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SupplierDAO — handles all SQL for the SUPPLIER table.
 * findAll() used here in Day 4 for the Parts supplier dropdown.
 * Full CRUD (insert/update/delete) added in Day 5.
 */
public class SupplierDAO {

    // ── INSERT ────────────────────────────────────────────────────────────

    public void insert(Supplier s) throws SQLException {
        String sql = """
                INSERT INTO SUPPLIER (name, contact_name, phone, email)
                VALUES (?, ?, ?, ?);
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactName());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.executeUpdate();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    public void update(Supplier s) throws SQLException {
        String sql = """
                UPDATE SUPPLIER
                SET name = ?, contact_name = ?, phone = ?, email = ?
                WHERE id = ?;
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactName());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setInt(5, s.getId());
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM SUPPLIER WHERE id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────

    public List<Supplier> findAll() throws SQLException {
        String sql = "SELECT * FROM SUPPLIER ORDER BY name;";
        List<Supplier> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────

    public Supplier findById(int id) throws SQLException {
        String sql = "SELECT * FROM SUPPLIER WHERE id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ── ROW MAPPER ────────────────────────────────────────────────────────

    private Supplier mapRow(ResultSet rs) throws SQLException {
        return new Supplier(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("contact_name"),
                rs.getString("phone"),
                rs.getString("email")
        );
    }
}
