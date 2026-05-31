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
        return String.format(
                "{\"equipmentId\":%d,\"doneOn\":\"%s\"," +
                        "\"notes\":\"%s\",\"doneBy\":\"%s\"}",
                log.getEquipmentId(),
                log.getDoneOn() != null ? log.getDoneOn().toString() : "",
                escape(log.getNotes()),
                escape(log.getDoneBy())
        );
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}