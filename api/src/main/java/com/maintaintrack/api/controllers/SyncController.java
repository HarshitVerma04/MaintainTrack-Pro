package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.*;
import com.maintaintrack.api.repositories.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final EquipmentRepository      equipRepo;
    private final PartRepository           partRepo;
    private final SupplierRepository       supplierRepo;
    private final MaintenanceLogRepository maintenanceRepo;
    private final BreakdownLogRepository   breakdownRepo;
    private final WorkOrderRepository      workOrderRepo;

    public SyncController(EquipmentRepository equipRepo,
                          PartRepository partRepo,
                          SupplierRepository supplierRepo,
                          MaintenanceLogRepository maintenanceRepo,
                          BreakdownLogRepository breakdownRepo,
                          WorkOrderRepository workOrderRepo) {
        this.equipRepo       = equipRepo;
        this.partRepo        = partRepo;
        this.supplierRepo    = supplierRepo;
        this.maintenanceRepo = maintenanceRepo;
        this.breakdownRepo   = breakdownRepo;
        this.workOrderRepo   = workOrderRepo;
    }

    // ── GET /api/sync/changes — desktop pulls cloud changes ──────────────────
    // Desktop calls this on login and every 5 minutes.
    // Returns only records updated after the given timestamp.
    @GetMapping("/changes")
    public Map<String, Object> getChanges(
            @RequestParam(defaultValue = "1970-01-01T00:00:00") String since) {

        LocalDateTime sinceTime = LocalDateTime.parse(since);
        Map<String, Object> changes = new LinkedHashMap<>();

        // Equipment
        changes.put("equipment", equipRepo.findAll().stream()
                .filter(e -> e.getUpdatedAt() == null
                        || e.getUpdatedAt().isAfter(sinceTime))
                .toList());

// Parts
        changes.put("parts", partRepo.findAll().stream()
                .filter(p -> p.getUpdatedAt() == null
                        || p.getUpdatedAt().isAfter(sinceTime))
                .toList());

// Maintenance logs
        changes.put("maintenanceLogs", maintenanceRepo.findAll().stream()
                .filter(m -> m.getUpdatedAt() == null
                        || m.getUpdatedAt().isAfter(sinceTime))
                .toList());

// Breakdown logs
        changes.put("breakdownLogs", breakdownRepo.findAll().stream()
                .filter(b -> b.getUpdatedAt() == null
                        || b.getUpdatedAt().isAfter(sinceTime))
                .toList());

        changes.put("timestamp", LocalDateTime.now().toString());
        return changes;
    }

    // ── POST /api/sync/push — desktop pushes local records to cloud ──────────
    // Receives batches of records created or edited on the desktop.
    // Uses sync_id as the universal key — last write wins on updated_at.
    @PostMapping("/push")
    public ResponseEntity<Map<String, Object>> push(
            @RequestBody Map<String, Object> payload) {

        int accepted = 0;
        int skipped  = 0;
        List<String> errors = new ArrayList<>();

        // ── Equipment ────────────────────────────────────────────────────────
        List<Map<String, Object>> equipList = getList(payload, "equipment");
        for (Map<String, Object> rec : equipList) {
            try {
                String syncId     = str(rec, "syncId");
                String updatedAt  = str(rec, "updatedAt");
                if (syncId == null) { skipped++; continue; }

                Optional<Equipment> existing = equipRepo.findBySyncId(syncId);
                if (existing.isPresent()) {
                    Equipment cloud = existing.get();
                    // Skip if cloud is same age or newer
                    if (cloud.getUpdatedAt() != null && updatedAt != null &&
                            !LocalDateTime.parse(updatedAt)
                                    .isAfter(cloud.getUpdatedAt())) {
                        skipped++; continue;
                    }
                    // Desktop is newer — overwrite
                    cloud.setName(str(rec, "name"));
                    cloud.setLocation(str(rec, "location"));
                    cloud.setStatus(str(rec, "status"));
                    cloud.setNextMaintenanceDate(str(rec, "nextMaintenanceDate"));
                    cloud.setIntervalDays(intVal(rec, "intervalDays"));
                    equipRepo.save(cloud);
                } else {
                    // New record from desktop — insert
                    Equipment e = new Equipment();
                    e.setSyncId(syncId);
                    e.setName(str(rec, "name"));
                    e.setLocation(str(rec, "location"));
                    e.setStatus(str(rec, "status"));
                    e.setNextMaintenanceDate(str(rec, "nextMaintenanceDate"));
                    e.setIntervalDays(intVal(rec, "intervalDays"));
                    equipRepo.save(e);
                }
                accepted++;
            } catch (Exception ex) {
                errors.add("equipment[" + rec.get("syncId") + "]: " + ex.getMessage());
            }
        }

        // ── Suppliers ────────────────────────────────────────────────────────
        List<Map<String, Object>> supplierList = getList(payload, "suppliers");
        for (Map<String, Object> rec : supplierList) {
            try {
                String syncId = str(rec, "syncId");
                String updatedAt = str(rec, "updatedAt");
                if (syncId == null) { skipped++; continue; }

                Optional<Supplier> existing = supplierRepo.findBySyncId(syncId);
                if (existing.isPresent()) {
                    Supplier cloud = existing.get();
                    if (cloud.getUpdatedAt() != null && updatedAt != null &&
                            !LocalDateTime.parse(updatedAt)
                                    .isAfter(cloud.getUpdatedAt())) {
                        skipped++; continue;
                    }
                    cloud.setName(str(rec, "name"));
                    cloud.setContactName(str(rec, "contactName"));
                    cloud.setPhone(str(rec, "phone"));
                    cloud.setEmail(str(rec, "email"));
                    supplierRepo.save(cloud);
                } else {
                    Supplier s = new Supplier();
                    s.setSyncId(syncId);
                    s.setName(str(rec, "name"));
                    s.setContactName(str(rec, "contactName"));
                    s.setPhone(str(rec, "phone"));
                    s.setEmail(str(rec, "email"));
                    supplierRepo.save(s);
                }
                accepted++;
            } catch (Exception ex) {
                errors.add("supplier[" + rec.get("syncId") + "]: " + ex.getMessage());
            }
        }

        // ── Parts ────────────────────────────────────────────────────────────
        List<Map<String, Object>> partList = getList(payload, "parts");
        for (Map<String, Object> rec : partList) {
            try {
                String syncId = str(rec, "syncId");
                String updatedAt = str(rec, "updatedAt");
                if (syncId == null) { skipped++; continue; }

                Optional<Part> existing = partRepo.findBySyncId(syncId);
                if (existing.isPresent()) {
                    Part cloud = existing.get();
                    if (cloud.getUpdatedAt() != null && updatedAt != null &&
                            !LocalDateTime.parse(updatedAt)
                                    .isAfter(cloud.getUpdatedAt())) {
                        skipped++; continue;
                    }
                    cloud.setName(str(rec, "name"));
                    cloud.setQtyOnHand(intVal(rec, "qtyOnHand"));
                    cloud.setMinQty(intVal(rec, "minQty"));
                    cloud.setUnit(str(rec, "unit"));
                    if (rec.get("unitCost") != null)
                        cloud.setUnitCost(new java.math.BigDecimal(
                                rec.get("unitCost").toString()));
                    partRepo.save(cloud);
                } else {
                    Part p = new Part();
                    p.setSyncId(syncId);
                    p.setName(str(rec, "name"));
                    p.setQtyOnHand(intVal(rec, "qtyOnHand"));
                    p.setMinQty(intVal(rec, "minQty"));
                    p.setUnit(str(rec, "unit"));
                    if (rec.get("unitCost") != null)
                        p.setUnitCost(new java.math.BigDecimal(
                                rec.get("unitCost").toString()));
                    partRepo.save(p);
                }
                accepted++;
            } catch (Exception ex) {
                errors.add("part[" + rec.get("syncId") + "]: " + ex.getMessage());
            }
        }

        // ── Maintenance Logs ─────────────────────────────────────────────────
        List<Map<String, Object>> maintList = getList(payload, "maintenanceLogs");
        for (Map<String, Object> rec : maintList) {
            try {
                String syncId = str(rec, "syncId");
                String updatedAt = str(rec, "updatedAt");
                if (syncId == null) { skipped++; continue; }

                // Resolve equipment by sync_id
                String equipSyncId = str(rec, "equipmentSyncId");
                Optional<Equipment> equip = equipSyncId != null
                        ? equipRepo.findBySyncId(equipSyncId) : Optional.empty();
                if (equip.isEmpty()) { skipped++; continue; }

                Optional<MaintenanceLog> existing = maintenanceRepo.findBySyncId(syncId);
                if (existing.isPresent()) {
                    MaintenanceLog cloud = existing.get();
                    if (cloud.getUpdatedAt() != null && updatedAt != null &&
                            !LocalDateTime.parse(updatedAt)
                                    .isAfter(cloud.getUpdatedAt())) {
                        skipped++; continue;
                    }
                    cloud.setDoneOn(str(rec, "doneOn"));
                    cloud.setNotes(str(rec, "notes"));
                    cloud.setDoneBy(str(rec, "doneBy"));
                    maintenanceRepo.save(cloud);
                } else {
                    MaintenanceLog m = new MaintenanceLog();
                    m.setSyncId(syncId);
                    m.setEquipment(equip.get());
                    m.setDoneOn(str(rec, "doneOn"));
                    m.setNotes(str(rec, "notes"));
                    m.setDoneBy(str(rec, "doneBy"));
                    maintenanceRepo.save(m);
                }
                accepted++;
            } catch (Exception ex) {
                errors.add("maintenanceLog[" + rec.get("syncId") + "]: " + ex.getMessage());
            }
        }

        // ── Breakdown Logs ───────────────────────────────────────────────────
        List<Map<String, Object>> breakList = getList(payload, "breakdownLogs");
        for (Map<String, Object> rec : breakList) {
            try {
                String syncId = str(rec, "syncId");
                String updatedAt = str(rec, "updatedAt");
                if (syncId == null) { skipped++; continue; }

                String equipSyncId = str(rec, "equipmentSyncId");
                Optional<Equipment> equip = equipSyncId != null
                        ? equipRepo.findBySyncId(equipSyncId) : Optional.empty();
                if (equip.isEmpty()) { skipped++; continue; }

                Optional<BreakdownLog> existing = breakdownRepo.findBySyncId(syncId);
                if (existing.isPresent()) {
                    BreakdownLog cloud = existing.get();
                    if (cloud.getUpdatedAt() != null && updatedAt != null &&
                            !LocalDateTime.parse(updatedAt)
                                    .isAfter(cloud.getUpdatedAt())) {
                        skipped++; continue;
                    }
                    cloud.setOccurredOn(str(rec, "occurredOn"));
                    cloud.setDescription(str(rec, "description"));
                    cloud.setResolvedBy(str(rec, "resolvedBy"));
                    breakdownRepo.save(cloud);
                } else {
                    BreakdownLog b = new BreakdownLog();
                    b.setSyncId(syncId);
                    b.setEquipment(equip.get());
                    b.setOccurredOn(str(rec, "occurredOn"));
                    b.setDescription(str(rec, "description"));
                    b.setResolvedBy(str(rec, "resolvedBy"));
                    breakdownRepo.save(b);
                }
                accepted++;
            } catch (Exception ex) {
                errors.add("breakdownLog[" + rec.get("syncId") + "]: " + ex.getMessage());
            }
        }

        // ── Summary response ─────────────────────────────────────────────────
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted",  accepted);
        result.put("skipped",   skipped);
        result.put("errors",    errors);
        result.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(result);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val instanceof List) return (List<Map<String, Object>>) val;
        return Collections.emptyList();
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Integer intVal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        try { return Integer.parseInt(val.toString()); }
        catch (Exception e) { return null; }
    }
}