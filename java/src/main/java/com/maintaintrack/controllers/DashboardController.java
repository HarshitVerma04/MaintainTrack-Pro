package com.maintaintrack.controllers;

import com.maintaintrack.models.Equipment;
import com.maintaintrack.models.Part;
import com.maintaintrack.services.DashboardService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * DashboardController — populates every widget on Dashboard.fxml.
 *
 * Load order:
 *  1. KPI tiles  (counts + totals)
 *  2. Alert feed (overdue equipment + low-stock parts)
 *  3. Cost per asset bar list
 *  4. Recent activity table
 */
public class DashboardController {

    // ── KPI tiles ─────────────────────────────────────────────────────────
    @FXML private Label kpiTotalEquipment;
    @FXML private Label kpiOperational;
    @FXML private Label kpiMaintenanceJobs;
    @FXML private Label kpiBreakdowns;
    @FXML private Label kpiPartsSpend;
    @FXML private Label kpiLowStockCount;
    @FXML private Label kpiAlerts;
    @FXML private Label kpiAlertsBreakdown;
    @FXML private Label lastRefreshed;

    // ── Alert feed ────────────────────────────────────────────────────────
    @FXML private VBox alertFeed;

    // ── Cost per asset ────────────────────────────────────────────────────
    @FXML private VBox costFeed;

    // ── Activity table ────────────────────────────────────────────────────
    @FXML private TableView<String[]>           activityTable;
    @FXML private TableColumn<String[],String>  colType;
    @FXML private TableColumn<String[],String>  colDate;
    @FXML private TableColumn<String[],String>  colSubject;
    @FXML private TableColumn<String[],String>  colDetail;
    @FXML private TableColumn<String[],String>  colActor;

