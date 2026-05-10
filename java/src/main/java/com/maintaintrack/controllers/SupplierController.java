package com.maintaintrack.controllers;

import com.maintaintrack.models.Supplier;
import com.maintaintrack.services.SupplierService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * SupplierController — handles all user interactions on Suppliers.fxml.
 * Simplest of the three CRUD screens — no FK dropdowns or colour coding.
 */
public class SupplierController {

    // ── Table ─────────────────────────────────────────────────────────────
    @FXML private TableView<Supplier>           supplierTable;
    @FXML private TableColumn<Supplier,Integer> colId;
    @FXML private TableColumn<Supplier,String>  colName;
    @FXML private TableColumn<Supplier,String>  colContact;
    @FXML private TableColumn<Supplier,String>  colPhone;
    @FXML private TableColumn<Supplier,String>  colEmail;

    // ── Form ──────────────────────────────────────────────────────────────
    @FXML private javafx.scene.layout.VBox formPanel;
    @FXML private Label     formTitle;
    @FXML private TextField fieldName;
    @FXML private TextField fieldContact;
    @FXML private TextField fieldPhone;
    @FXML private TextField fieldEmail;
    @FXML private Button    btnDelete;
    @FXML private Label     errorLabel;

    // ── Search ────────────────────────────────────────────────────────────
    @FXML private TextField searchField;

    // ── State ─────────────────────────────────────────────────────────────
    private final SupplierService service = new SupplierService();
    private final ObservableList<Supplier> tableData = FXCollections.observableArrayList();
    private Supplier selectedSupplier = null;

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        setupRowClickListener();
        loadTable();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contactName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        supplierTable.setItems(tableData);
    }

    private void setupRowClickListener() {
        supplierTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> { if (newVal != null) populateForm(newVal); }
        );
    }

    // ── Table load ────────────────────────────────────────────────────────

    private void loadTable() {
        try {
            List<Supplier> data = service.getAllSuppliers();

            // Filter by search if text is present
            String kw = searchField.getText().trim().toLowerCase();
            if (!kw.isBlank()) {
                data = data.stream()
                        .filter(s -> (s.getName()        != null && s.getName().toLowerCase().contains(kw))
                                  || (s.getContactName() != null && s.getContactName().toLowerCase().contains(kw))
                                  || (s.getEmail()       != null && s.getEmail().toLowerCase().contains(kw)))
                        .toList();
            }

            tableData.setAll(data);
        } catch (SQLException e) {
            showError("Failed to load suppliers: " + e.getMessage());
        }
    }

    // ── Search ────────────────────────────────────────────────────────────

    @FXML private void onSearch() { loadTable(); }

    // ── Add ───────────────────────────────────────────────────────────────

    @FXML
    private void onAdd() {
        selectedSupplier = null;
        clearForm();
        formTitle.setText("Add Supplier");
        btnDelete.setVisible(false);
        showForm(true);
    }

    // ── Row click → Edit ──────────────────────────────────────────────────

    private void populateForm(Supplier s) {
        selectedSupplier = s;
        fieldName.setText(s.getName() != null ? s.getName() : "");
        fieldContact.setText(s.getContactName() != null ? s.getContactName() : "");
        fieldPhone.setText(s.getPhone() != null ? s.getPhone() : "");
        fieldEmail.setText(s.getEmail() != null ? s.getEmail() : "");
        formTitle.setText("Edit Supplier");
        btnDelete.setVisible(true);
        errorLabel.setText("");
        showForm(true);
    }

    // ── Save ──────────────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        errorLabel.setText("");
        try {
            Supplier s = buildFromForm();
            if (selectedSupplier == null) {
                service.addSupplier(s);
            } else {
                s.setId(selectedSupplier.getId());
                service.updateSupplier(s);
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
        if (selectedSupplier == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selectedSupplier.getName() + "\"?\n"
                + "Note: Parts linked to this supplier will lose their supplier reference.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.deleteSupplier(selectedSupplier.getId());
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
        supplierTable.getSelectionModel().clearSelection();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Supplier buildFromForm() {
        String name    = fieldName.getText().trim();
        String contact = fieldContact.getText().trim();
        String phone   = fieldPhone.getText().trim();
        String email   = fieldEmail.getText().trim();

        if (name.isBlank())
            throw new IllegalArgumentException("Supplier name is required.");

        return new Supplier(0, name,
                contact.isBlank() ? null : contact,
                phone.isBlank()   ? null : phone,
                email.isBlank()   ? null : email);
    }

    private void showForm(boolean v) {
        formPanel.setVisible(v);
        formPanel.setManaged(v);
    }

    private void clearForm() {
        fieldName.clear();
        fieldContact.clear();
        fieldPhone.clear();
        fieldEmail.clear();
        errorLabel.setText("");
        selectedSupplier = null;
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
    }
}
