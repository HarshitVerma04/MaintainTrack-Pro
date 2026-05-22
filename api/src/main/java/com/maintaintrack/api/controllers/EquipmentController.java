package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.Equipment;
import com.maintaintrack.api.services.EquipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@CrossOrigin(origins = "*")
public class EquipmentController {

    private final EquipmentService service;

    public EquipmentController(EquipmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Equipment> getAll() {
        return service.getAll();
    }

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

    @PostMapping
    public ResponseEntity<Equipment> create(@RequestBody Equipment equipment) {
        return ResponseEntity.ok(service.create(equipment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Equipment> update(@PathVariable Long id,
                                            @RequestBody Equipment equipment) {
        return service.update(id, equipment)
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