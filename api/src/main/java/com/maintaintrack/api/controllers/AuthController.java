package com.maintaintrack.api.controllers;

import com.maintaintrack.api.config.JwtUtil;
import com.maintaintrack.api.dto.AuthDto;
import com.maintaintrack.api.models.AppUser;
import com.maintaintrack.api.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService service;
    private final JwtUtil     jwtUtil;

    public AuthController(AuthService service, JwtUtil jwtUtil) {
        this.service  = service;
        this.jwtUtil  = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthDto.RegisterRequest req) {
        try {
            AppUser user = service.register(req);
            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully",
                    "username", user.getUsername(),
                    "role", user.getRole()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDto.LoginRequest req) {
        try {
            AuthDto.LoginResponse response = service.login(req);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(
            org.springframework.security.core.Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of(
                "username", auth.getName(),
                "role", auth.getAuthorities().iterator().next()
                        .getAuthority().replace("ROLE_", "")
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            org.springframework.security.core.Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        String username = auth.getName();
        String role     = auth.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");

        String newToken = jwtUtil.generateToken(username, role);
        return ResponseEntity.ok(Map.of(
                "token",       newToken,
                "username",    username,
                "role",        role,
                "expiresInMs", 28800000L
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}