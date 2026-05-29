package com.maintaintrack.api.controllers;

import com.maintaintrack.api.models.AppUser;
import com.maintaintrack.api.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Tag(name = "Users", description = "User management. All endpoints require ADMIN role.")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @Operation(summary = "Get all users", description = "Requires ADMIN role.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<AppUser> getAll() {
        return service.getAll();
    }

    @Operation(summary = "Update user role",
            description = "Role must be ADMIN, MANAGER or TECHNICIAN. Requires ADMIN role.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        String role = body.get("role");
        return service.updateRole(id, role)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a user", description = "Requires ADMIN role.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}