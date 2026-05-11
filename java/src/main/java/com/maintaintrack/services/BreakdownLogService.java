package com.maintaintrack.services;

import com.maintaintrack.dao.BreakdownLogDAO;
import com.maintaintrack.models.BreakdownLog;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * BreakdownLogService — business layer for breakdown logging.
 *
 * Simpler than MaintenanceLogService — no date recalculation needed.
 * Breakdowns are recorded as-is; the equipment status update is
 * handled separately via the Equipment screen.
 */
public class BreakdownLogService {

    private final BreakdownLogDAO dao = new BreakdownLogDAO();

    public void logBreakdown(BreakdownLog log) throws SQLException {
        // ── Validate ──────────────────────────────────────────────────────
        if (log.getEquipmentId() <= 0)
            throw new IllegalArgumentException("Please select an equipment.");
        if (log.getOccurredOn() == null)
            throw new IllegalArgumentException("Occurred on date is required.");
        if (log.getOccurredOn().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Occurred date cannot be in the future.");
        if (log.getDescription() == null || log.getDescription().isBlank())
            throw new IllegalArgumentException("Description is required.");

        dao.insert(log);
    }

    public List<BreakdownLog> getAllLogs() throws SQLException {
        return dao.findAll();
    }

    public List<BreakdownLog> getLogsForEquipment(int equipmentId) throws SQLException {
        return dao.findByEquipment(equipmentId);
    }

    public int getBreakdownCount(int equipmentId) throws SQLException {
        return dao.countByEquipment(equipmentId);
    }

    public void deleteLog(int id) throws SQLException {
        dao.delete(id);
    }
}
