package com.maintaintrack.controllers;

import com.maintaintrack.models.Alert;
import com.maintaintrack.services.AlertPollingService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;
import com.maintaintrack.auth.AuthContext;
import com.maintaintrack.auth.TokenRefreshService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;
import javafx.scene.control.Button;

import java.io.IOException;
import java.util.List;

/**
 * MainLayoutController — sidebar navigation + alert polling lifecycle.
 *
 * Day 17/18: Starts the AlertPollingService background thread on init.
 *            Receives fresh Alert lists on the JavaFX thread via
 *            Platform.runLater() and updates sidebar badges.
 *
 * Day 16:    Sidebar shows live overdue / low-stock badge counts
 *            that update every poll cycle without any user interaction.
 *
 * Day 19:    Because AlertService re-queries the DB every cycle,
 *            badges clear automatically when issues are resolved.
 */
public class MainLayoutController {

    @FXML private StackPane contentArea;

    // ── Sidebar alert badges (Day 16) ─────────────────────────────────────
    @FXML private Label sidebarOverdueBadge;
    @FXML private Label sidebarLowStockBadge;

    @FXML private Label  loggedInLabel;
    @FXML private Button btnLogout;

    // ── Background poller (Day 17) ────────────────────────────────────────
    private final AlertPollingService poller = new AlertPollingService();

    // Track which controller is currently loaded
    private Object currentController = null;

    // ── Initialise ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        showEquipment();
        startAlertPoller();

        AuthContext auth = AuthContext.getInstance();
        if (auth.isLoggedIn()) {
            loggedInLabel.setText("👤  " + auth.getUsername()
                    + " (" + auth.getRole() + ")");
        } else {
            loggedInLabel.setText("👤  Offline");
        }
    }

    private void startAlertPoller() {
        poller.start(this::onAlertsReceived);
    }

    /**
     * Called on the JavaFX thread (via Platform.runLater) every poll cycle.
     * Day 18: safe to update UI controls here directly.
     */
    private void onAlertsReceived(List<Alert> alerts) {
        int overdue  = (int) alerts.stream()
                .filter(a -> a.getType() == Alert.Type.OVERDUE_MAINTENANCE).count();
        int lowStock = (int) alerts.stream()
                .filter(a -> a.getType() == Alert.Type.LOW_STOCK).count();

        updateBadge(sidebarOverdueBadge,  overdue);
        updateBadge(sidebarLowStockBadge, lowStock);

        // If the Issues screen is open, push fresh alerts to it
        if (currentController instanceof IssueController ic) {
            ic.refreshAlerts();
        }
    }

    private void updateBadge(Label badge, int count) {
        if (badge == null) return;
        badge.setText(String.valueOf(count));
        badge.setVisible(count > 0);
        badge.setManaged(count > 0);
    }

    /** Called from MainApp on window close to stop the daemon thread cleanly. */
    public void onWindowClose(WindowEvent e) {
        poller.stop();
    }

    // ── Nav handlers ──────────────────────────────────────────────────────

    @FXML private void showDashboard()   { loadView("/fxml/Dashboard.fxml"); }
    @FXML private void showEquipment()   { loadView("/fxml/Equipment.fxml"); }
    @FXML private void showParts()       { loadView("/fxml/Parts.fxml"); }
    @FXML private void showSuppliers()   { loadView("/fxml/Suppliers.fxml"); }
    @FXML private void showKpis()      { loadView("/fxml/Kpis.fxml"); }
    @FXML private void showActivity()   { loadView("/fxml/Activity.fxml"); }
    @FXML private void showReports()    { loadView("/fxml/Reports.fxml"); }
    @FXML private void showMaintenance() { loadView("/fxml/Maintenance.fxml"); }
    @FXML private void showBreakdown()   { loadView("/fxml/Breakdown.fxml"); }
    @FXML private void showIssues()      { loadView("/fxml/Issues.fxml"); }

    // ── View loader ───────────────────────────────────────────────────────

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            currentController = loader.getController();
            contentArea.getChildren().setAll(view);

            // Trigger an immediate out-of-schedule poll on every nav
            poller.pollNow(this::onAlertsReceived);

        } catch (IOException e) {
            System.err.println("[NAV] Could not load: " + fxmlPath + " — " + e.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        TokenRefreshService.getInstance().cancelPending();
        AuthContext.getInstance().clearSession();

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(loader.load(), 900, 600);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/styles/app.css")
                    ).toExternalForm());

            Stage stage = (Stage) btnLogout.getScene().getWindow();
            stage.setMinWidth(600);
            stage.setMinHeight(400);
            stage.setWidth(900);
            stage.setHeight(600);
            stage.setScene(scene);
            stage.setTitle("MaintainTrack Pro — Sign In");
        } catch (Exception e) {
            System.err.println("[Logout] Failed: " + e.getMessage());
        }
    }
}
