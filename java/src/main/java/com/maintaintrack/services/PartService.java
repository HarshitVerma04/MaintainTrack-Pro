package com.maintaintrack.services;

import com.maintaintrack.dao.PartDAO;
import com.maintaintrack.models.Part;

import java.sql.SQLException;
import java.util.List;

/**
 * PartService — sits between PartController and PartDAO.
 * Validates input before passing to the DAO.
 * Phase 3: low-stock detection queries will also come through here.
 */
public class PartService {

    private final PartDAO dao = new PartDAO();

    public void addPart(Part p) throws SQLException {
        validate(p);
        dao.insert(p);
    }

    public void updatePart(Part p) throws SQLException {
        validate(p);
        dao.update(p);
    }

    public void deletePart(int id) throws SQLException {
        dao.delete(id);
    }

    public List<Part> getAllParts() throws SQLException {
        return dao.findAll();
    }

    public Part getById(int id) throws SQLException {
        return dao.findById(id);
    }

    public List<Part> getLowStockParts() throws SQLException {
        return dao.findLowStock();
    }

    public List<Part> search(String keyword) throws SQLException {
        return dao.search(keyword);
    }

    // ── Validation ────────────────────────────────────────────────────────

    private void validate(Part p) {
        if (p.getName() == null || p.getName().isBlank())
            throw new IllegalArgumentException("Part name is required.");
        if (p.getQtyOnHand() < 0)
            throw new IllegalArgumentException("Quantity on hand cannot be negative.");
        if (p.getMinQty() < 0)
            throw new IllegalArgumentException("Minimum quantity cannot be negative.");
        if (p.getUnitCost() < 0)
            throw new IllegalArgumentException("Unit cost cannot be negative.");
        if (p.getUnit() == null || p.getUnit().isBlank())
            throw new IllegalArgumentException("Unit is required (e.g. pcs, kg, can).");
    }
}
