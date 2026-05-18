package com.maintaintrack.controllers;

import com.maintaintrack.dao.BreakdownLogDAO;
import com.maintaintrack.dao.EquipmentDAO;
import com.maintaintrack.dao.PartDAO;
import com.maintaintrack.models.Alert;
import com.maintaintrack.models.BreakdownLog;
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
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * IssueController — handles the Issues and Work Orders screen.
 *
 * Two responsibilities:
 *
 * 1. Issue / Return form (Days 10-12)
 *    Transactional stock update. Live stock preview on part selection.
 *
 * 2. Day 8 — Work Order system
 *    Optional breakdown dropdown. When selected, the issue is linked to
 *    that breakdown incident. The work order cost preview shows how much
 *    has already been spent on parts for that breakdown.
 *
 * 3. Alert feed panel (Day 16)
 *    Right-hand panel, refreshed after every save and by the polling service.
 */
public class IssueController {

    // ── Table ─────────────────────────────────────────────────────────────
    @FXML private TableView<IssueRecord>             issueTable;
    @FXML private TableColumn<IssueRecord,Integer>   colId;
    @FXML private TableColumn<IssueRecord,String>    colPart;
    @FXML private TableColumn<IssueRecord,String>    colEquipment;
    @FXML private TableColumn<IssueRecord,LocalDate> colIssuedOn;
    @FXML private TableColumn<IssueRecord,Integer>   colQty;
    @FXML private TableColumn<IssueRecord,String>    colIssuedBy;
    @FXML private TableColumn<IssueRecord,String>    colType;
    @FXML private TableColumn<IssueRecord,String>    colBreakdown;

    // ── Form ──────────────────────────────────────────────────────────────
    @FXML private javafx.scene.layout.VBox formPanel;
    @FXML private ComboBox<Part>           fieldPart;
    @FXML private ComboBox<Equipment>      fieldEquipment;
    @FXML private DatePicker               fieldIssuedOn;
    @FXML private TextField                fieldQty;
    @FXML private TextField                fieldIssuedBy;
    @FXML private RadioButton              radioIssue;
    @FXML private RadioButton              radioReturn;
    @FXML private Label                    stockPreview;
    @FXML private Label                    errorLabel;

    // ── Day 8 — Work Order ────────────────────────────────────────────────
    @FXML private ComboBox<BreakdownLog>   fieldBreakdown;
    @FXML private HBox                     workOrderCostBox;
    @FXML private Label                    workOrderCostLabel;

    // ── Alert feed ────────────────────────────────────────────────────────
    @FXML private Label           alertCountLabel;
    @FXML private ListView<Alert> alertListView;

    // ── Header badges ─────────────────────────────────────────────────────
    @FXML private Label overdueBadge;
    @FXML private Label lowStockBadge;

    // ── State ─────────────────────────────────────────────────────────────
    private final IssueRecordService service      = new IssueRecordService();
    private final AlertService       alertService = new AlertService();
    private final EquipmentDAO       equipmentDAO = new EquipmentDAO();
    private final PartDAO            partDAO      = new PartDAO();
    private final BreakdownLogDAO    breakdownDAO = new BreakdownLogDAO();

    private final ObservableList<IssueRecord> tableData = FXCollections.observableArrayList();

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupColumns();
        setupToggleGroup();
        loadDropdowns();
        setupPartPreview();
        setupBreakdownPreview();
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

