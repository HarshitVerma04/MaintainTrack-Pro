package com.maintaintrack.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Schedules a token refresh 10 minutes before expiry.
 * Runs silently in the background — user never notices.
 */
public class TokenRefreshService {

    private static final String BASE_URL = "http://localhost:8080";
    private static final TokenRefreshService INSTANCE = new TokenRefreshService();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "token-refresh");
                t.setDaemon(true); // dies with the app
                return t;
            });

    private ScheduledFuture<?> pendingRefresh;

    private TokenRefreshService() {}

    public static TokenRefreshService getInstance() { return INSTANCE; }

    /**
     * Call this after every successful login or refresh.
     * Schedules the next refresh for 7h50m from now (10min before 8h expiry).
     */
    public void scheduleRefresh() {
        cancelPending();

        if (!AuthContext.getInstance().isLoggedIn()) return;

        long delaySeconds = (8 * 60 * 60) - (10 * 60); // 7h50m

        pendingRefresh = scheduler.schedule(() -> {
            try {
                String newToken = callRefreshEndpoint();
                if (newToken != null) {
                    AuthContext ctx = AuthContext.getInstance();
                    ctx.setSession(newToken, ctx.getUsername(), ctx.getRole());
                    scheduleRefresh(); // schedule the next one
                    System.out.println("[Auth] Token refreshed silently.");
                }
            } catch (Exception e) {
                System.err.println("[Auth] Token refresh failed: " + e.getMessage());
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    public void cancelPending() {
        if (pendingRefresh != null && !pendingRefresh.isDone()) {
            pendingRefresh.cancel(false);
        }
    }

    public void shutdown() {
        cancelPending();
        scheduler.shutdown();
    }

    private String callRefreshEndpoint() throws Exception {
        String token = AuthContext.getInstance().getToken();
        if (token == null) return null;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/refresh"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return extractField(response.body(), "token");
        }
        return null;
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start  = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }
}