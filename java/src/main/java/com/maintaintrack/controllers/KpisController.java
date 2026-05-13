package com.maintaintrack.controllers;

import com.maintaintrack.services.DashboardService;
import com.maintaintrack.services.KpiService;
import com.maintaintrack.services.KpiService.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

/**
 * KpisController — drives the 3-tab KPIs screen.
 *
 * Tab 1: Uptime %     (Day 21)
 * Tab 2: MTBF         (Day 22)
 * Tab 3: Cost/Asset   (Day 23)
 */
public class KpisController {

    // ── Summary strip ─────────────────────────────────────────────────────
    @FXML private Label kpiFleetUptime;
    @FXML private Label kpiGoodMachines;
    @FXML private Label kpiBadMachines;
    @FXML private Label kpiTotalSpend;
    @FXML private Label periodLabel;

    // ── Uptime tab ────────────────────────────────────────────────────────
    @FXML private TableView<UptimeRecord>           uptimeTable;
    @FXML private TableColumn<UptimeRecord,String>  uColName;
    @FXML private TableColumn<UptimeRecord,String>  uColLocation;
    @FXML private TableColumn<UptimeRecord,String>  uColStatus;
    @FXML private TableColumn<UptimeRecord,String>  uColBreakdowns;
    @FXML private TableColumn<UptimeRecord,String>  uColDowntime;
    @FXML private TableColumn<UptimeRecord,String>  uColUptime;
    @FXML private TableColumn<UptimeRecord,String>  uColBar;

    // ── MTBF tab ──────────────────────────────────────────────────────────
    @FXML private TableView<MtbfRecord>             mtbfTable;
    @FXML private TableColumn<MtbfRecord,String>    mColName;
    @FXML private TableColumn<MtbfRecord,String>    mColLocation;
    @FXML private TableColumn<MtbfRecord,String>    mColBreakdowns;
    @FXML private TableColumn<MtbfRecord,String>    mColMaintenance;
    @FXML private TableColumn<MtbfRecord,String>    mColMtbf;
    @FXML private TableColumn<MtbfRecord,String>    mColRating;

    // ── Cost tab ──────────────────────────────────────────────────────────
    @FXML private TableView<CostRecord>             costTable;
    @FXML private TableColumn<CostRecord,String>    cColName;
    @FXML private TableColumn<CostRecord,String>    cColLocation;
    @FXML private TableColumn<CostRecord,String>    cColIssues;
    @FXML private TableColumn<CostRecord,String>    cColCost;
    @FXML private TableColumn<CostRecord,String>    cColPerDay;
    @FXML private TableColumn<CostRecord,String>    cColBar;

