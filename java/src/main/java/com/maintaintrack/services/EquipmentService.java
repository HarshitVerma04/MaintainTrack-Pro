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
        return String.format(
                "{\"name\":\"%s\",\"location\":\"%s\",\"status\":\"%s\"," +
                        "\"nextMaintenanceDate\":\"%s\",\"intervalDays\":%d}",
                escape(e.getName()),
                escape(e.getLocation()),
                escape(e.getStatus()),
                e.getNextMaintenanceDate() != null
                        ? e.getNextMaintenanceDate().toString() : "",
                e.getIntervalDays()
        );
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}