package com.maintaintrack.api.services;

import com.maintaintrack.api.models.Equipment;
import com.maintaintrack.api.models.WorkOrder;
import com.maintaintrack.api.repositories.EquipmentRepository;
import com.maintaintrack.api.repositories.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.util.Optional;

@Service
public class WorkOrderService {

    private final WorkOrderRepository repo;
    private final EquipmentRepository equipRepo;

    public WorkOrderService(WorkOrderRepository repo, EquipmentRepository equipRepo) {
        this.repo      = repo;
        this.equipRepo = equipRepo;
    }

    public List<WorkOrder> getAll() { return repo.findAll(); }

    public Optional<WorkOrder> getById(Long id) { return repo.findById(id); }

    public List<WorkOrder> getByStatus(String status) { return repo.findByStatus(status); }

    @CacheEvict(value = "dashboard", allEntries = true)
    public WorkOrder create(Long equipmentId, WorkOrder wo) {
        Equipment equipment = equipRepo.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));
        wo.setEquipment(equipment);
        if (wo.getStatus() == null)   wo.setStatus("Open");
        if (wo.getPriority() == null) wo.setPriority("Medium");
        wo.setSynced(true);
        return repo.save(wo);
    }

    @CacheEvict(value = "dashboard", allEntries = true)
    public Optional<WorkOrder> updateStatus(Long id, String status) {
        return repo.findById(id).map(wo -> {
            wo.setStatus(status);
            wo.setSynced(true);
            return repo.save(wo);
        });
    }

    public Optional<WorkOrder> update(Long id, WorkOrder updated) {
        return repo.findById(id).map(wo -> {
            wo.setTitle(updated.getTitle());
            wo.setDescription(updated.getDescription());
            wo.setStatus(updated.getStatus());
            wo.setPriority(updated.getPriority());
            wo.setAssignedTo(updated.getAssignedTo());
            wo.setSynced(true);
            return repo.save(wo);
        });
    }

    public boolean delete(Long id) {
        if (repo.existsById(id)) { repo.deleteById(id); return true; }
        return false;
    }
}