    private final KpiService       kpiService  = new KpiService();
    private final DashboardService dashService = new DashboardService();

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        periodLabel.setText("Analysis window: last "
                + KpiService.ANALYSIS_DAYS + " days");
        setupUptimeTable();
        setupMtbfTable();
        setupCostTable();
        loadAll();
    }

    @FXML
    private void onRefresh() { loadAll(); }

    private void loadAll() {
        loadSummaryStrip();
        loadUptimeTab();
        loadMtbfTab();
        loadCostTab();
    }

    // ── Summary strip ─────────────────────────────────────────────────────

    private void loadSummaryStrip() {
        try {
            List<UptimeRecord> records = kpiService.getUptimePerEquipment();
            double fleetAvg = kpiService.getFleetUptimeAverage();
            long good = records.stream().filter(r -> r.uptimePct >= 95).count();
            long bad  = records.stream().filter(r -> r.uptimePct <  80).count();
            double spend = dashService.getTotalPartsSpend();

            kpiFleetUptime.setText(String.format("%.1f%%", fleetAvg));
            kpiGoodMachines.setText(String.valueOf(good));
            kpiBadMachines.setText(String.valueOf(bad));
            kpiTotalSpend.setText(String.format("₹ %,.0f", spend));

        } catch (SQLException e) {
            System.err.println("[KPIs] Summary strip failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TAB 1 — UPTIME %
    // ══════════════════════════════════════════════════════════════════════

    private void setupUptimeTable() {
        uColName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().equipmentName));
        uColLocation.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().location));
        uColBreakdowns.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().breakdownCount)));
        uColDowntime.setCellValueFactory(d ->
                new SimpleStringProperty(
                        String.format("%.0f days", d.getValue().downtimeDays)));
        uColUptime.setCellValueFactory(d ->
                new SimpleStringProperty(
                        String.format("%.1f%%", d.getValue().uptimePct)));

        // Status — colour coded
        uColStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setText(null); setStyle(""); return; }
                UptimeRecord r = getTableView().getItems().get(getIndex());
                setText(r.status);
                setStyle(switch (r.status) {
                    case "Operational"       -> "-fx-text-fill:#1a7a4a; -fx-font-weight:bold;";
                    case "Under Maintenance" -> "-fx-text-fill:#b85c00; -fx-font-weight:bold;";
                    default                  -> "-fx-text-fill:#8b1a1a; -fx-font-weight:bold;";
                });
            }
        });

        // Uptime % — colour coded
        uColUptime.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setText(null); setStyle(""); return; }
                UptimeRecord r = getTableView().getItems().get(getIndex());
                setText(String.format("%.1f%%", r.uptimePct));
                setStyle(switch (r.getRating()) {
                    case "good"   -> "-fx-text-fill:#1a7a4a; -fx-font-weight:bold;";
                    case "warn"   -> "-fx-text-fill:#b85c00; -fx-font-weight:bold;";
                    default       -> "-fx-text-fill:#8b1a1a; -fx-font-weight:bold;";
                });
            }
        });

        // Visual bar column
        uColBar.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                UptimeRecord r = getTableView().getItems().get(getIndex());
                setGraphic(buildBar(r.uptimePct / 100.0, r.getRating(), 180));
            }
        });

        uptimeTable.setItems(FXCollections.observableArrayList());
    }

    private void loadUptimeTab() {
        try {
            uptimeTable.setItems(
                    FXCollections.observableArrayList(
                            kpiService.getUptimePerEquipment()));
        } catch (SQLException e) {
            System.err.println("[KPIs] Uptime tab failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TAB 2 — MTBF
    // ══════════════════════════════════════════════════════════════════════

    private void setupMtbfTable() {
        mColName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().equipmentName));
        mColLocation.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().location));
        mColBreakdowns.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().breakdownCount)));
        mColMaintenance.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().maintenanceCount)));
        mColMtbf.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().breakdownCount == 0
                                ? "No failures"
                                : String.format("%.1f days", d.getValue().mtbfDays)));

        // MTBF colour coding
        mColMtbf.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setText(null); setStyle(""); return; }
                MtbfRecord r = getTableView().getItems().get(getIndex());
                setText(r.breakdownCount == 0 ? "No failures ✓"
                        : String.format("%.1f days", r.mtbfDays));
                setStyle(switch (r.getRating()) {
                    case "good"  -> "-fx-text-fill:#1a7a4a; -fx-font-weight:bold;";
                    case "warn"  -> "-fx-text-fill:#b85c00; -fx-font-weight:bold;";
                    default      -> "-fx-text-fill:#8b1a1a; -fx-font-weight:bold;";
                });
            }
        });

        // Rating badge
        mColRating.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setText(null); setStyle(""); return; }
                MtbfRecord r = getTableView().getItems().get(getIndex());
                switch (r.getRating()) {
                    case "good" -> { setText("✓ Good");    setStyle("-fx-text-fill:#1a7a4a; -fx-font-weight:bold;"); }
                    case "warn" -> { setText("⚡ Fair");    setStyle("-fx-text-fill:#b85c00; -fx-font-weight:bold;"); }
                    default     -> { setText("⚠ Poor");   setStyle("-fx-text-fill:#8b1a1a; -fx-font-weight:bold;"); }
                }
            }
        });

        mtbfTable.setItems(FXCollections.observableArrayList());
    }

    private void loadMtbfTab() {
        try {
            mtbfTable.setItems(
                    FXCollections.observableArrayList(
                            kpiService.getMtbfPerEquipment()));
        } catch (SQLException e) {
            System.err.println("[KPIs] MTBF tab failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TAB 3 — COST PER ASSET
    // ══════════════════════════════════════════════════════════════════════

    private void setupCostTable() {
        cColName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().equipmentName));
        cColLocation.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().location));
        cColIssues.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().issueCount)));
        cColCost.setCellValueFactory(d ->
                new SimpleStringProperty(
                        String.format("₹ %,.2f", d.getValue().totalCost)));
        cColPerDay.setCellValueFactory(d ->
                new SimpleStringProperty(
                        String.format("₹ %,.2f", d.getValue().costPerDay)));

        // Visual bar — scaled to max cost
        cColBar.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                CostRecord r = getTableView().getItems().get(getIndex());
                try {
                    double max = kpiService.getMaxCost();
                    double ratio = max > 0 ? r.totalCost / max : 0;
                    setGraphic(buildBar(ratio, "cost", 200));
                } catch (SQLException e) {
                    setGraphic(null);
                }
            }
        });

        costTable.setItems(FXCollections.observableArrayList());
    }

    private void loadCostTab() {
        try {
            costTable.setItems(
                    FXCollections.observableArrayList(
                            kpiService.getCostPerEquipment()));
        } catch (SQLException e) {
            System.err.println("[KPIs] Cost tab failed: " + e.getMessage());
        }
    }

    // ── Shared UI helpers ─────────────────────────────────────────────────

    /**
     * Builds a simple proportional bar for table cells.
     * ratio = 0.0 → 1.0, type = "good" | "warn" | "danger" | "cost"
     */
    private HBox buildBar(double ratio, String type, double maxWidth) {
        String color = switch (type) {
            case "good"  -> "#1a7a4a";
            case "warn"  -> "#b85c00";
            case "danger"-> "#8b1a1a";
            default      -> "#2e86de";   // cost → blue
        };

        Region filled = new Region();
        filled.setPrefWidth(ratio * maxWidth);
        filled.setPrefHeight(10);
        filled.setStyle("-fx-background-color:" + color
                + "; -fx-background-radius:4;");

        Region empty = new Region();
        empty.setPrefHeight(10);
        HBox.setHgrow(empty, Priority.ALWAYS);
        empty.setStyle("-fx-background-color:#e8eef5; -fx-background-radius:4;");

        HBox bar = new HBox(filled, empty);
        bar.setPrefWidth(maxWidth);
        bar.setMaxWidth(maxWidth);
        bar.setStyle("-fx-alignment:CENTER_LEFT;");
        return bar;
    }
}
