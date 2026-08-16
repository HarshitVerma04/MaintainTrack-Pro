package com.maintaintrack.api.dto;

public class AuthDto {

    // ── Register request ─────────────────────────────────────
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    // ── Login request ────────────────────────────────────────
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    // ── Login response ───────────────────────────────────────
    public static class LoginResponse {
        private String token;
        private String username;
        private String role;
        private String email;
        private long expiresInMs;

        public LoginResponse(String token, String username,
                             String role, String email, long expiresInMs) {
            this.token       = token;
            this.username    = username;
            this.role        = role;
            this.email       = email;
            this.expiresInMs = expiresInMs;
        }

        public String getToken() { return token; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getEmail() { return email; }
        public long getExpiresInMs() { return expiresInMs; }
    }
}