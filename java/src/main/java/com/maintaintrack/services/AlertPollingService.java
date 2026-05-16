package com.maintaintrack.services;

import com.maintaintrack.models.Alert;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * AlertPollingService — background polling on a timer.
 *
 * Day 17: Uses ScheduledExecutorService to run AlertService every
 *         POLL_INTERVAL_SECONDS without blocking the JavaFX UI thread.
 *
 * Day 18: Pushes results back to the UI via Platform.runLater() so
 *         JavaFX controls can be updated safely from a non-UI thread.
 *
 * Usage (in MainLayoutController):
 *
 *   poller = new AlertPollingService();
 *   poller.start(alerts -> {
 *       updateSidebarBadges(alerts);
 *       updateAlertFeed(alerts);
 *   });
 *
 *   // On app close:
 *   poller.stop();
 *
 * The daemon thread means the JVM won't hang on exit even if stop()
 * isn't called, but always call stop() for clean shutdown.
 */
public class AlertPollingService {

    private static final int POLL_INTERVAL_SECONDS = 30;

    private final AlertService              alertService = new AlertService();
    private       ScheduledExecutorService  executor;
    private       ScheduledFuture<?>        future;

    /**
     * Starts the background polling loop.
     *
     * @param onAlertsUpdated callback invoked on the JavaFX UI thread
     *                        every time a fresh alert list is ready.
     *                        Receives an immutable snapshot of current alerts.
     */
    public void start(Consumer<List<Alert>> onAlertsUpdated) {
        if (executor != null && !executor.isShutdown()) return; // already running

        // Daemon thread so the JVM exits cleanly without an explicit stop()
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "alert-poller");
            t.setDaemon(true);
            return t;
        });

        Runnable pollTask = () -> {
            try {
                List<Alert> alerts = alertService.getActiveAlerts();
                // Day 18: hand back to the JavaFX thread safely
                Platform.runLater(() -> onAlertsUpdated.accept(alerts));
            } catch (SQLException e) {
                System.err.println("[AlertPoller] DB error: " + e.getMessage());
            }
        };

        // Run immediately (delay=0), then every POLL_INTERVAL_SECONDS
        future = executor.scheduleAtFixedRate(
                pollTask,
                0,
                POLL_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        System.out.println("[AlertPoller] Started — polling every "
                + POLL_INTERVAL_SECONDS + "s.");
    }

    /**
     * Triggers an immediate out-of-schedule poll.
     * Call this after saving a maintenance log, issuing a part, etc.,
     * so the UI reflects the change instantly without waiting for the timer.
     */
    public void pollNow(Consumer<List<Alert>> onAlertsUpdated) {
        if (executor == null || executor.isShutdown()) return;
        executor.submit(() -> {
            try {
                List<Alert> alerts = alertService.getActiveAlerts();
                Platform.runLater(() -> onAlertsUpdated.accept(alerts));
            } catch (SQLException e) {
                System.err.println("[AlertPoller] pollNow error: " + e.getMessage());
            }
        });
    }

    /** Stops the background polling thread gracefully. */
    public void stop() {
        if (future != null)   future.cancel(false);
        if (executor != null) executor.shutdownNow();
        System.out.println("[AlertPoller] Stopped.");
    }

    public boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }
}
