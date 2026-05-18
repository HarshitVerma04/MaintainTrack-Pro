package com.maintaintrack.controllers;

import com.maintaintrack.dao.BreakdownLogDAO;
import com.maintaintrack.dao.EquipmentDAO;
import com.maintaintrack.dao.MaintenanceLogDAO;
import com.maintaintrack.dao.PartDAO;
import com.maintaintrack.models.*;
import com.maintaintrack.models.Alert;
import com.maintaintrack.services.AlertService;
import com.maintaintrack.services.IssueRecordService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * IssueController — Issues and Work Orders screen.
 *
 * Day 8: breakdown link dropdown + cost preview
 * Day 9: maintenance link dropdown + full work order column
 */
public class IssueController {

    // Table
    @FXML private TableView<IssueRecord>             issueTable;
    @FXML private TableColumn<IssueRecord,Integer>   colId;
    @FXML private TableColumn<IssueRecord,String>    colPart;
    @FXML private TableColumn<IssueRecord,String>    colEquipment;
    @FXML private TableColumn<IssueRecord,LocalDate> colIssuedOn;
    @FXML private TableColumn<IssueRecord,Integer>   colQty;
    @FXML private TableColumn<IssueRecord,String>    colIssuedBy;
    @FXML private TableColumn<IssueRecord,String>    colType;
    @FXML private TableColumn<IssueRecord,String>    colWorkOrder;

    // Form
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

    // Day 8
    @FXML private ComboBox<BreakdownLog>   fieldBreakdown;

    // Day 9
    @FXML private ComboBox<MaintenanceLog> fieldMaintenance;

    // Shared work order preview
    @FXML private HBox  workOrderCostBox;
    @FXML private Label workOrderCostLabel;

    // Alert feed
    @FXML private Label           alertCountLabel;
    @FXML private ListView<Alert> alertListView;

    // Badges
    @FXML private Label overdueBadge;
    @FXML private Label lowStockBadge;

    private final IssueRecordService service       = new IssueRecordService();
    private final AlertService       alertService  = new AlertService();
    private final EquipmentDAO       equipmentDAO  = new EquipmentDAO();
    private final PartDAO            partDAO       = new PartDAO();
    private final BreakdownLogDAO    breakdownDAO  = new BreakdownLogDAO();
    private final MaintenanceLogDAO  maintenanceDAO= new MaintenanceLogDAO();

