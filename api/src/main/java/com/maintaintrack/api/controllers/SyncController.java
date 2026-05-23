package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.*;
import com.maintaintrack.api.repositories.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "*")
public class SyncController {

    private final EquipmentRepository      equipRepo;
    private final PartRepository           partRepo;
    private final MaintenanceLogRepository maintenanceRepo;
    private final BreakdownLogRepository   breakdownRepo;

    public SyncController(EquipmentRepository equipRepo,
                          PartRepository partRepo,
                          MaintenanceLogRepository maintenanceRepo,
                          BreakdownLogRepository breakdownRepo) {
        this.equipRepo       = equipRepo;
        this.partRepo        = partRepo;
        this.maintenanceRepo = maintenanceRepo;
        this.breakdownRepo   = breakdownRepo;
    }

    /**
     * Returns all records updated after the given timestamp.
     * Desktop calls this on login and every 5 minutes.
     */
    @GetMapping("/changes")
    public Map<String, Object> getChanges(
            @RequestParam(defaultValue = "1970-01-01T00:00:00") String since) {

        LocalDateTime sinceTime = LocalDateTime.parse(since);
        Map<String, Object> changes = new LinkedHashMap<>();

        changes.put("equipment", equipRepo.findAll().stream()
                .filter(e -> e.getUpdatedAt() != null
                        && e.getUpdatedAt().isAfter(sinceTime))
                .toList());

        changes.put("parts", partRepo.findAll().stream()
                .filter(p -> p.getUpdatedAt() != null
                        && p.getUpdatedAt().isAfter(sinceTime))
                .toList());

        changes.put("maintenanceLogs", maintenanceRepo.findAll().stream()
                .filter(m -> m.getUpdatedAt() != null
                        && m.getUpdatedAt().isAfter(sinceTime))
                .toList());

        changes.put("breakdownLogs", breakdownRepo.findAll().stream()
                .filter(b -> b.getUpdatedAt() != null
                        && b.getUpdatedAt().isAfter(sinceTime))
                .toList());

        changes.put("timestamp", LocalDateTime.now().toString());
        return changes;
    }
}