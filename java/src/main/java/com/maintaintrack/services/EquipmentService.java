package com.maintaintrack.services;

import com.maintaintrack.dao.EquipmentDAO;
import com.maintaintrack.models.Equipment;

import java.sql.SQLException;
import java.util.List;

/**
 * EquipmentService — sits between the Controller and the DAO.
 * Phase 1: mostly delegates to the DAO.
 * Phase 2: next-due date recalculation logic will live here.
 */
public class EquipmentService {

    private final EquipmentDAO dao = new EquipmentDAO();

    public void addEquipment(Equipment e) throws SQLException {
        if (e.getName() == null || e.getName().isBlank())
            throw new IllegalArgumentException("Equipment name is required.");
        if (e.getIntervalDays() <= 0)
            throw new IllegalArgumentException("Interval days must be greater than 0.");
        dao.insert(e);
    }

    public void updateEquipment(Equipment e) throws SQLException {
        if (e.getName() == null || e.getName().isBlank())
            throw new IllegalArgumentException("Equipment name is required.");
        dao.update(e);
    }

    public void deleteEquipment(int id) throws SQLException {
        dao.delete(id);
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
}