        // Type column — colour coded
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                setStyle("Issue".equals(val)
                        ? "-fx-text-fill:#8b1a1a; -fx-font-weight:bold;"
                        : "-fx-text-fill:#1a7a4a; -fx-font-weight:bold;");
            }
        });

        // Work order column — Day 8
        colBreakdown.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty) { setText(null); setStyle(""); return; }
                IssueRecord r = getTableView().getItems().get(getIndex());
                if (r.isWorkOrder()) {
                    String desc = r.getBreakdownDesc();
                    setText(desc != null && desc.length() > 28
                            ? desc.substring(0, 25) + "..." : desc);
                    setStyle("-fx-text-fill:#2e86de; -fx-font-style:italic;");
                } else {
                    setText("-- Standalone");
                    setStyle("-fx-text-fill:#5c6b8a;");
                }
            }
        });

        issueTable.setItems(tableData);
    }

    private void setupToggleGroup() {
        ToggleGroup group = new ToggleGroup();
        radioIssue.setToggleGroup(group);
        radioReturn.setToggleGroup(group);
        radioIssue.setSelected(true);
    }

    private void loadDropdowns() {
        try {
            fieldPart.setItems(FXCollections.observableArrayList(partDAO.findAll()));
            fieldEquipment.setItems(FXCollections.observableArrayList(equipmentDAO.findAll()));

            // Breakdown dropdown — Day 8
            // Show a blank "None" option first so user can leave it unlinked
            List<BreakdownLog> breakdowns = breakdownDAO.findAll();
            fieldBreakdown.setItems(FXCollections.observableArrayList(breakdowns));
            fieldBreakdown.setValue(null);
        } catch (SQLException e) {
            showError("Could not load dropdowns: " + e.getMessage());
        }
    }

    // ── Live stock preview on part selection ──────────────────────────────

    private void setupPartPreview() {
        fieldPart.valueProperty().addListener((obs, old, part) -> {
            if (part != null) {
                stockPreview.setText("Stock: " + part.getQtyOnHand()
                        + " " + part.getUnit()
                        + (part.isLowStock() ? "  Low!" : "  OK"));
                stockPreview.setStyle(part.isLowStock()
                        ? "-fx-text-fill:#8b1a1a; -fx-font-weight:bold;"
                        : "-fx-text-fill:#1a7a4a;");
            } else {
                stockPreview.setText("--");
                stockPreview.setStyle("");
            }
        });
    }

    // ── Day 8 — Work order cost preview on breakdown selection ────────────

    private void setupBreakdownPreview() {
        fieldBreakdown.valueProperty().addListener((obs, old, breakdown) -> {
            if (breakdown != null) {
                try {
                    double cost = service.getWorkOrderCost(breakdown.getId());
                    workOrderCostLabel.setText(String.format("Rs %.2f", cost));
                    workOrderCostBox.setVisible(true);
                    workOrderCostBox.setManaged(true);
                } catch (SQLException e) {
                    workOrderCostBox.setVisible(false);
                    workOrderCostBox.setManaged(false);
                }
            } else {
                workOrderCostBox.setVisible(false);
                workOrderCostBox.setManaged(false);
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

    // ── Alert feed ────────────────────────────────────────────────────────

    /**
     * Public — called by MainLayoutController on each poll cycle.
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
                    ? "-fx-text-fill:#8b1a1a; -fx-font-weight:bold;"
                    : "-fx-text-fill:#1a7a4a; -fx-font-weight:bold;");

            setBadge(overdueBadge,  overdue,  "Overdue");
            setBadge(lowStockBadge, lowStock, "Low Stock");

        } catch (SQLException e) {
            alertCountLabel.setText("Could not load alerts");
        }
    }

    private void setBadge(Label badge, int count, String label) {
        badge.setText(count + " " + label);
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
            loadDropdowns();      // refresh qty in part dropdown
            showForm(false);
            clearForm();
            refreshAlerts();      // immediately recheck after save
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
        Part       part   = fieldPart.getValue();
        Equipment  equip  = fieldEquipment.getValue();
        BreakdownLog bd   = fieldBreakdown.getValue();
        String qtyStr     = fieldQty.getText().trim();
        String issuedBy   = fieldIssuedBy.getText().trim();
        String type       = radioReturn.isSelected() ? "return" : "issue";

        IssueRecord r = new IssueRecord();
        r.setPartId(part  != null ? part.getId()  : 0);
        r.setEquipmentId(equip != null ? equip.getId() : 0);
        r.setBreakdownId(bd   != null ? bd.getId()    : null);   // Day 8
        r.setIssuedOn(fieldIssuedOn.getValue());
        r.setIssuedBy(issuedBy.isBlank() ? null : issuedBy);
        r.setType(type);

        if (qtyStr.isBlank())
            throw new IllegalArgumentException("Quantity is required.");
        try {
            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0) throw new NumberFormatException();
            r.setQty(qty);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantity must be a positive whole number.");
        }
        return r;
    }

    private void showForm(boolean v) {
        formPanel.setVisible(v);
        formPanel.setManaged(v);
    }

    private void clearForm() {
        fieldPart.setValue(null);
        fieldEquipment.setValue(null);
        fieldBreakdown.setValue(null);
        fieldIssuedOn.setValue(null);
        fieldQty.clear();
        fieldIssuedBy.clear();
        stockPreview.setText("--");
        stockPreview.setStyle("");
        workOrderCostBox.setVisible(false);
        workOrderCostBox.setManaged(false);
        errorLabel.setText("");
    }

    private void showError(String msg) { errorLabel.setText(msg); }

    // ── Alert list cell ───────────────────────────────────────────────────

    private static class AlertCell extends ListCell<Alert> {
        @Override
        protected void updateItem(Alert alert, boolean empty) {
            super.updateItem(alert, empty);
            if (empty || alert == null) {
                setText(null);
                setStyle("-fx-padding:0;");
                return;
            }
            setText(alert.getIcon() + " " + alert.getTitle()
                    + "\n  " + alert.getDetail()
                    + " [" + alert.getTimestampFormatted() + "]");
            setStyle(alert.getSeverityStyle() + " -fx-padding:8 12 8 12;");
        }
    }
}
