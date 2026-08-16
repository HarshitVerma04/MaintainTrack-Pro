package com.maintaintrack.api.services;

import com.maintaintrack.api.config.JwtUtil;
import com.maintaintrack.api.dto.AuthDto;
import com.maintaintrack.api.models.AppUser;
import com.maintaintrack.api.repositories.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private static final List<String> VALID_ROLES =
            List.of("ADMIN", "MANAGER", "TECHNICIAN");

    private final AppUserRepository userRepo;
    private final PasswordEncoder   passwordEncoder;
    private final JwtUtil           jwtUtil;

    public AuthService(AppUserRepository userRepo,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepo        = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil         = jwtUtil;
    }

    public AppUser register(AuthDto.RegisterRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException(
                    "Username already taken: " + req.getUsername());
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already registered: " + req.getEmail());
        }

        String role = req.getRole() != null
                && VALID_ROLES.contains(req.getRole().toUpperCase())
                ? req.getRole().toUpperCase()
                : "TECHNICIAN";

        AppUser user = new AppUser();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(role);

        return userRepo.save(user);
    }

    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        AppUser user = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid username or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        return new AuthDto.LoginResponse(
                token,
                user.getUsername(),
                user.getRole(),
                user.getEmail(),
                28800000L  // 8 hours
        );
    }
}