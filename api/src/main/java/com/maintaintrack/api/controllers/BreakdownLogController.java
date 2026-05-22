package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.BreakdownLog;
import com.maintaintrack.api.services.BreakdownLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/breakdowns")
@CrossOrigin(origins = "*")
public class BreakdownLogController {

    private final BreakdownLogService service;

    public BreakdownLogController(BreakdownLogService service) {
        this.service = service;
    }

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

    @PostMapping
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}