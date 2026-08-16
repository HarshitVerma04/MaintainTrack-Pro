package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.Equipment;
import com.maintaintrack.api.services.EquipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Equipment", description = "CRUD for facility equipment. Delete requires ADMIN or MANAGER role.")
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService service;

    public EquipmentController(EquipmentService service) {
        this.service = service;
    }

    @Operation(summary = "Get all equipment")
    @GetMapping
    public List<Equipment> getAll() {
        return service.getAll();
    }

    @Operation(summary = "Get equipment by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Equipment> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public List<Equipment> getByStatus(@PathVariable String status) {
        return service.getByStatus(status);
    }

    @Operation(summary = "Create equipment")
    @PostMapping
    public ResponseEntity<Equipment> create(@Valid @RequestBody Equipment equipment) {
        return ResponseEntity.ok(service.create(equipment));
    }

    @Operation(summary = "Update equipment")
    @PutMapping("/{id}")
    public ResponseEntity<Equipment> update(@PathVariable Long id,
                                            @Valid @RequestBody Equipment equipment) {
        return service.update(id, equipment)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete equipment", description = "Requires ADMIN or MANAGER role.")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}