package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.BreakdownLog;
import com.maintaintrack.api.services.BreakdownLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Tag(name = "Breakdowns", description = "Breakdown incident logs. Logging a breakdown sets equipment status to Under Maintenance.")
@RestController
@RequestMapping("/api/breakdowns")
public class BreakdownLogController {

    private final BreakdownLogService service;

    public BreakdownLogController(BreakdownLogService service) {
        this.service = service;
    }

    @Operation(summary = "Get all breakdown logs")
    @GetMapping
    public List<BreakdownLog> getAll() {
        return service.getAll();
    }

    @GetMapping("/by-equipment/{equipmentId}")
    public List<BreakdownLog> getByEquipment(@PathVariable Long equipmentId) {
        return service.getByEquipment(equipmentId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BreakdownLog> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Log a breakdown",
            description = "Body: { equipmentId, occurredOn, description }. " +
                    "Equipment status is automatically set to Under Maintenance.")
    @PostMapping("/log")
    public ResponseEntity<?> log(@RequestBody Map<String, Object> body) {
        try {
            Long   equipmentId  = Long.valueOf(body.get("equipmentId").toString());
            String occurredOn   = (String) body.getOrDefault("occurredOn", null);
            String description  = (String) body.getOrDefault("description", null);
            String resolvedBy   = (String) body.getOrDefault("resolvedBy", null);

            return ResponseEntity.ok(
                    service.log(equipmentId, occurredOn, description, resolvedBy));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Resolve a breakdown",
            description = "Marks the breakdown as resolved. Equipment status reverts to Operational.")
    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body) {
        try {
            String resolvedBy = (String) body.getOrDefault("resolvedBy", "system");
            return service.resolve(id, resolvedBy)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Delete a breakdown log")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}