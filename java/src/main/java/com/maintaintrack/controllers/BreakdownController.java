package com.maintaintrack.controllers;

import com.maintaintrack.dao.EquipmentDAO;
import com.maintaintrack.models.BreakdownLog;
import com.maintaintrack.models.Equipment;
import com.maintaintrack.services.BreakdownLogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * BreakdownController — handles the Breakdown Log screen.
 *
 * Key difference from MaintenanceController:
 * - No date recalculation
 * - resolvedBy is optional (breakdown may still be open)
 * - Description is required (must know what failed)
 * - Shows a total breakdown count badge in the header
 */
public class BreakdownController {

    // ── Table ─────────────────────────────────────────────────────────────
    @FXML private TableView<BreakdownLog>             logTable;
    @FXML private TableColumn<BreakdownLog,Integer>   colId;
    @FXML private TableColumn<BreakdownLog,String>    colEquipment;
    @FXML private TableColumn<BreakdownLog,LocalDate> colOccurredOn;
    @FXML private TableColumn<BreakdownLog,String>    colResolvedBy;
    @FXML private TableColumn<BreakdownLog,String>    colDescription;

    // ── Form ──────────────────────────────────────────────────────────────
    @FXML private javafx.scene.layout.VBox formPanel;
    @FXML private ComboBox<Equipment>      fieldEquipment;
    @FXML private DatePicker               fieldOccurredOn;
    @FXML private TextField                fieldResolvedBy;
    @FXML private TextField                fieldDescription;
    @FXML private Label                    errorLabel;

    // ── Header ────────────────────────────────────────────────────────────
    @FXML private Label totalBadge;

    // ── State ─────────────────────────────────────────────────────────────
    private final BreakdownLogService service      = new BreakdownLogService();
    private final EquipmentDAO        equipmentDAO = new EquipmentDAO();
    private final ObservableList<BreakdownLog> tableData = FXCollections.observableArrayList();

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        loadEquipmentDropdown();
        loadTable();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEquipment.setCellValueFactory(new PropertyValueFactory<>("equipmentName"));
        colOccurredOn.setCellValueFactory(new PropertyValueFactory<>("occurredOn"));
        colResolvedBy.setCellValueFactory(new PropertyValueFactory<>("resolvedBy"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Colour resolved by — grey if empty (unresolved)
        colResolvedBy.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty) { setText(null); setStyle(""); return; }
                if (val == null || val.isBlank()) {
                    setText("— Unresolved");
                    setStyle("-fx-text-fill: #8b1a1a; -fx-font-style: italic;");
                } else {
                    setText(val);
                    setStyle("-fx-text-fill: #1a7a4a;");
                }
            }
        });

        logTable.setItems(tableData);
    }

    private void loadEquipmentDropdown() {
        try {
            List<Equipment> equipment = equipmentDAO.findAll();
            fieldEquipment.setItems(FXCollections.observableArrayList(equipment));
        } catch (SQLException e) {
            showError("Could not load equipment: " + e.getMessage());
        }
    }

    // ── Table load ────────────────────────────────────────────────────────

    private void loadTable() {
        try {
            List<BreakdownLog> data = service.getAllLogs();
            tableData.setAll(data);

            // Update total badge
            int total = data.size();
            if (total > 0) {
                totalBadge.setText("⚠  " + total + " Total Breakdowns");
                totalBadge.setVisible(true);
                totalBadge.setManaged(true);
            } else {
                totalBadge.setVisible(false);
                totalBadge.setManaged(false);
            }
        } catch (SQLException e) {
            showError("Failed to load breakdown logs: " + e.getMessage());
        }
    }

    // ── Add ───────────────────────────────────────────────────────────────

    @FXML
    private void onAdd() {
        clearForm();
        fieldOccurredOn.setValue(LocalDate.now());
        showForm(true);
    }

    // ── Save ──────────────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        errorLabel.setText("");
        try {
            BreakdownLog log = buildFromForm();
            service.logBreakdown(log);
            loadTable();
            showForm(false);
            clearForm();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    // ── Cancel ────────────────────────────────────────────────────────────

    @FXML
    private void onCancel() {
        showForm(false);
        clearForm();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private BreakdownLog buildFromForm() {
        Equipment e       = fieldEquipment.getValue();
        LocalDate occurred = fieldOccurredOn.getValue();
        String resolvedBy  = fieldResolvedBy.getText().trim();
        String description = fieldDescription.getText().trim();

        BreakdownLog log = new BreakdownLog();
        log.setEquipmentId(e != null ? e.getId() : 0);
        log.setOccurredOn(occurred);
        log.setDescription(description.isBlank() ? null : description);
        log.setResolvedBy(resolvedBy.isBlank() ? null : resolvedBy);
        return log;
    }

    private void showForm(boolean v) {
        formPanel.setVisible(v);
        formPanel.setManaged(v);
    }

    private void clearForm() {
        fieldEquipment.setValue(null);
        fieldOccurredOn.setValue(null);
        fieldResolvedBy.clear();
        fieldDescription.clear();
        errorLabel.setText("");
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
    }
}
