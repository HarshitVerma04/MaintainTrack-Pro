package com.maintaintrack.api.services;

import com.maintaintrack.api.models.Supplier;
import com.maintaintrack.api.repositories.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {

    private final SupplierRepository repo;

    public SupplierService(SupplierRepository repo) {
        this.repo = repo;
    }

    public List<Supplier> getAll() {
        return repo.findAll();
    }

    public Optional<Supplier> getById(Long id) {
        return repo.findById(id);
    }

    public Supplier create(Supplier supplier) {
        supplier.setSynced(true);
        return repo.save(supplier);
    }

    public Optional<Supplier> update(Long id, Supplier updated) {
        return repo.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setContactName(updated.getContactName());
            existing.setPhone(updated.getPhone());
            existing.setEmail(updated.getEmail());
            existing.setSynced(true);
            return repo.save(existing);
        });
    }

    public boolean delete(Long id) {
        if (repo.existsById(id)) {
            repo.deleteById(id);
            return true;
        }
        return false;
    }
}