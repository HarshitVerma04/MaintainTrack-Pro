package com.maintaintrack.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Calls the Spring Boot /auth endpoints.
 * Uses Java 11+ HttpClient — no extra dependencies needed.
 */
public class ApiAuthService {

    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Attempts login. Returns the JWT token string on success.
     * Throws RuntimeException with a user-friendly message on failure.
     */
    public static LoginResult login(String username, String password) throws Exception {
        String body = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                username, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = CLIENT.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String token = extractField(response.body(), "token");
            String role  = extractField(response.body(), "role");
            return new LoginResult(token, role != null ? role : "TECHNICIAN");
        } else if (response.statusCode() == 400) {
            String error = extractField(response.body(), "error");
            throw new RuntimeException(error != null
                    ? error : "Invalid username or password");
        } else {
            throw new RuntimeException(
                    "Server error (" + response.statusCode() + "). Try again.");
        }
    }

    public record LoginResult(String token, String role) {}

    public static boolean isApiReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/actuator/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> response = CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /** Minimal JSON field extractor — no extra library needed. */
    private static String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start  = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }
}
