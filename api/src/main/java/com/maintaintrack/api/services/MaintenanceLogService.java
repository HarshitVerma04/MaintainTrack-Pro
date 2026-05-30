package com.maintaintrack.api.services;

import com.maintaintrack.api.models.Equipment;
import com.maintaintrack.api.models.MaintenanceLog;
import com.maintaintrack.api.repositories.EquipmentRepository;
import com.maintaintrack.api.repositories.MaintenanceLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.maintaintrack.api.config.SecurityUtils;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MaintenanceLogService {

    private final MaintenanceLogRepository logRepo;
    private final EquipmentRepository      equipRepo;

    public MaintenanceLogService(MaintenanceLogRepository logRepo,
                                 EquipmentRepository equipRepo) {
        this.logRepo   = logRepo;
        this.equipRepo = equipRepo;
    }

    public List<MaintenanceLog> getAll() {
        return logRepo.findAll();
    }

    public List<MaintenanceLog> getByEquipment(Long equipmentId) {
        return logRepo.findByEquipmentIdOrderByDoneOnDesc(equipmentId);
    }

    public Optional<MaintenanceLog> getById(Long id) {
        return logRepo.findById(id);
    }

    @CacheEvict(value = "dashboard", allEntries = true)
    @Transactional
    public MaintenanceLog log(Long equipmentId, String doneOn, String notes) {

        Equipment equipment = equipRepo.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));

        MaintenanceLog entry = new MaintenanceLog();
        entry.setEquipment(equipment);
        entry.setDoneOn(doneOn != null ? doneOn : LocalDate.now().toString());
        entry.setNotes(notes);
        entry.setDoneBy(SecurityUtils.getCurrentUsername()); // ← auto from JWT
        entry.setSynced(true);
        logRepo.save(entry);

        if (equipment.getIntervalDays() != null) {
            LocalDate next = LocalDate.parse(entry.getDoneOn())
                    .plusDays(equipment.getIntervalDays());
            equipment.setNextMaintenanceDate(next.toString());
            equipment.setSynced(true);
            equipRepo.save(equipment);
        }

        return entry;
    }

    public boolean delete(Long id) {
        if (logRepo.existsById(id)) {
            logRepo.deleteById(id);
            return true;
        }
        return false;
    }
}