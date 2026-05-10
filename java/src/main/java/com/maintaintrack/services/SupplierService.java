package com.maintaintrack.services;

import com.maintaintrack.dao.SupplierDAO;
import com.maintaintrack.models.Supplier;

import java.sql.SQLException;
import java.util.List;

/**
 * SupplierService — sits between SupplierController and SupplierDAO.
 * Validates input before passing to the DAO.
 */
public class SupplierService {

    private final SupplierDAO dao = new SupplierDAO();

    public void addSupplier(Supplier s) throws SQLException {
        validate(s);
        dao.insert(s);
    }

    public void updateSupplier(Supplier s) throws SQLException {
        validate(s);
        dao.update(s);
    }

    public void deleteSupplier(int id) throws SQLException {
        dao.delete(id);
    }

    public List<Supplier> getAllSuppliers() throws SQLException {
        return dao.findAll();
    }

    public Supplier getById(int id) throws SQLException {
        return dao.findById(id);
    }

    // ── Validation ────────────────────────────────────────────────────────

    private void validate(Supplier s) {
        if (s.getName() == null || s.getName().isBlank())
            throw new IllegalArgumentException("Supplier name is required.");
    }
}