    private final ObservableList<IssueRecord> tableData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        setupToggleGroup();
        loadDropdowns();
        setupPartPreview();
        setupWorkOrderPreview();
        loadTable();
        refreshAlerts();
    }

    private void setupColumns() {
        colId.setCellValueFactory(d ->
            new javafx.beans.property.SimpleIntegerProperty(d.getValue().getId()).asObject());
        colPart.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getPartName()));
        colEquipment.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getEquipmentName()));
        colIssuedOn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getIssuedOn()));
        colQty.setCellValueFactory(d ->
            new javafx.beans.property.SimpleIntegerProperty(d.getValue().getQty()).asObject());
        colIssuedBy.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getIssuedBy()));

        // Type - colour coded
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setText(null); setStyle(""); return; }
                IssueRecord r = getTableView().getItems().get(getIndex());
                setText(r.getTypeDisplay());
                setStyle("Issue".equals(r.getTypeDisplay())
                    ? "-fx-text-fill:#8b1a1a; -fx-font-weight:bold;"
                    : "-fx-text-fill:#1a7a4a; -fx-font-weight:bold;");
            }
        });

        // Work order column - Day 9 - shows both breakdown and maintenance links
        colWorkOrder.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setText(null); setStyle(""); return; }
                IssueRecord r = getTableView().getItems().get(getIndex());
                setText(r.getWorkOrderLabel());
                if (r.isBreakdownWorkOrder()) {
                    setStyle("-fx-text-fill:#8b1a1a; -fx-font-style:italic;");
                } else if (r.isMaintenanceWorkOrder()) {
                    setStyle("-fx-text-fill:#1a7a4a; -fx-font-style:italic;");
                } else {
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
            fieldBreakdown.setItems(FXCollections.observableArrayList(breakdownDAO.findAll()));
            fieldMaintenance.setItems(FXCollections.observableArrayList(maintenanceDAO.findAll()));
            fieldBreakdown.setValue(null);
            fieldMaintenance.setValue(null);
        } catch (SQLException e) {
            showError("Could not load dropdowns: " + e.getMessage());
        }
    }

    private void setupPartPreview() {
        fieldPart.valueProperty().addListener((obs, old, part) -> {
            if (part != null) {
                stockPreview.setText("Stock: " + part.getQtyOnHand()
                    + " " + part.getUnit() + (part.isLowStock() ? "  Low!" : "  OK"));
                stockPreview.setStyle(part.isLowStock()
                    ? "-fx-text-fill:#8b1a1a; -fx-font-weight:bold;"
                    : "-fx-text-fill:#1a7a4a;");
            } else {
                stockPreview.setText("--");
                stockPreview.setStyle("");
            }
        });
    }

    /**
     * Day 8+9: when either breakdown or maintenance is selected,
     * show the running work order cost so far.
     */
    private void setupWorkOrderPreview() {
        fieldBreakdown.valueProperty().addListener((obs, old, bd) -> {
            // Clear maintenance when breakdown selected and vice versa
            if (bd != null) fieldMaintenance.setValue(null);
            updateCostPreview();
        });
        fieldMaintenance.valueProperty().addListener((obs, old, m) -> {
            if (m != null) fieldBreakdown.setValue(null);
            updateCostPreview();
        });
    }

    private void updateCostPreview() {
        try {
            BreakdownLog   bd = fieldBreakdown.getValue();
            MaintenanceLog m  = fieldMaintenance.getValue();

            if (bd != null) {
                double cost = service.getBreakdownWorkOrderCost(bd.getId());
                workOrderCostLabel.setText(String.format("Rs %.2f (breakdown repair)", cost));
                workOrderCostBox.setVisible(true);
                workOrderCostBox.setManaged(true);
            } else if (m != null) {
                double cost = service.getMaintenanceWorkOrderCost(m.getId());
                workOrderCostLabel.setText(String.format("Rs %.2f (PM job consumables)", cost));
                workOrderCostBox.setVisible(true);
                workOrderCostBox.setManaged(true);
            } else {
                workOrderCostBox.setVisible(false);
                workOrderCostBox.setManaged(false);
            }
        } catch (SQLException e) {
            workOrderCostBox.setVisible(false);
            workOrderCostBox.setManaged(false);
        }
    }

    private void loadTable() {
        try {
            tableData.setAll(service.getAllRecords());
        } catch (SQLException e) {
            showError("Failed to load records: " + e.getMessage());
        }
    }

    public void refreshAlerts() {
        try {
            List<Alert> alerts = alertService.getActiveAlerts();
            alertListView.setItems(FXCollections.observableArrayList(alerts));
            alertListView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Alert a, boolean empty) {
                    super.updateItem(a, empty);
                    if (empty || a == null) { setText(null); return; }
                    setText(a.getIcon() + " " + a.getTitle() + "\n  " + a.getDetail());
                    setStyle(a.getSeverityStyle() + " -fx-padding:6 10;");
                }
            });
            int total = alerts.size();
            alertCountLabel.setText(total + " active alert" + (total == 1 ? "" : "s"));
            alertCountLabel.setStyle(total > 0
                ? "-fx-text-fill:#8b1a1a; -fx-font-weight:bold;"
                : "-fx-text-fill:#1a7a4a; -fx-font-weight:bold;");

            long overdue  = alerts.stream().filter(a -> a.getType() == Alert.Type.OVERDUE_MAINTENANCE).count();
            long lowStock = alerts.stream().filter(a -> a.getType() == Alert.Type.LOW_STOCK).count();
            setBadge(overdueBadge,  (int)overdue,  "Overdue");
            setBadge(lowStockBadge, (int)lowStock, "Low Stock");
        } catch (SQLException e) {
            alertCountLabel.setText("Could not load alerts");
        }
    }

    private void setBadge(Label b, int count, String label) {
        b.setText(count + " " + label);
        b.setVisible(count > 0);
        b.setManaged(count > 0);
    }

    @FXML
    private void onAdd() {
        clearForm();
        fieldIssuedOn.setValue(LocalDate.now());
        radioIssue.setSelected(true);
        showForm(true);
    }

    @FXML
    private void onSave() {
        errorLabel.setText("");
        try {
            IssueRecord record = buildFromForm();
            service.recordTransaction(record);
            loadTable();
            loadDropdowns();
            showForm(false);
            clearForm();
            refreshAlerts();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancel() { showForm(false); clearForm(); }

    private IssueRecord buildFromForm() {
        Part           part  = fieldPart.getValue();
        Equipment      equip = fieldEquipment.getValue();
        BreakdownLog   bd    = fieldBreakdown.getValue();
        MaintenanceLog m     = fieldMaintenance.getValue();
        String qtyStr  = fieldQty.getText().trim();
        String issuedBy= fieldIssuedBy.getText().trim();
        String type    = radioReturn.isSelected() ? "return" : "issue";

        IssueRecord r = new IssueRecord();
        r.setPartId(part  != null ? part.getId()  : 0);
        r.setEquipmentId(equip != null ? equip.getId() : 0);
        r.setBreakdownId(bd   != null ? bd.getId()    : null);
        r.setMaintenanceId(m  != null ? m.getId()     : null);
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

    private void showForm(boolean v) { formPanel.setVisible(v); formPanel.setManaged(v); }

    private void clearForm() {
        fieldPart.setValue(null);
        fieldEquipment.setValue(null);
        fieldBreakdown.setValue(null);
        fieldMaintenance.setValue(null);
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
}
