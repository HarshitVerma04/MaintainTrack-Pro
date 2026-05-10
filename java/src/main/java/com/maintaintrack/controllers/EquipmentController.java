package com.maintaintrack.controllers;

import com.maintaintrack.models.Equipment;
import com.maintaintrack.services.EquipmentService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * EquipmentController — handles all user interactions on Equipment.fxml.
 * Pattern: user action → service call → refresh table.
 */
public class EquipmentController {

    // ── Table ─────────────────────────────────────────────────────────────
    @FXML private TableView<Equipment>       equipmentTable;
    @FXML private TableColumn<Equipment,Integer>   colId;
    @FXML private TableColumn<Equipment,String>    colName;
    @FXML private TableColumn<Equipment,String>    colLocation;
    @FXML private TableColumn<Equipment,String>    colStatus;
    @FXML private TableColumn<Equipment,LocalDate> colNextDate;
    @FXML private TableColumn<Equipment,Integer>   colInterval;

    // ── Form ──────────────────────────────────────────────────────────────
    @FXML private javafx.scene.layout.VBox formPanel;
    @FXML private Label      formTitle;
    @FXML private TextField  fieldName;
    @FXML private TextField  fieldLocation;
    @FXML private ComboBox<String> fieldStatus;
    @FXML private TextField  fieldInterval;
    @FXML private DatePicker fieldNextDate;
    @FXML private Button     btnDelete;
    @FXML private Label      errorLabel;

    // ── Search ────────────────────────────────────────────────────────────
    @FXML private TextField searchField;

    // ── State ─────────────────────────────────────────────────────────────
    private final EquipmentService service = new EquipmentService();
    private final ObservableList<Equipment> tableData = FXCollections.observableArrayList();
    private Equipment selectedEquipment = null; // null = Add mode, non-null = Edit mode

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        setupStatusOptions();
        setupRowClickListener();
        loadTable();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colNextDate.setCellValueFactory(new PropertyValueFactory<>("nextMaintenanceDate"));
        colInterval.setCellValueFactory(new PropertyValueFactory<>("intervalDays"));

        // Colour-code the Status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    setStyle(switch (status) {
                        case "Operational"       -> "-fx-text-fill: #1a7a4a; -fx-font-weight: bold;";
                        case "Under Maintenance" -> "-fx-text-fill: #b85c00; -fx-font-weight: bold;";
                        case "Out of Service"    -> "-fx-text-fill: #8b1a1a; -fx-font-weight: bold;";
                        default                  -> "";
                    });
                }
            }
        });

        // Colour-code Next Due — red if overdue
        colNextDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(date.toString());
                    setStyle(date.isBefore(LocalDate.now())
                            ? "-fx-text-fill: #8b1a1a; -fx-font-weight: bold;"
                            : "");
                }
            }
        });

        equipmentTable.setItems(tableData);
    }

    private void setupStatusOptions() {
        fieldStatus.setItems(FXCollections.observableArrayList(
                "Operational", "Under Maintenance", "Out of Service"
        ));
        fieldStatus.setValue("Operational");
    }

    private void setupRowClickListener() {
        equipmentTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) populateForm(newVal);
                }
        );
    }

    // ── Table load ────────────────────────────────────────────────────────

    private void loadTable() {
        try {
            List<Equipment> data = searchField.getText().isBlank()
                    ? service.getAllEquipment()
                    : service.search(searchField.getText().trim());
            tableData.setAll(data);
        } catch (SQLException e) {
            showError("Failed to load equipment: " + e.getMessage());
        }
    }

    // ── Search ────────────────────────────────────────────────────────────

    @FXML
    private void onSearch() {
        loadTable();
    }

    // ── Add button ────────────────────────────────────────────────────────

    @FXML
    private void onAdd() {
        selectedEquipment = null;
        clearForm();
        formTitle.setText("Add Equipment");
        btnDelete.setVisible(false);
        showForm(true);
    }

    // ── Row click → Edit mode ─────────────────────────────────────────────

    private void populateForm(Equipment e) {
        selectedEquipment = e;
        fieldName.setText(e.getName());
        fieldLocation.setText(e.getLocation() != null ? e.getLocation() : "");
        fieldStatus.setValue(e.getStatus() != null ? e.getStatus() : "Operational");
        fieldInterval.setText(String.valueOf(e.getIntervalDays()));
        fieldNextDate.setValue(e.getNextMaintenanceDate());
        formTitle.setText("Edit Equipment");
        btnDelete.setVisible(true);
        errorLabel.setText("");
        showForm(true);
    }

    // ── Save (Add or Update) ──────────────────────────────────────────────

    @FXML
    private void onSave() {
        errorLabel.setText("");
        try {
            Equipment e = buildFromForm();
            if (selectedEquipment == null) {
                service.addEquipment(e);
            } else {
                e.setId(selectedEquipment.getId());
                service.updateEquipment(e);
            }
            loadTable();
            showForm(false);
            clearForm();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────

    @FXML
    private void onDelete() {
        if (selectedEquipment == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selectedEquipment.getName() + "\"? This cannot be undone.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.deleteEquipment(selectedEquipment.getId());
                    loadTable();
                    showForm(false);
                    clearForm();
                } catch (SQLException e) {
                    showError("Delete failed: " + e.getMessage());
                }
            }
        });
    }

    // ── Cancel ────────────────────────────────────────────────────────────

    @FXML
    private void onCancel() {
        showForm(false);
        clearForm();
        equipmentTable.getSelectionModel().clearSelection();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Equipment buildFromForm() {
        String name     = fieldName.getText().trim();
        String location = fieldLocation.getText().trim();
        String status   = fieldStatus.getValue();
        String intervalStr = fieldInterval.getText().trim();
        LocalDate nextDate = fieldNextDate.getValue();

        if (name.isBlank())
            throw new IllegalArgumentException("Name is required.");
        if (intervalStr.isBlank())
            throw new IllegalArgumentException("Interval days is required.");

        int interval;
        try {
            interval = Integer.parseInt(intervalStr);
            if (interval <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Interval must be a positive whole number.");
        }

        return new Equipment(0, name, location, status, nextDate, interval);
    }

    private void showForm(boolean visible) {
        formPanel.setVisible(visible);
        formPanel.setManaged(visible);
    }

    private void clearForm() {
        fieldName.clear();
        fieldLocation.clear();
        fieldStatus.setValue("Operational");
        fieldInterval.clear();
        fieldNextDate.setValue(null);
        errorLabel.setText("");
        selectedEquipment = null;
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
    }
}
