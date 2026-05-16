package com.maintaintrack.controllers;

import com.maintaintrack.dao.EquipmentDAO;
import com.maintaintrack.dao.PartDAO;
import com.maintaintrack.models.Alert;
import com.maintaintrack.models.Equipment;
import com.maintaintrack.models.IssueRecord;
import com.maintaintrack.models.Part;
import com.maintaintrack.services.AlertService;
import com.maintaintrack.services.IssueRecordService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * IssueController — handles the Issues &amp; Alerts screen (Issues.fxml).
 *
 * Two responsibilities on one screen:
 *
 *  1. Issue / Return form (Days 10-12)
 *     Records part transactions with fully transactional stock updates.
 *     Stock preview updates live when a part is selected.
 *
 *  2. Alert feed panel (Day 16)
 *     Right-hand panel showing colour-coded active alerts from AlertService.
 *     Refreshed on init, on every save, and by MainLayoutController's
 *     polling callback — so it self-clears when conditions are fixed (Day 19).
 */
public class IssueController {

    // ── Issue/Return table ────────────────────────────────────────────────
    @FXML private TableView<IssueRecord>             issueTable;
    @FXML private TableColumn<IssueRecord, Integer>  colId;
    @FXML private TableColumn<IssueRecord, String>   colPart;
    @FXML private TableColumn<IssueRecord, String>   colEquipment;
    @FXML private TableColumn<IssueRecord, LocalDate>colIssuedOn;
    @FXML private TableColumn<IssueRecord, Integer>  colQty;
    @FXML private TableColumn<IssueRecord, String>   colIssuedBy;
    @FXML private TableColumn<IssueRecord, String>   colType;

    // ── Form ──────────────────────────────────────────────────────────────
    @FXML private VBox               formPanel;
    @FXML private ComboBox<Part>     fieldPart;
    @FXML private ComboBox<Equipment>fieldEquipment;
    @FXML private DatePicker         fieldIssuedOn;
    @FXML private TextField          fieldQty;
    @FXML private TextField          fieldIssuedBy;
    @FXML private RadioButton        radioIssue;
    @FXML private RadioButton        radioReturn;
    @FXML private Label              stockPreview;
    @FXML private Label              errorLabel;

    // ── Alert feed (Day 16) ───────────────────────────────────────────────
    @FXML private Label              alertCountLabel;
    @FXML private ListView<Alert>    alertListView;

    // ── Header badges ─────────────────────────────────────────────────────
    @FXML private Label              overdueBadge;
    @FXML private Label              lowStockBadge;

    // ── State ─────────────────────────────────────────────────────────────
    private final IssueRecordService service      = new IssueRecordService();
    private final AlertService       alertService = new AlertService();
    private final EquipmentDAO       equipmentDAO = new EquipmentDAO();
    private final PartDAO            partDAO      = new PartDAO();
    private final ObservableList<IssueRecord> tableData = FXCollections.observableArrayList();

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        loadDropdowns();
        setupPartPreview();
        loadTable();
        refreshAlerts();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPart.setCellValueFactory(new PropertyValueFactory<>("partName"));
        colEquipment.setCellValueFactory(new PropertyValueFactory<>("equipmentName"));
        colIssuedOn.setCellValueFactory(new PropertyValueFactory<>("issuedOn"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colIssuedBy.setCellValueFactory(new PropertyValueFactory<>("issuedBy"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeDisplay"));

        // Colour-code Type: Issue = red, Return = green
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                setStyle("Issue".equals(val)
                        ? "-fx-text-fill: #8b1a1a; -fx-font-weight: bold;"
                        : "-fx-text-fill: #1a7a4a; -fx-font-weight: bold;");
            }
        });

        issueTable.setItems(tableData);
    }

    private void loadDropdowns() {
        try {
            fieldPart.setItems(FXCollections.observableArrayList(partDAO.findAll()));
            fieldEquipment.setItems(FXCollections.observableArrayList(equipmentDAO.findAll()));
        } catch (SQLException e) {
            showError("Could not load dropdowns: " + e.getMessage());
        }
    }

    /** Live stock preview updates whenever the user picks a different part. */
    private void setupPartPreview() {
        fieldPart.valueProperty().addListener((obs, old, part) -> {
            if (part != null) {
                stockPreview.setText("Current stock: " + part.getQtyOnHand()
                        + " " + part.getUnit()
                        + (part.isLowStock() ? "  ⚠ Low" : "  ✓ OK"));
                stockPreview.setStyle(part.isLowStock()
                        ? "-fx-text-fill: #8b1a1a; -fx-font-weight: bold;"
                        : "-fx-text-fill: #1a7a4a;");
            } else {
                stockPreview.setText("—");
                stockPreview.setStyle("");
            }
        });
    }

