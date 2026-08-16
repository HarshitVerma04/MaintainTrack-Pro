package com.maintaintrack.api.services;

import com.maintaintrack.api.models.BreakdownLog;
import com.maintaintrack.api.models.Equipment;
import com.maintaintrack.api.repositories.BreakdownLogRepository;
import com.maintaintrack.api.repositories.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BreakdownLogService {

    private final BreakdownLogRepository logRepo;
    private final EquipmentRepository    equipRepo;

    public BreakdownLogService(BreakdownLogRepository logRepo,
                               EquipmentRepository equipRepo) {
        this.logRepo   = logRepo;
        this.equipRepo = equipRepo;
    }

    public List<BreakdownLog> getAll() {
        return logRepo.findAll();
    }

    public List<BreakdownLog> getByEquipment(Long equipmentId) {
        return logRepo.findByEquipmentIdOrderByOccurredOnDesc(equipmentId);
    }

    public Optional<BreakdownLog> getById(Long id) {
        return logRepo.findById(id);
    }

    @CacheEvict(value = "dashboard", allEntries = true)
    @Transactional
    public BreakdownLog log(Long equipmentId, String occurredOn,
                            String description, String resolvedBy) {

        Equipment equipment = equipRepo.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Equipment not found: " + equipmentId));

        // Mark equipment as Under Maintenance when breakdown is logged
        equipment.setStatus("Under Maintenance");
        equipment.setSynced(true);
        equipRepo.save(equipment);

        BreakdownLog entry = new BreakdownLog();
        entry.setEquipment(equipment);
        entry.setOccurredOn(occurredOn != null ? occurredOn : LocalDate.now().toString());
        entry.setDescription(description);
        entry.setResolvedBy(resolvedBy);
        entry.setSynced(true);

        return logRepo.save(entry);
    }

    @CacheEvict(value = "dashboard", allEntries = true)
    @Transactional
    public Optional<BreakdownLog> resolve(Long id, String resolvedBy) {
        return logRepo.findById(id).map(entry -> {
            entry.setResolvedBy(resolvedBy);
            entry.setSynced(true);

            // Mark equipment back to Operational on resolve
            Equipment eq = entry.getEquipment();
            eq.setStatus("Operational");
            eq.setSynced(true);
            equipRepo.save(eq);

            return logRepo.save(entry);
        });
    }

    public boolean delete(Long id) {
        if (logRepo.existsById(id)) {
            logRepo.deleteById(id);
            return true;
        }
        return false;
    }
}