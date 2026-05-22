package com.maintaintrack.api.services;

import com.maintaintrack.api.models.Part;
import com.maintaintrack.api.models.Supplier;
import com.maintaintrack.api.repositories.PartRepository;
import com.maintaintrack.api.repositories.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PartService {

    private final PartRepository partRepo;
    private final SupplierRepository supplierRepo;

    public PartService(PartRepository partRepo, SupplierRepository supplierRepo) {
        this.partRepo = partRepo;
        this.supplierRepo = supplierRepo;
    }

    public List<Part> getAll() {
        return partRepo.findAll();
    }

    public Optional<Part> getById(Long id) {
        return partRepo.findById(id);
    }

    public List<Part> getLowStock() {
        return partRepo.findLowStock();
    }

    public Part create(Part part, Long supplierId) {
        if (supplierId != null) {
            Supplier supplier = supplierRepo.findById(supplierId)
                    .orElseThrow(() -> new RuntimeException("Supplier not found: " + supplierId));
            part.setSupplier(supplier);
        }
        if (part.getQtyOnHand() == null) part.setQtyOnHand(0);
        if (part.getMinQty() == null)    part.setMinQty(5);
        if (part.getUnit() == null)      part.setUnit("pcs");
        part.setSynced(true);
        return partRepo.save(part);
    }

    public Optional<Part> update(Long id, Part updated, Long supplierId) {
        return partRepo.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setQtyOnHand(updated.getQtyOnHand());
            existing.setMinQty(updated.getMinQty());
            existing.setUnit(updated.getUnit());
            existing.setUnitCost(updated.getUnitCost());
            if (supplierId != null) {
                supplierRepo.findById(supplierId).ifPresent(existing::setSupplier);
            }
            existing.setSynced(true);
            return partRepo.save(existing);
        });
    }

    public boolean delete(Long id) {
        if (partRepo.existsById(id)) {
            partRepo.deleteById(id);
            return true;
        }
        return false;
    }
}