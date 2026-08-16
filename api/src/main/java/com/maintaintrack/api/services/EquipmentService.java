package com.maintaintrack.api.services;

import com.maintaintrack.api.models.Equipment;
import com.maintaintrack.api.repositories.EquipmentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EquipmentService {

    private final EquipmentRepository repo;

    public EquipmentService(EquipmentRepository repo) {
        this.repo = repo;
    }

    @Cacheable("equipment")
    public List<Equipment> getAll() {
        return repo.findAll();
    }

    public Optional<Equipment> getById(Long id) {
        return repo.findById(id);
    }

    // Evict both equipment list AND dashboard on any write
    @Caching(evict = {
            @CacheEvict(value = "equipment", allEntries = true),
            @CacheEvict(value = "dashboard", allEntries = true)
    })
    public Equipment create(Equipment equipment) {
        if (equipment.getStatus() == null)      equipment.setStatus("Operational");
        if (equipment.getIntervalDays() == null) equipment.setIntervalDays(30);
        equipment.setSynced(true);
        return repo.save(equipment);
    }

    @Caching(evict = {
            @CacheEvict(value = "equipment", allEntries = true),
            @CacheEvict(value = "dashboard", allEntries = true)
    })
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

    @Caching(evict = {
            @CacheEvict(value = "equipment", allEntries = true),
            @CacheEvict(value = "dashboard", allEntries = true)
    })
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