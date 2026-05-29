package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.WorkOrder;
import com.maintaintrack.api.services.WorkOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Tag(name = "Work Orders", description = "Work order lifecycle: Open → In Progress → Completed.")
@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService service;

    public WorkOrderController(WorkOrderService service) {
        this.service = service;
    }

    @Operation(summary = "Get all work orders")
    @GetMapping
    public List<WorkOrder> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrder> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get work orders by status",
            description = "Status values: Open, In Progress, Completed.")
    @GetMapping("/status/{status}")
    public List<WorkOrder> getByStatus(@PathVariable String status) {
        return service.getByStatus(status);
    }

    @Operation(summary = "Create a work order",
            description = "Pass equipmentId as a query param. Body: { title, description, priority, assignedTo }.")
    @PostMapping
    public ResponseEntity<?> create(
            @RequestParam Long equipmentId,
            @RequestBody WorkOrder workOrder) {
        try {
            return ResponseEntity.ok(service.create(equipmentId, workOrder));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Update work order status",
            description = "Body: { \"status\": \"In Progress\" }")
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return service.updateStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update a work order")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody WorkOrder workOrder) {
        return service.update(id, workOrder)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a work order")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}