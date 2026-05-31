package com.maintaintrack.services;

import com.maintaintrack.dao.SupplierDAO;
import com.maintaintrack.models.Supplier;
import com.maintaintrack.sync.SyncService;
import com.maintaintrack.sync.SyncTask;

import java.sql.SQLException;
import java.util.List;

public class SupplierService {

    private final SupplierDAO dao = new SupplierDAO();

    public void addSupplier(Supplier s) throws SQLException {
        if (s.getName() == null || s.getName().isBlank())
            throw new IllegalArgumentException("Supplier name is required.");
        dao.insert(s);
        SyncService.getInstance().push(new SyncTask(
                "SUPPLIER", s.getId(),
                toJson(s), SyncTask.Operation.INSERT));
    }

    public void updateSupplier(Supplier s) throws SQLException {
        if (s.getName() == null || s.getName().isBlank())
            throw new IllegalArgumentException("Supplier name is required.");
        dao.update(s);
        SyncService.getInstance().push(new SyncTask(
                "SUPPLIER", s.getId(),
                toJson(s), SyncTask.Operation.UPDATE));
    }

    public void deleteSupplier(int id) throws SQLException {
        dao.delete(id);
        SyncService.getInstance().push(new SyncTask(
                "SUPPLIER", id,
                "{\"id\":" + id + "}", SyncTask.Operation.DELETE));
    }

    public List<Supplier> getAllSuppliers() throws SQLException {
        return dao.findAll();
    }

    public Supplier getById(int id) throws SQLException {
        return dao.findById(id);
    }

    public List<Supplier> search(String keyword) throws SQLException {
        return dao.search(keyword);
    }

    private String toJson(Supplier s) {
        return String.format(
                "{\"name\":\"%s\",\"contactName\":\"%s\"," +
                        "\"phone\":\"%s\",\"email\":\"%s\"}",
                escape(s.getName()),
                escape(s.getContactName()),
                escape(s.getPhone()),
                escape(s.getEmail())
        );
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}