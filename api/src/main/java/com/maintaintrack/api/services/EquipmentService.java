package com.maintaintrack.api.services;

import com.maintaintrack.api.models.Equipment;
import com.maintaintrack.api.repositories.EquipmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EquipmentService {

    private final EquipmentRepository repo;

    public EquipmentService(EquipmentRepository repo) {
        this.repo = repo;
    }

    public List<Equipment> getAll() {
        return repo.findAll();
    }

    public Optional<Equipment> getById(Long id) {
        return repo.findById(id);
    }

    public Equipment create(Equipment equipment) {
        if (equipment.getStatus() == null) equipment.setStatus("Operational");
        if (equipment.getIntervalDays() == null) equipment.setIntervalDays(30);
        equipment.setSynced(true);
        return repo.save(equipment);
    }

    public Optional<Equipment> update(Long id, Equipment updated) {
        return repo.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setLocation(updated.getLocation());
            existing.setStatus(updated.getStatus());
            existing.setNextMaintenanceDate(updated.getNextMaintenanceDate());
            existing.setIntervalDays(updated.getIntervalDays());
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

    public List<Equipment> getByStatus(String status) {
        return repo.findByStatus(status);
    }
}