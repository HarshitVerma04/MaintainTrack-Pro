package com.maintaintrack.controllers;

import com.maintaintrack.dao.EquipmentDAO;
import com.maintaintrack.models.Equipment;
import com.maintaintrack.models.MaintenanceLog;
import com.maintaintrack.services.MaintenanceLogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MaintenanceController {

    // ── Table ─────────────────────────────────────────────────────────────
    @FXML private TableView<MaintenanceLog>             logTable;
    @FXML private TableColumn<MaintenanceLog,Integer>   colId;
    @FXML private TableColumn<MaintenanceLog,String>    colEquipment;
    @FXML private TableColumn<MaintenanceLog,LocalDate> colDoneOn;
    @FXML private TableColumn<MaintenanceLog,String>    colDoneBy;
    @FXML private TableColumn<MaintenanceLog,String>    colNotes;

    // ── Form ──────────────────────────────────────────────────────────────
    @FXML private javafx.scene.layout.VBox formPanel;
    @FXML private ComboBox<Equipment>      fieldEquipment;
    @FXML private DatePicker               fieldDoneOn;
    @FXML private TextField                fieldDoneBy;
    @FXML private TextField                fieldNotes;
    @FXML private Label                    nextDuePreview;
    @FXML private Label                    errorLabel;

    // ── State ─────────────────────────────────────────────────────────────
    private final MaintenanceLogService service      = new MaintenanceLogService();
    private final EquipmentDAO          equipmentDAO = new EquipmentDAO();
    private final ObservableList<MaintenanceLog> tableData =
            FXCollections.observableArrayList();

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        loadEquipmentDropdown();
        setupPreviewListeners();
        loadTable();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEquipment.setCellValueFactory(new PropertyValueFactory<>("equipmentName"));
        colDoneOn.setCellValueFactory(new PropertyValueFactory<>("doneOn"));
        colDoneBy.setCellValueFactory(new PropertyValueFactory<>("doneBy"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // ── Delete button column ──────────────────────────────────────────
        TableColumn<MaintenanceLog, Void> colDelete = new TableColumn<>("");
        colDelete.setPrefWidth(60);
        colDelete.setStyle("-fx-alignment: CENTER;");
        colDelete.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✕");
            {
                btn.setStyle(
                        "-fx-background-color: #c0392b; -fx-text-fill: white; " +
                                "-fx-font-size: 11px; -fx-padding: 2 8 2 8; " +
                                "-fx-background-radius: 4; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    MaintenanceLog log = getTableView().getItems().get(getIndex());
                    confirmAndDelete(log);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        logTable.getColumns().add(colDelete);
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

    private void setupPreviewListeners() {
        fieldEquipment.valueProperty().addListener((obs, old, newVal) -> updatePreview());
        fieldDoneOn.valueProperty().addListener((obs, old, newVal) -> updatePreview());
    }

    private void updatePreview() {
        Equipment e    = fieldEquipment.getValue();
        LocalDate done = fieldDoneOn.getValue();
        if (e != null && done != null) {
            LocalDate nextDue = done.plusDays(e.getIntervalDays());
            nextDuePreview.setText(nextDue.toString()
                    + "  (in " + e.getIntervalDays() + " days)");
        } else {
            nextDuePreview.setText("—");
        }
    }

    // ── Table load ────────────────────────────────────────────────────────

    private void loadTable() {
        try {
            tableData.setAll(service.getAllLogs());
        } catch (SQLException e) {
            showError("Failed to load logs: " + e.getMessage());
        }
    }

    // ── Add ───────────────────────────────────────────────────────────────

    @FXML
    private void onAdd() {
        clearForm();
        fieldDoneOn.setValue(LocalDate.now());
        showForm(true);
    }

    // ── Save ──────────────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        errorLabel.setText("");
        try {
            MaintenanceLog log = buildFromForm();
            service.logMaintenance(log);
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

    // ── Delete ────────────────────────────────────────────────────────────

    private void confirmAndDelete(MaintenanceLog log) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Maintenance Log");
        confirm.setHeaderText("Delete this log?");
        confirm.setContentText(
                log.getEquipmentName() + " — " + log.getDoneOn() +
                        (log.getNotes() != null && !log.getNotes().isBlank()
                                ? "\n" + log.getNotes() : ""));
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    service.deleteLog(log.getId());
                    loadTable();
                } catch (Exception e) {
                    showError("Delete failed: " + e.getMessage());
                }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private MaintenanceLog buildFromForm() {
        Equipment e    = fieldEquipment.getValue();
        LocalDate done = fieldDoneOn.getValue();
        String doneBy  = fieldDoneBy.getText().trim();
        String notes   = fieldNotes.getText().trim();

        MaintenanceLog log = new MaintenanceLog();
        log.setEquipmentId(e != null ? e.getId() : 0);
        log.setDoneOn(done);
        log.setDoneBy(doneBy.isBlank() ? null : doneBy);
        log.setNotes(notes.isBlank() ? null : notes);
        return log;
    }

    private void showForm(boolean v) {
        formPanel.setVisible(v);
        formPanel.setManaged(v);
    }

    private void clearForm() {
        fieldEquipment.setValue(null);
        fieldDoneOn.setValue(null);
        fieldDoneBy.clear();
        fieldNotes.clear();
        nextDuePreview.setText("—");
        errorLabel.setText("");
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
    }
}