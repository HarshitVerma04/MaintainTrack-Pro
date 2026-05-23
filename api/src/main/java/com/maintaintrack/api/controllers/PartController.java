package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.Part;
import com.maintaintrack.api.services.PartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
@CrossOrigin(origins = "*")
public class PartController {

    private final PartService service;

    public PartController(PartService service) {
        this.service = service;
    }

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

    @GetMapping("/low-stock")
    public List<Part> getLowStock() {
        return service.getLowStock();
    }

    @PostMapping
    public ResponseEntity<Part> create(
            @Valid @RequestBody Part part,
            @RequestParam(required = false) Long supplierId) {
        return ResponseEntity.ok(service.create(part, supplierId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Part> update(
            @PathVariable Long id,
            @Valid @RequestBody Part part,
            @RequestParam(required = false) Long supplierId) {
        return service.update(id, part, supplierId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}