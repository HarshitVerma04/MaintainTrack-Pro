package com.maintaintrack.services;

import com.maintaintrack.dao.DBConnection;
import com.maintaintrack.dao.EquipmentDAO;
import com.maintaintrack.dao.MaintenanceLogDAO;
import com.maintaintrack.models.Equipment;
import com.maintaintrack.models.MaintenanceLog;
import com.maintaintrack.sync.SyncService;
import com.maintaintrack.sync.SyncTask;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MaintenanceLogService {

    private final MaintenanceLogDAO logDAO       = new MaintenanceLogDAO();
    private final EquipmentDAO      equipmentDAO = new EquipmentDAO();

    public void logMaintenance(MaintenanceLog log) throws SQLException {
        if (log.getEquipmentId() <= 0)
            throw new IllegalArgumentException("Please select an equipment.");
        if (log.getDoneOn() == null)
            throw new IllegalArgumentException("Date is required.");
        if (log.getDoneOn().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Done date cannot be in the future.");
        if (log.getDoneBy() == null || log.getDoneBy().isBlank())
            throw new IllegalArgumentException("'Done by' is required.");

        Equipment equipment = equipmentDAO.findById(log.getEquipmentId());
        LocalDate nextDue   = equipment != null
                ? log.getDoneOn().plusDays(equipment.getIntervalDays())
                : null;

        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            conn.createStatement().execute("PRAGMA foreign_keys = ON;");
            logDAO.insert(log, conn);
            if (nextDue != null) {
                equipmentDAO.updateNextMaintenanceDate(
                        log.getEquipmentId(), nextDue, conn);
                System.out.println("[Maintenance] Next due for '"
                        + equipment.getName() + "' → " + nextDue);
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }

        // Push to cloud after successful local save
        SyncService.getInstance().push(new SyncTask(
                "MAINTENANCE_LOG", log.getId(),
                toJson(log), SyncTask.Operation.INSERT));
    }

    public List<MaintenanceLog> getAllLogs() throws SQLException {
        return logDAO.findAll();
    }

    public List<MaintenanceLog> getLogsForEquipment(int equipmentId) throws SQLException {
        return logDAO.findByEquipment(equipmentId);
    }

    public void deleteLog(int id) throws SQLException {
        logDAO.delete(id);
        SyncService.getInstance().push(new SyncTask(
                "MAINTENANCE_LOG", id,
                "{\"id\":" + id + "}", SyncTask.Operation.DELETE));
    }

    private String toJson(MaintenanceLog log) {
        // Look up the equipment's sync_id so the cloud can resolve
        // the relationship regardless of ID differences
        String equipSyncId = getEquipmentSyncId(log.getEquipmentId());
        String logSyncId   = getLogSyncId(log.getId());

        return String.format(
                "{\"syncId\":\"%s\",\"equipmentSyncId\":\"%s\"," +
                        "\"doneOn\":\"%s\",\"notes\":\"%s\",\"doneBy\":\"%s\"," +
                        "\"updatedAt\":\"%s\"}",
                logSyncId,
                equipSyncId,
                log.getDoneOn() != null ? log.getDoneOn().toString() : "",
                escape(log.getNotes()),
                escape(log.getDoneBy()),
                java.time.LocalDateTime.now().toString()
        );
    }

    private String getEquipmentSyncId(int equipmentId) {
        String sql = "SELECT sync_id FROM EQUIPMENT WHERE id = ?";
        try (java.sql.Connection conn = com.maintaintrack.dao.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipmentId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("sync_id");
        } catch (Exception e) {
            System.err.println("[Sync] Could not resolve equipment sync_id: " + e.getMessage());
        }
        return "";
    }

    private String getLogSyncId(int logId) {
        String sql = "SELECT sync_id FROM MAINTENANCE_LOG WHERE id = ?";
        try (java.sql.Connection conn = com.maintaintrack.dao.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, logId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String s = rs.getString("sync_id");
                return s != null ? s : java.util.UUID.randomUUID().toString();
            }
        } catch (Exception e) {
            System.err.println("[Sync] Could not resolve log sync_id: " + e.getMessage());
        }
        return java.util.UUID.randomUUID().toString();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}