    private final DashboardService service = new DashboardService();

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupActivityTable();
        loadAll();
    }

    @FXML
    private void onRefresh() {
        loadAll();
    }

    private void loadAll() {
        loadKpiTiles();
        loadAlertFeed();
        loadCostPerAsset();
        loadActivityFeed();
        lastRefreshed.setText("Last refreshed: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    // ── KPI Tiles ─────────────────────────────────────────────────────────

    private void loadKpiTiles() {
        try {
            int total        = service.getTotalEquipmentCount();
            int operational  = service.getOperationalCount();
            int jobs         = service.getTotalMaintenanceJobs();
            int breakdowns   = service.getTotalBreakdowns();
            double spend     = service.getTotalPartsSpend();
            int lowStock     = service.getLowStockParts().size();
            int overdue      = service.getOverdueEquipment().size();
            int totalAlerts  = lowStock + overdue;

            kpiTotalEquipment.setText(String.valueOf(total));
            kpiOperational.setText(operational + " operational");

            kpiMaintenanceJobs.setText(String.valueOf(jobs));
            kpiBreakdowns.setText(breakdowns + " breakdowns");

            kpiPartsSpend.setText(String.format("₹ %,.0f", spend));
            kpiLowStockCount.setText(lowStock + " low stock");

            kpiAlerts.setText(String.valueOf(totalAlerts));
            kpiAlertsBreakdown.setText(overdue + " overdue · " + lowStock + " low stock");

        } catch (SQLException e) {
            System.err.println("[Dashboard] KPI load failed: " + e.getMessage());
        }
    }

    // ── Alert Feed ────────────────────────────────────────────────────────

    private void loadAlertFeed() {
        alertFeed.getChildren().clear();
        try {
            // Overdue equipment
            List<Equipment> overdue = service.getOverdueEquipment();
            if (!overdue.isEmpty()) {
                alertFeed.getChildren().add(sectionLabel("⚙  Overdue Maintenance"));
                for (Equipment e : overdue) {
                    alertFeed.getChildren().add(alertRow(
                            "danger",
                            e.getName(),
                            "Due: " + e.getNextMaintenanceDate()
                                    + "  (" + e.getLocation() + ")"
                    ));
                }
            }

            // Low stock parts
            List<Part> lowStock = service.getLowStockParts();
            if (!lowStock.isEmpty()) {
                alertFeed.getChildren().add(sectionLabel("🔩  Low Stock Parts"));
                for (Part p : lowStock) {
                    alertFeed.getChildren().add(alertRow(
                            "warning",
                            p.getName(),
                            "Qty: " + p.getQtyOnHand() + " / Min: " + p.getMinQty()
                                    + "  (" + p.getUnit() + ")"
                    ));
                }
            }

            if (overdue.isEmpty() && lowStock.isEmpty()) {
                Label ok = new Label("✓  No active alerts — all systems nominal.");
                ok.setStyle("-fx-text-fill: #1a7a4a; -fx-font-size: 13px; -fx-padding: 8 0 0 0;");
                alertFeed.getChildren().add(ok);
            }

        } catch (SQLException e) {
            System.err.println("[Dashboard] Alert feed failed: " + e.getMessage());
        }
    }

    // ── Cost Per Asset ────────────────────────────────────────────────────

    private void loadCostPerAsset() {
        costFeed.getChildren().clear();
        try {
            Map<String, Double> costMap = service.getCostPerAsset();
            if (costMap.isEmpty()) {
                costFeed.getChildren().add(new Label("No cost data yet."));
                return;
            }

            // Find max value for bar scaling
            double max = costMap.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);

            for (Map.Entry<String, Double> entry : costMap.entrySet()) {
                String name  = entry.getKey();
                double cost  = entry.getValue();
                double ratio = cost / max;

                // Name + cost label
                HBox header = new HBox();
                header.setSpacing(8);
                Label nameLbl = new Label(name);
                nameLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #1b3a6b;");
                nameLbl.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(nameLbl, Priority.ALWAYS);
                Label costLbl = new Label(String.format("₹ %,.0f", cost));
                costLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1b3a6b;");
                header.getChildren().addAll(nameLbl, costLbl);

                // Bar
                HBox barBg = new HBox();
                barBg.setStyle("-fx-background-color: #e8eef5; -fx-background-radius: 4;");
                barBg.setPrefHeight(8);
                barBg.setMaxWidth(Double.MAX_VALUE);

                Region bar = new Region();
                bar.setStyle("-fx-background-color: #2e86de; -fx-background-radius: 4;");
                bar.setPrefHeight(8);
                bar.setPrefWidth(ratio * 260);
                barBg.getChildren().add(bar);

                VBox row = new VBox(4, header, barBg);
                costFeed.getChildren().add(row);
            }
        } catch (SQLException e) {
            System.err.println("[Dashboard] Cost feed failed: " + e.getMessage());
        }
    }

    // ── Activity Table ────────────────────────────────────────────────────

    private void setupActivityTable() {
        colType.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colDate.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        colSubject.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));
        colDetail.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colActor.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[4]));

        // Colour-code the Type column
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) { setText(null); setStyle(""); return; }
                setText(type);
                setStyle(switch (type) {
                    case "Maintenance"   -> "-fx-text-fill: #1a7a4a; -fx-font-weight: bold;";
                    case "Breakdown"     -> "-fx-text-fill: #8b1a1a; -fx-font-weight: bold;";
                    case "Part Issued"   -> "-fx-text-fill: #b85c00; -fx-font-weight: bold;";
                    case "Part Returned" -> "-fx-text-fill: #2e86de; -fx-font-weight: bold;";
                    default              -> "";
                });
            }
        });
    }

    private void loadActivityFeed() {
        try {
            List<String[]> activity = service.getRecentActivity(15);
            ObservableList<String[]> data = FXCollections.observableArrayList(activity);
            activityTable.setItems(data);
        } catch (SQLException e) {
            System.err.println("[Dashboard] Activity feed failed: " + e.getMessage());
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; "
                + "-fx-text-fill: #5c6b8a; -fx-padding: 6 0 2 0;");
        return lbl;
    }

    private HBox alertRow(String type, String title, String subtitle) {
        String bg     = type.equals("danger") ? "#fdecea" : "#fff3cd";
        String border = type.equals("danger") ? "#c0392b" : "#b85c00";
        String fg     = type.equals("danger") ? "#8b1a1a" : "#b85c00";

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " + fg + ";");

        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + fg + "; -fx-opacity: 0.85;");

        VBox content = new VBox(2, titleLbl, subLbl);

        HBox row = new HBox(content);
        row.setStyle("-fx-background-color: " + bg + "; "
                + "-fx-border-color: " + border + "; "
                + "-fx-border-radius: 6; -fx-background-radius: 6; "
                + "-fx-border-width: 0 0 0 4; "
                + "-fx-padding: 8 12 8 12;");
        HBox.setHgrow(content, Priority.ALWAYS);
        return row;
    }
}
