package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.MaintenanceLog;
import com.maintaintrack.api.services.MaintenanceLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Tag(name = "Maintenance", description = "Preventive maintenance logs. done_by is auto-filled from the JWT.")
@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceLogController {

    private final MaintenanceLogService service;

    public MaintenanceLogController(MaintenanceLogService service) {
        this.service = service;
    }

    @Operation(summary = "Get all maintenance logs")
    @GetMapping
    public List<MaintenanceLog> getAll() {
        return service.getAll();
    }

    @Operation(summary = "Get logs for one equipment")
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

    @Operation(summary = "Log a maintenance event",
            description = "Body: { equipmentId, doneOn (yyyy-MM-dd), notes }. " +
                    "done_by is auto-populated from the logged-in user's JWT.")
    @PostMapping("/log")
    public ResponseEntity<?> log(@RequestBody Map<String, Object> body) {
        try {
            Long   equipmentId = Long.valueOf(body.get("equipmentId").toString());
            String doneOn      = (String) body.getOrDefault("doneOn", null);
            String notes       = (String) body.getOrDefault("notes", null);

            return ResponseEntity.ok(service.log(equipmentId, doneOn, notes));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Delete a maintenance log")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}