package com.maintaintrack.services;

import com.maintaintrack.dao.BreakdownLogDAO;
import com.maintaintrack.models.BreakdownLog;
import com.maintaintrack.sync.SyncService;
import com.maintaintrack.sync.SyncTask;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class BreakdownLogService {

    private final BreakdownLogDAO dao = new BreakdownLogDAO();

    public void logBreakdown(BreakdownLog log) throws SQLException {
        if (log.getEquipmentId() <= 0)
            throw new IllegalArgumentException("Please select an equipment.");
        if (log.getOccurredOn() == null)
            throw new IllegalArgumentException("Occurred on date is required.");
        if (log.getOccurredOn().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Occurred date cannot be in the future.");
        if (log.getDescription() == null || log.getDescription().isBlank())
            throw new IllegalArgumentException("Description is required.");

        dao.insert(log);

        // Push to cloud after successful local save
        SyncService.getInstance().push(new SyncTask(
                "BREAKDOWN_LOG", log.getId(),
                toJson(log), SyncTask.Operation.INSERT));
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
        SyncService.getInstance().push(new SyncTask(
                "BREAKDOWN_LOG", id,
                "{\"id\":" + id + "}", SyncTask.Operation.DELETE));
    }

    private String toJson(BreakdownLog log) {
        return String.format(
                "{\"equipmentId\":%d,\"occurredOn\":\"%s\"," +
                        "\"description\":\"%s\",\"resolvedBy\":\"%s\"}",
                log.getEquipmentId(),
                log.getOccurredOn() != null ? log.getOccurredOn().toString() : "",
                escape(log.getDescription()),
                escape(log.getResolvedBy())
        );
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}