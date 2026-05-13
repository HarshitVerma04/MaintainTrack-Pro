package com.maintaintrack.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * MainLayoutController — manages the sidebar navigation.
 * Each nav button loads its FXML into the center StackPane,
 * replacing whatever was there before.
 */
public class MainLayoutController {

    @FXML private StackPane contentArea;

    // ── Initialise — show Equipment screen by default ─────────────────────
    @FXML
    public void initialize() {
        showEquipment();
    }

    // ── Nav handlers ──────────────────────────────────────────────────────

    @FXML private void showDashboard()   { loadView("/fxml/Dashboard.fxml"); }
    @FXML private void showEquipment()   { loadView("/fxml/Equipment.fxml"); }
    @FXML private void showParts()       { loadView("/fxml/Parts.fxml"); }
    @FXML private void showSuppliers()   { loadView("/fxml/Suppliers.fxml"); }
    @FXML private void showKpis()      { loadView("/fxml/Kpis.fxml"); }
    @FXML private void showMaintenance() { loadView("/fxml/Maintenance.fxml"); }
    @FXML private void showBreakdown()   { loadView("/fxml/Breakdown.fxml"); }
    @FXML private void showIssues()      { loadView("/fxml/Issues.fxml"); }

    // ── Loader helper ─────────────────────────────────────────────────────

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath)
            );
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("[NAV] Could not load: " + fxmlPath + " — " + e.getMessage());
        }
    }
}
