package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.Part;
import com.maintaintrack.api.services.PartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Parts", description = "Spare parts inventory. Stock issues tracked via qty_on_hand changes.")
@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartService service;

    public PartController(PartService service) {
        this.service = service;
    }

    @Operation(summary = "Get all parts")
    @GetMapping
    public List<Part> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Part> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get low-stock parts", description = "Returns parts where qty_on_hand <= min_qty.")
    @GetMapping("/low-stock")
    public List<Part> getLowStock() {
        return service.getLowStock();
    }

    @Operation(summary = "Create a part", description = "Pass supplierId as a query param to link a supplier.")
    @PostMapping
    public ResponseEntity<Part> create(
            @Valid @RequestBody Part part,
            @RequestParam(required = false) Long supplierId) {
        return ResponseEntity.ok(service.create(part, supplierId));
    }

    @Operation(summary = "Update a part")
    @PutMapping("/{id}")
    public ResponseEntity<Part> update(
            @PathVariable Long id,
            @Valid @RequestBody Part part,
            @RequestParam(required = false) Long supplierId) {
        return service.update(id, part, supplierId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a part", description = "Requires ADMIN or MANAGER role.")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}