package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.MaintenanceLog;
import com.maintaintrack.api.services.MaintenanceLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
@CrossOrigin(origins = "*")
public class MaintenanceLogController {

    private final MaintenanceLogService service;

    public MaintenanceLogController(MaintenanceLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<MaintenanceLog> getAll() {
        return service.getAll();
    }

    @GetMapping("/by-equipment/{equipmentId}")
    public List<MaintenanceLog> getByEquipment(@PathVariable Long equipmentId) {
        return service.getByEquipment(equipmentId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceLog> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/log")
    public ResponseEntity<?> log(@RequestBody Map<String, Object> body) {
        try {
            Long   equipmentId = Long.valueOf(body.get("equipmentId").toString());
            String doneOn      = (String) body.getOrDefault("doneOn", null);
            String notes       = (String) body.getOrDefault("notes", null);
            String doneBy      = (String) body.getOrDefault("doneBy", "system");

            return ResponseEntity.ok(service.log(equipmentId, doneOn, notes, doneBy));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}