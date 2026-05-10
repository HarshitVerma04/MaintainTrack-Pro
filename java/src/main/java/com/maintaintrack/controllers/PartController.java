package com.maintaintrack.controllers;

import com.maintaintrack.dao.SupplierDAO;
import com.maintaintrack.models.Part;
import com.maintaintrack.models.Supplier;
import com.maintaintrack.services.PartService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * PartController — handles all user interactions on Parts.fxml.
 * Key difference from EquipmentController: supplier ComboBox is
 * populated from the DB, not a hardcoded list.
 */
public class PartController {

    // ── Table ─────────────────────────────────────────────────────────────
    @FXML private TableView<Part>              partsTable;
    @FXML private TableColumn<Part, Integer>   colId;
    @FXML private TableColumn<Part, String>    colName;
    @FXML private TableColumn<Part, String>    colSupplier;
    @FXML private TableColumn<Part, Integer>   colQty;
    @FXML private TableColumn<Part, Integer>   colMinQty;
    @FXML private TableColumn<Part, String>    colUnit;
    @FXML private TableColumn<Part, Double>    colUnitCost;
    @FXML private TableColumn<Part, String>    colStockStatus;

    // ── Form ──────────────────────────────────────────────────────────────
    @FXML private javafx.scene.layout.VBox formPanel;
    @FXML private Label                    formTitle;
    @FXML private TextField                fieldName;
    @FXML private ComboBox<Supplier>       fieldSupplier;
    @FXML private TextField                fieldQty;
    @FXML private TextField                fieldMinQty;
    @FXML private ComboBox<String>         fieldUnit;
    @FXML private TextField                fieldUnitCost;
    @FXML private Button                   btnDelete;
    @FXML private Label                    errorLabel;
    @FXML private Label                    lowStockBadge;

    // ── Search ────────────────────────────────────────────────────────────
    @FXML private TextField searchField;

