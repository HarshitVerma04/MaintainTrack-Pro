package com.maintaintrack.api.controllers;

import com.maintaintrack.api.config.JwtUtil;
import com.maintaintrack.api.dto.AuthDto;
import com.maintaintrack.api.models.AppUser;
import com.maintaintrack.api.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Authentication", description = "Login, register, token refresh and logout")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;
    private final JwtUtil     jwtUtil;

    public AuthController(AuthService service, JwtUtil jwtUtil) {
        this.service  = service;
        this.jwtUtil  = jwtUtil;
    }

    @Operation(summary = "Register a new user",
            description = "Creates a new account. Role must be ADMIN, MANAGER or TECHNICIAN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Username/email already exists")
    })
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

    @Operation(summary = "Login",
            description = "Returns a JWT token valid for 8 hours. Paste it in the Authorize button above.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful — returns token"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
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

    @Operation(summary = "Get current user",
            description = "Returns username and role of the logged-in user.")
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

    @Operation(summary = "Refresh token",
            description = "Issues a fresh JWT using the current valid token.")
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

    @Operation(summary = "Logout",
            description = "Stateless logout — client should discard the token.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}