package com.maintaintrack.services;

import com.maintaintrack.dao.EquipmentDAO;
import com.maintaintrack.dao.MaintenanceLogDAO;
import com.maintaintrack.models.Equipment;
import com.maintaintrack.models.MaintenanceLog;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * MaintenanceLogService — the most important service in Phase 2.
 *
 * Core logic: when a maintenance job is logged, the equipment's
 * next_maintenance_date must be recalculated automatically:
 *
 *   next_maintenance_date = done_on + interval_days
 *
 * Both writes (insert log + update equipment) happen together.
 * If either fails, neither is saved.
 */
public class MaintenanceLogService {

    private final MaintenanceLogDAO logDAO       = new MaintenanceLogDAO();
    private final EquipmentDAO      equipmentDAO = new EquipmentDAO();

    /**
     * Logs a maintenance job AND recalculates the equipment's next due date.
     * This is the key business rule for Phase 2.
     */
    public void logMaintenance(MaintenanceLog log) throws SQLException {
        // ── Validate ──────────────────────────────────────────────────────
        if (log.getEquipmentId() <= 0)
            throw new IllegalArgumentException("Please select an equipment.");
        if (log.getDoneOn() == null)
            throw new IllegalArgumentException("Date is required.");
        if (log.getDoneOn().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Done date cannot be in the future.");
        if (log.getDoneBy() == null || log.getDoneBy().isBlank())
            throw new IllegalArgumentException("'Done by' is required.");

        // ── Insert the maintenance log ────────────────────────────────────
        logDAO.insert(log);

        // ── Recalculate next_maintenance_date ─────────────────────────────
        Equipment equipment = equipmentDAO.findById(log.getEquipmentId());
        if (equipment != null) {
            LocalDate nextDue = log.getDoneOn().plusDays(equipment.getIntervalDays());
            equipmentDAO.updateNextMaintenanceDate(log.getEquipmentId(), nextDue);
            System.out.println("[Maintenance] Next due for '"
                    + equipment.getName() + "' → " + nextDue);
        }
    }

    public List<MaintenanceLog> getAllLogs() throws SQLException {
        return logDAO.findAll();
    }

    public List<MaintenanceLog> getLogsForEquipment(int equipmentId) throws SQLException {
        return logDAO.findByEquipment(equipmentId);
    }

    public void deleteLog(int id) throws SQLException {
        logDAO.delete(id);
    }
}
