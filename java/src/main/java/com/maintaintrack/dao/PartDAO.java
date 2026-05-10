package com.maintaintrack.dao;

import com.maintaintrack.models.Part;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PartDAO — handles all SQL for the PART table.
 * findAll() uses a LEFT JOIN to bring in supplier name for display.
 */
public class PartDAO {

    // ── INSERT ────────────────────────────────────────────────────────────

    public void insert(Part p) throws SQLException {
        String sql = """
                INSERT INTO PART (supplier_id, name, qty_on_hand, min_qty, unit, unit_cost)
                VALUES (?, ?, ?, ?, ?, ?);
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getSupplierId());
            ps.setString(2, p.getName());
            ps.setInt(3, p.getQtyOnHand());
            ps.setInt(4, p.getMinQty());
            ps.setString(5, p.getUnit());
            ps.setDouble(6, p.getUnitCost());
            ps.executeUpdate();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    public void update(Part p) throws SQLException {
        String sql = """
                UPDATE PART
                SET supplier_id = ?, name = ?, qty_on_hand = ?,
                    min_qty = ?, unit = ?, unit_cost = ?
                WHERE id = ?;
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getSupplierId());
            ps.setString(2, p.getName());
            ps.setInt(3, p.getQtyOnHand());
            ps.setInt(4, p.getMinQty());
            ps.setString(5, p.getUnit());
            ps.setDouble(6, p.getUnitCost());
            ps.setInt(7, p.getId());
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM PART WHERE id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── FIND ALL (with supplier name joined) ──────────────────────────────

    public List<Part> findAll() throws SQLException {
        String sql = """
                SELECT p.*, s.name AS supplier_name
                FROM PART p
                LEFT JOIN SUPPLIER s ON p.supplier_id = s.id
                ORDER BY p.name;
                """;
        List<Part> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────

    public Part findById(int id) throws SQLException {
        String sql = """
                SELECT p.*, s.name AS supplier_name
                FROM PART p
                LEFT JOIN SUPPLIER s ON p.supplier_id = s.id
                WHERE p.id = ?;
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // ── FIND LOW STOCK (qty_on_hand <= min_qty) ───────────────────────────

    public List<Part> findLowStock() throws SQLException {
        String sql = """
                SELECT p.*, s.name AS supplier_name
                FROM PART p
                LEFT JOIN SUPPLIER s ON p.supplier_id = s.id
                WHERE p.qty_on_hand <= p.min_qty
                ORDER BY p.name;
                """;
        List<Part> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── SEARCH ────────────────────────────────────────────────────────────

    public List<Part> search(String keyword) throws SQLException {
        String sql = """
                SELECT p.*, s.name AS supplier_name
                FROM PART p
                LEFT JOIN SUPPLIER s ON p.supplier_id = s.id
                WHERE p.name LIKE ? OR s.name LIKE ?
                ORDER BY p.name;
                """;
        String pattern = "%" + keyword + "%";
        List<Part> list = new ArrayList<>();
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

    private Part mapRow(ResultSet rs) throws SQLException {
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
        return p;
    }
}
