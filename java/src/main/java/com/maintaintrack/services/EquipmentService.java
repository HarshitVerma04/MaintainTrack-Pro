package com.maintaintrack.services;

import com.maintaintrack.dao.EquipmentDAO;
import com.maintaintrack.models.Equipment;
import com.maintaintrack.sync.SyncService;
import com.maintaintrack.sync.SyncTask;

import java.sql.SQLException;
import java.util.List;

public class EquipmentService {

    private final EquipmentDAO dao = new EquipmentDAO();

    public void addEquipment(Equipment e) throws SQLException {
        if (e.getName() == null || e.getName().isBlank())
            throw new IllegalArgumentException("Equipment name is required.");
        if (e.getIntervalDays() <= 0)
            throw new IllegalArgumentException("Interval days must be greater than 0.");
        dao.insert(e);
        // Assign a sync_id immediately after insert
        if (e.getSyncId() == null) {
            String uuid = java.util.UUID.randomUUID().toString();
            e.setSyncId(uuid);
            dao.updateSyncId(e.getId(), uuid);
        }
        SyncService.getInstance().push(new SyncTask(
                "EQUIPMENT", e.getId(),
                toJson(e), SyncTask.Operation.INSERT));
    }

    public void updateEquipment(Equipment e) throws SQLException {
        if (e.getName() == null || e.getName().isBlank())
            throw new IllegalArgumentException("Equipment name is required.");
        dao.update(e);
        SyncService.getInstance().push(new SyncTask(
                "EQUIPMENT", e.getId(),
                toJson(e), SyncTask.Operation.UPDATE));
    }

    public void deleteEquipment(int id) throws SQLException {
        dao.delete(id);
        SyncService.getInstance().push(new SyncTask(
                "EQUIPMENT", id,
                "{\"id\":" + id + "}", SyncTask.Operation.DELETE));
    }

    public List<Equipment> getAllEquipment() throws SQLException {
        return dao.findAll();
    }

    public Equipment getById(int id) throws SQLException {
        return dao.findById(id);
    }

    public List<Equipment> search(String keyword) throws SQLException {
        return dao.search(keyword);
    }

    private String toJson(Equipment e) {
        String syncId = e.getSyncId() != null
                ? e.getSyncId()
                : getSyncId("EQUIPMENT", e.getId());
        return String.format(
                "{\"syncId\":\"%s\",\"name\":\"%s\",\"location\":\"%s\"," +
                        "\"status\":\"%s\",\"nextMaintenanceDate\":\"%s\"," +
                        "\"intervalDays\":%d,\"updatedAt\":\"%s\"}",
                syncId,
                escape(e.getName()),
                escape(e.getLocation()),
                escape(e.getStatus()),
                e.getNextMaintenanceDate() != null
                        ? e.getNextMaintenanceDate().toString() : "",
                e.getIntervalDays(),
                java.time.LocalDateTime.now().toString()
        );
    }

    private String getSyncId(String table, int id) {
        String sql = "SELECT sync_id FROM " + table + " WHERE id = ?";
        try (java.sql.Connection conn = com.maintaintrack.dao.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String s = rs.getString("sync_id");
                return s != null ? s : java.util.UUID.randomUUID().toString();
            }
        } catch (Exception e) {
            System.err.println("[Sync] getSyncId failed: " + e.getMessage());
        }
        return java.util.UUID.randomUUID().toString();
    }


    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}