    // ── Table load ────────────────────────────────────────────────────────

    private void loadTable() {
        try {
            tableData.setAll(service.getAllRecords());
        } catch (SQLException e) {
            showError("Failed to load records: " + e.getMessage());
        }
    }

    // ── Day 16/19: Alert feed ─────────────────────────────────────────────

    /**
     * Refreshes the alert feed from the live database.
     * Public so MainLayoutController can call it on each polling cycle.
     * Because AlertService always re-queries the DB, alerts self-clear
     * the moment the underlying condition is fixed (Day 19).
     */
    public void refreshAlerts() {
        try {
            List<Alert> alerts = alertService.getActiveAlerts();
            alertListView.setItems(FXCollections.observableArrayList(alerts));
            alertListView.setCellFactory(lv -> new AlertCell());

            int total    = alerts.size();
            int overdue  = (int) alerts.stream()
                    .filter(a -> a.getType() == Alert.Type.OVERDUE_MAINTENANCE).count();
            int lowStock = (int) alerts.stream()
                    .filter(a -> a.getType() == Alert.Type.LOW_STOCK).count();

            alertCountLabel.setText(total + " active alert" + (total == 1 ? "" : "s"));
            alertCountLabel.setStyle(total > 0
                    ? "-fx-text-fill: #8b1a1a; -fx-font-weight: bold;"
                    : "-fx-text-fill: #1a7a4a; -fx-font-weight: bold;");

            setBadge(overdueBadge,  overdue,  "Overdue");
            setBadge(lowStockBadge, lowStock, "Low Stock");

        } catch (SQLException e) {
            alertCountLabel.setText("⚠ Could not load alerts");
        }
    }

    private void setBadge(Label badge, int count, String label) {
        badge.setText("⚠  " + count + " " + label);
        badge.setVisible(count > 0);
        badge.setManaged(count > 0);
    }

    // ── Add ───────────────────────────────────────────────────────────────

    @FXML
    private void onAdd() {
        clearForm();
        fieldIssuedOn.setValue(LocalDate.now());
        radioIssue.setSelected(true);
        showForm(true);
    }

    // ── Save ──────────────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        errorLabel.setText("");
        try {
            IssueRecord record = buildFromForm();
            service.recordTransaction(record);
            loadTable();
            loadDropdowns(); // refresh qty counts in part dropdown
            showForm(false);
            clearForm();
            refreshAlerts(); // Day 19: recheck immediately after save
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

    private IssueRecord buildFromForm() {
        Part      part  = fieldPart.getValue();
        Equipment equip = fieldEquipment.getValue();
        String    qtyStr   = fieldQty.getText().trim();
        String    issuedBy = fieldIssuedBy.getText().trim();
        String    type     = radioReturn.isSelected() ? "return" : "issue";

        IssueRecord r = new IssueRecord();
        r.setPartId(part  != null ? part.getId()  : 0);
        r.setEquipmentId(equip != null ? equip.getId() : 0);
        r.setIssuedOn(fieldIssuedOn.getValue());
        r.setIssuedBy(issuedBy.isBlank() ? null : issuedBy);
        r.setType(type);

        if (qtyStr.isBlank()) throw new IllegalArgumentException("Quantity is required.");
        try {
            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0) throw new NumberFormatException();
            r.setQty(qty);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantity must be a positive whole number.");
        }
        return r;
    }

    private void showForm(boolean v) { formPanel.setVisible(v); formPanel.setManaged(v); }

    private void clearForm() {
        fieldPart.setValue(null);
        fieldEquipment.setValue(null);
        fieldIssuedOn.setValue(null);
        fieldQty.clear();
        fieldIssuedBy.clear();
        stockPreview.setText("—");
        stockPreview.setStyle("");
        errorLabel.setText("");
    }

    private void showError(String msg) { errorLabel.setText("⚠  " + msg); }

    // ── Alert list cell — Day 16: colour-coded by severity ───────────────

    private static class AlertCell extends ListCell<Alert> {
        @Override
        protected void updateItem(Alert alert, boolean empty) {
            super.updateItem(alert, empty);
            if (empty || alert == null) {
                setText(null);
                setStyle("-fx-padding: 0;");
                return;
            }
            String line1 = alert.getIcon() + "  " + alert.getTitle();
            String line2 = "    " + alert.getDetail()
                         + "  [" + alert.getTimestampFormatted() + "]";
            setText(line1 + "\n" + line2);
            setStyle(alert.getSeverityStyle() + " -fx-padding: 8 12 8 12;");
        }
    }
}
