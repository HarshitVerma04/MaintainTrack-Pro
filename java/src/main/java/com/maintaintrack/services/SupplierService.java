package com.maintaintrack.services;

import com.maintaintrack.dao.SupplierDAO;
import com.maintaintrack.models.Supplier;
import com.maintaintrack.sync.SyncService;
import com.maintaintrack.sync.SyncTask;

import java.sql.SQLException;
import java.util.List;

public class SupplierService {

    private final SupplierDAO dao = new SupplierDAO();

    public void addSupplier(Supplier s) throws SQLException {
        if (s.getName() == null || s.getName().isBlank())
            throw new IllegalArgumentException("Supplier name is required.");
        dao.insert(s);
        SyncService.getInstance().push(new SyncTask(
                "SUPPLIER", s.getId(),
                toJson(s), SyncTask.Operation.INSERT));
    }

    public void updateSupplier(Supplier s) throws SQLException {
        if (s.getName() == null || s.getName().isBlank())
            throw new IllegalArgumentException("Supplier name is required.");
        dao.update(s);
        SyncService.getInstance().push(new SyncTask(
                "SUPPLIER", s.getId(),
                toJson(s), SyncTask.Operation.UPDATE));
    }

    public void deleteSupplier(int id) throws SQLException {
        dao.delete(id);
        SyncService.getInstance().push(new SyncTask(
                "SUPPLIER", id,
                "{\"id\":" + id + "}", SyncTask.Operation.DELETE));
    }

    public List<Supplier> getAllSuppliers() throws SQLException {
        return dao.findAll();
    }

    public Supplier getById(int id) throws SQLException {
        return dao.findById(id);
    }


    private String toJson(Supplier s) {
        String syncId = getSyncId(s.getId());
        return String.format(
                "{\"syncId\":\"%s\",\"name\":\"%s\",\"contactName\":\"%s\"," +
                        "\"phone\":\"%s\",\"email\":\"%s\",\"updatedAt\":\"%s\"}",
                syncId,
                escape(s.getName()),
                escape(s.getContactName()),
                escape(s.getPhone()),
                escape(s.getEmail()),
                java.time.LocalDateTime.now().toString()
        );
    }

    private String getSyncId(int id) {
        String sql = "SELECT sync_id FROM SUPPLIER WHERE id = ?";
        try (java.sql.Connection conn =
                     com.maintaintrack.dao.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String s = rs.getString("sync_id");
                return s != null ? s : java.util.UUID.randomUUID().toString();
            }
        } catch (Exception e) {
            System.err.println("[Sync] Supplier getSyncId failed: " + e.getMessage());
        }
        return java.util.UUID.randomUUID().toString();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}