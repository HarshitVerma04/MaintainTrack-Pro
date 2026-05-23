package com.maintaintrack.sync;

import com.maintaintrack.auth.AuthContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Background sync engine.
 * Queues tasks locally, drains to cloud when online.
 */
public class SyncService {

    private static final SyncService INSTANCE = new SyncService();
    private static final int MAX_RETRIES = 3;

    private final SyncQueueDAO queueDAO = new SyncQueueDAO();
    private final HttpClient   client   = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sync-worker");
                t.setDaemon(true);
                return t;
            });

    // UI callback — updates status label
    private Consumer<String> statusCallback;

    private SyncService() {}

    public static SyncService getInstance() { return INSTANCE; }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    /**
     * Call this once on app startup.
     * Drains any queue left over from a previous session,
     * then checks every 30 seconds.
     */
    public void start() {
        try {
            SyncQueueDAO.createTableIfMissing();
        } catch (Exception e) {
            System.err.println("[Sync] Failed to create queue table: " + e.getMessage());
        }

        // Drain immediately on startup, then every 30 seconds
        scheduler.scheduleAtFixedRate(this::drainQueue, 0, 30, TimeUnit.SECONDS);
        System.out.println("[Sync] SyncService started.");
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * Push a task — if online send immediately,
     * otherwise add to persistent queue.
     */
    public void push(SyncTask task) {
        if (!AuthContext.getInstance().isLoggedIn()) return;

        if (NetworkUtil.isOnline()) {
            CompletableFuture.runAsync(() -> sendTask(task));
        } else {
            try {
                queueDAO.enqueue(task);
                updateStatus("Offline — " + pendingCount() + " pending");
                System.out.println("[Sync] Queued offline: " + task.getTableName());
            } catch (Exception e) {
                System.err.println("[Sync] Failed to queue: " + e.getMessage());
            }
        }
    }

    private void drainQueue() {
        if (!AuthContext.getInstance().isLoggedIn()) return;
        if (!NetworkUtil.isOnline()) {
            updateStatus("Offline — " + pendingCount() + " pending");
            return;
        }

        try {
            List<SyncTask> pending = queueDAO.getPending();
            if (pending.isEmpty()) {
                updateStatus("Synced ✓");
                return;
            }

            updateStatus("Syncing " + pending.size() + " items...");
            for (SyncTask task : pending) {
                if (task.getRetryCount() >= MAX_RETRIES) {
                    queueDAO.delete(task.getId());
                    System.err.println("[Sync] Dropped after max retries: "
                            + task.getTableName() + " #" + task.getLocalId());
                    continue;
                }
                boolean success = sendTask(task);
                if (success) {
                    queueDAO.delete(task.getId());
                } else {
                    queueDAO.incrementRetry(task.getId());
                }
            }
            updateStatus("Synced ✓");
        } catch (Exception e) {
            System.err.println("[Sync] Drain error: " + e.getMessage());
        }
    }

    private boolean sendTask(SyncTask task) {
        try {
            String token = AuthContext.getInstance().getToken();
            String url   = buildUrl(task);
            String method = task.getOperation() == SyncTask.Operation.DELETE
                    ? "DELETE" : "POST";

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(10));

            if (task.getOperation() == SyncTask.Operation.DELETE) {
                builder.DELETE();
            } else {
                builder.POST(HttpRequest.BodyPublishers.ofString(task.getPayload()));
            }

            HttpResponse<String> response = client.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString());

            boolean success = response.statusCode() >= 200
                    && response.statusCode() < 300;
            if (success) {
                System.out.println("[Sync] ✓ " + task.getTableName()
                        + " #" + task.getLocalId());
            }
            return success;

        } catch (Exception e) {
            System.err.println("[Sync] Send failed: " + e.getMessage());
            return false;
        }
    }

    private String buildUrl(SyncTask task) {
        String base = "http://localhost:8080/api/";
        return switch (task.getTableName()) {
            case "EQUIPMENT"      -> base + "equipment";
            case "SUPPLIER"       -> base + "suppliers";
            case "PART"           -> base + "parts";
            case "MAINTENANCE_LOG"-> base + "maintenance/log";
            case "BREAKDOWN_LOG"  -> base + "breakdowns";
            case "ISSUE_RECORD"   -> base + "parts/issue";
            default -> base + task.getTableName().toLowerCase();
        };
    }

    private void updateStatus(String status) {
        if (statusCallback != null) {
            javafx.application.Platform.runLater(
                    () -> statusCallback.accept(status));
        }
    }

    private int pendingCount() {
        try {
            return queueDAO.getPending().size();
        } catch (Exception e) {
            return 0;
        }
    }
}