    // ── State ─────────────────────────────────────────────────────────────
    private final PartService    service     = new PartService();
    private final SupplierDAO    supplierDAO = new SupplierDAO();
    private final ObservableList<Part> tableData = FXCollections.observableArrayList();
    private Part selectedPart = null;

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        setupFormOptions();
        setupRowClickListener();
        loadTable();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("qtyOnHand"));
        colMinQty.setCellValueFactory(new PropertyValueFactory<>("minQty"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colUnitCost.setCellValueFactory(new PropertyValueFactory<>("unitCost"));

        // Qty column — red when below min
        colQty.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty || qty == null) {
                    setText(null); setStyle("");
                } else {
                    setText(String.valueOf(qty));
                    Part part = getTableView().getItems().get(getIndex());
                    setStyle(part.isLowStock()
                            ? "-fx-text-fill: #8b1a1a; -fx-font-weight: bold;"
                            : "-fx-text-fill: #1a7a4a; -fx-font-weight: bold;");
                }
            }
        });

        // Stock status badge column
        colStockStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setStyle(""); return; }
                Part part = getTableView().getItems().get(getIndex());
                if (part.isLowStock()) {
                    setText("⚠ Low Stock");
                    setStyle("-fx-text-fill: #8b1a1a; -fx-font-weight: bold;");
                } else {
                    setText("✓ OK");
                    setStyle("-fx-text-fill: #1a7a4a; -fx-font-weight: bold;");
                }
            }
        });

        // Unit cost — formatted to 2 decimal places
        colUnitCost.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double cost, boolean empty) {
                super.updateItem(cost, empty);
                setText(empty || cost == null ? null : String.format("%.2f", cost));
            }
        });

        partsTable.setItems(tableData);
    }

    private void setupFormOptions() {
        // Unit options — editable ComboBox so user can type custom units
        fieldUnit.setItems(FXCollections.observableArrayList(
                "pcs", "kg", "g", "l", "ml", "m", "cm", "set", "can", "box", "roll"
        ));
        fieldUnit.setValue("pcs");

        // Supplier dropdown — loaded from DB
        loadSupplierDropdown();
    }

    private void loadSupplierDropdown() {
        try {
            List<Supplier> suppliers = supplierDAO.findAll();
            fieldSupplier.setItems(FXCollections.observableArrayList(suppliers));
            if (!suppliers.isEmpty()) fieldSupplier.setValue(suppliers.get(0));
        } catch (SQLException e) {
            showError("Could not load suppliers: " + e.getMessage());
        }
    }

    private void setupRowClickListener() {
        partsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> { if (newVal != null) populateForm(newVal); }
        );
    }

    // ── Table load ────────────────────────────────────────────────────────

    private void loadTable() {
        try {
            List<Part> data = searchField.getText().isBlank()
                    ? service.getAllParts()
                    : service.search(searchField.getText().trim());
            tableData.setAll(data);

            // Show/hide low stock badge
            long lowCount = data.stream().filter(Part::isLowStock).count();
            lowStockBadge.setText("⚠  " + lowCount + " Low Stock");
            lowStockBadge.setVisible(lowCount > 0);
            lowStockBadge.setManaged(lowCount > 0);

        } catch (SQLException e) {
            showError("Failed to load parts: " + e.getMessage());
        }
    }

    // ── Search ────────────────────────────────────────────────────────────

    @FXML private void onSearch() { loadTable(); }

    // ── Add ───────────────────────────────────────────────────────────────

    @FXML
    private void onAdd() {
        selectedPart = null;
        clearForm();
        formTitle.setText("Add Part");
        btnDelete.setVisible(false);
        showForm(true);
    }

    // ── Row click → Edit ──────────────────────────────────────────────────

    private void populateForm(Part p) {
        selectedPart = p;
        fieldName.setText(p.getName());
        fieldQty.setText(String.valueOf(p.getQtyOnHand()));
        fieldMinQty.setText(String.valueOf(p.getMinQty()));
        fieldUnit.setValue(p.getUnit());
        fieldUnitCost.setText(String.format("%.2f", p.getUnitCost()));
        formTitle.setText("Edit Part");
        btnDelete.setVisible(true);
        errorLabel.setText("");

        // Select matching supplier in dropdown
        fieldSupplier.getItems().stream()
                .filter(s -> s.getId() == p.getSupplierId())
                .findFirst()
                .ifPresent(s -> fieldSupplier.setValue(s));

        showForm(true);
    }

    // ── Save ──────────────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        errorLabel.setText("");
        try {
            Part p = buildFromForm();
            if (selectedPart == null) {
                service.addPart(p);
            } else {
                p.setId(selectedPart.getId());
                service.updatePart(p);
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
        if (selectedPart == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selectedPart.getName() + "\"? This cannot be undone.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.deletePart(selectedPart.getId());
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
        partsTable.getSelectionModel().clearSelection();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Part buildFromForm() {
        String name     = fieldName.getText().trim();
        String qtyStr   = fieldQty.getText().trim();
        String minStr   = fieldMinQty.getText().trim();
        String unit     = fieldUnit.getValue() != null
                ? fieldUnit.getValue().trim() : "";
        String costStr  = fieldUnitCost.getText().trim();
        Supplier supplier = fieldSupplier.getValue();

        if (name.isBlank())  throw new IllegalArgumentException("Part name is required.");
        if (unit.isBlank())  throw new IllegalArgumentException("Unit is required.");

        int qty, minQty;
        double cost = 0.0;

        try { qty = Integer.parseInt(qtyStr); if (qty < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Qty must be a non-negative whole number."); }

        try { minQty = Integer.parseInt(minStr); if (minQty < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Min Qty must be a non-negative whole number."); }

        if (!costStr.isBlank()) {
            try { cost = Double.parseDouble(costStr); if (cost < 0) throw new NumberFormatException(); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Unit cost must be a positive number."); }
        }

        Part p = new Part(0, supplier != null ? supplier.getId() : 0,
                name, qty, minQty, unit, cost);
        if (supplier != null) p.setSupplierName(supplier.getName());
        return p;
    }

    private void showForm(boolean v) { formPanel.setVisible(v); formPanel.setManaged(v); }

    private void clearForm() {
        fieldName.clear();
        fieldQty.clear();
        fieldMinQty.clear();
        fieldUnit.setValue("pcs");
        fieldUnitCost.clear();
        errorLabel.setText("");
        selectedPart = null;
        loadSupplierDropdown();
    }

    private void showError(String msg) { errorLabel.setText("⚠  " + msg); }
}
