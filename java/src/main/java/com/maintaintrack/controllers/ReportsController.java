package com.maintaintrack.controllers;

import com.maintaintrack.dao.EquipmentDAO;
import com.maintaintrack.models.Equipment;
import com.maintaintrack.services.ReportService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.List;

/**
 * ReportsController — drives the Reports and Exports screen.
 * PDF and Excel both run on background threads so the UI never freezes.
 */
public class ReportsController {

    @FXML private ComboBox<Equipment> pdfEquipmentPicker;
    @FXML private Label               pdfStatusLabel;
    @FXML private Label               excelStatusLabel;

    private final ReportService reportService = new ReportService();
    private final EquipmentDAO  equipmentDAO  = new EquipmentDAO();

    @FXML
    public void initialize() {
        try {
            List<Equipment> equipment = equipmentDAO.findAll();
            pdfEquipmentPicker.setItems(FXCollections.observableArrayList(equipment));
            if (!equipment.isEmpty()) pdfEquipmentPicker.setValue(equipment.get(0));
        } catch (SQLException e) {
            pdfStatusLabel.setText("Could not load equipment list.");
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────

    @FXML
    private void onGeneratePdf() {
        Equipment selected = pdfEquipmentPicker.getValue();
        if (selected == null) {
            pdfStatusLabel.setStyle("-fx-text-fill:#8b1a1a;");
            pdfStatusLabel.setText("Please select an equipment first.");
            return;
        }
        pdfStatusLabel.setStyle("-fx-text-fill:#b85c00;");
        pdfStatusLabel.setText("Generating PDF for \"" + selected.getName() + "\"...");

        new Thread(() -> {
            try {
                String path = reportService.generateMaintenanceReport(
                        selected.getId(), selected.getName());
                Platform.runLater(() -> {
                    pdfStatusLabel.setStyle("-fx-text-fill:#1a7a4a; -fx-font-weight:bold;");
                    pdfStatusLabel.setText("Report saved: " + path);
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                            "Report generated.\nOpen it now?",
                            ButtonType.YES, ButtonType.NO);
                    a.setTitle("PDF Ready");
                    a.setHeaderText(selected.getName());
                    a.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.YES) {
                            try { reportService.openFile(path); }
                            catch (Exception ex) {
                                pdfStatusLabel.setText("Could not open file: " + ex.getMessage());
                            }
                        }
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    pdfStatusLabel.setStyle("-fx-text-fill:#8b1a1a;");
                    pdfStatusLabel.setText("Failed: " + e.getMessage());
                });
            }
        }, "pdf-generator").start();
    }

    // ── Excel ─────────────────────────────────────────────────────────────

    @FXML
    private void onExportExcel() {
        excelStatusLabel.setStyle("-fx-text-fill:#b85c00;");
        excelStatusLabel.setText("Generating Excel export...");

        new Thread(() -> {
            try {
                String python = System.getProperty("os.name")
                        .toLowerCase().contains("win") ? "python" : "python3";
                ProcessBuilder pb = new ProcessBuilder(
                        python, "scripts/export_parts.py");
                pb.directory(new java.io.File(System.getProperty("user.dir")));
                pb.redirectErrorStream(true);

                Process process = pb.start();
                StringBuilder out = new StringBuilder();
                try (java.io.BufferedReader r = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        out.append(line).append("\n");
                        System.out.println("[Python] " + line);
                    }
                }

                int code = process.waitFor();
                String text = out.toString().trim();

                Platform.runLater(() -> {
                    if (code == 0) {
                        excelStatusLabel.setStyle("-fx-text-fill:#1a7a4a; -fx-font-weight:bold;");
                        String path = text.contains("->")
                                ? text.substring(text.lastIndexOf("->") + 2).trim()
                                : "reports/parts_export.xlsx";
                        excelStatusLabel.setText("Saved: " + path);
                        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                                "Excel export ready.\nOpen it now?",
                                ButtonType.YES, ButtonType.NO);
                        a.setTitle("Excel Ready");
                        a.setHeaderText("Parts Usage Export");
                        a.showAndWait().ifPresent(btn -> {
                            if (btn == ButtonType.YES) {
                                try { reportService.openFile(path); }
                                catch (Exception ex) { /* ignore */ }
                            }
                        });
                    } else {
                        excelStatusLabel.setStyle("-fx-text-fill:#8b1a1a;");
                        excelStatusLabel.setText("Export failed. Check console.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    excelStatusLabel.setStyle("-fx-text-fill:#8b1a1a;");
                    excelStatusLabel.setText("Error: " + e.getMessage());
                });
            }
        }, "excel-exporter").start();
    }
}
