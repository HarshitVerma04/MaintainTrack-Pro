
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

/**
 * Day 4 — Parts CRUD screen.
 *
 * Mirrors the structure of {@link EquipmentController}.
 * Low-stock rows are highlighted in amber to give a visual hint
 * of the alert that the engine will automate on Days 13–16.
 */
public class PartsController {

    private final PartsDAO    dao     = new PartsDAO();
    private final SupplierDAO supDAO  = new SupplierDAO();

    private TableView<Parts>        table;
    private ObservableList<Parts>   data;
    private Label                   statusLabel;

    // ----- Public factory -----------------------------------

    public BorderPane buildView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #f4f6f9;");
        root.setTop(buildToolbar());
        root.setCenter(buildTable());
        root.setBottom(buildStatusBar());
        loadData();
        return root;
    }

    // ── Toolbar ──────────────────────────────────────────────

    private HBox buildToolbar() {
        Button addBtn  = styledBtn("+ Add",   "#2e86de");
        Button editBtn = styledBtn("✏ Edit",  "#27ae60");
        Button delBtn  = styledBtn("🗑 Delete","#e74c3c");
        Button lowBtn  = styledBtn("⚠ Low Stock", "#f39c12");

        addBtn.setOnAction(e -> openDialog(null));
        editBtn.setOnAction(e -> {
            Parts sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) openDialog(sel);
        });
        delBtn.setOnAction(e -> deleteSelected());
        lowBtn.setOnAction(e -> showLowStock());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, addBtn, editBtn, delBtn, lowBtn, spacer);
        toolbar.setPadding(new Insets(0, 0, 12, 0));
        return toolbar;
    }

    // ── Table ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private TableView<Parts> buildTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Parts, Integer> colId   = col("ID", 50);
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Parts, String> colName  = col("Part Name", 200);
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Parts, String> colPN    = col("Part #", 140);
        colPN.setCellValueFactory(new PropertyValueFactory<>("partNumber"));

        TableColumn<Parts, Integer> colQty  = col("Qty", 80);
        colQty.setCellValueFactory(new PropertyValueFactory<>("qtyOnHand"));

        TableColumn<Parts, Integer> colMin  = col("Min Qty", 80);
        colMin.setCellValueFactory(new PropertyValueFactory<>("minQty"));

        TableColumn<Parts, String> colCost  = col("Unit Cost", 100);
        colCost.setCellValueFactory(c ->
            new SimpleStringProperty(String.format("$%.2f", c.getValue().getUnitCost())));

        TableColumn<Parts, String> colStatus = col("Stock Status", 120);
        colStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().isLowStock() ? "⚠ LOW" : "OK"));

        // Row factory — highlight low-stock rows in amber
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Parts item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null && item.isLowStock()) {
                    setStyle("-fx-background-color: #fff3cd;");
                } else {
                    setStyle("");
                }
            }
        });

        table.getColumns().addAll(colId, colName, colPN, colQty, colMin, colCost, colStatus);
        data = FXCollections.observableArrayList();
        table.setItems(data);
        return table;
    }

    // ── Status bar ───────────────────────────────────────────

    private HBox buildStatusBar() {
        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(8, 0, 0, 0));
        return bar;
    }

    // ── Data loading ──────────────────────────────────────────

    private void loadData() {
        try {
            List<Parts> list = dao.findAll();
            data.setAll(list);
            long lowCount = list.stream().filter(Parts::isLowStock).count();
            statusLabel.setText(list.size() + " part(s) — " + lowCount + " low-stock.");
        } catch (SQLException e) {
            showError("Load failed: " + e.getMessage());
        }
    }

    private void showLowStock() {
        try {
            data.setAll(dao.findLowStock());
            statusLabel.setText("Showing low-stock parts only.");
        } catch (SQLException e) {
            showError("Filter failed: " + e.getMessage());
        }
    }

    // ── Add / Edit dialog ─────────────────────────────────────

    private void openDialog(Parts existing) {
        boolean isNew = (existing == null);
        Dialog<Parts> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Add Part" : "Edit Part");
        dialog.setHeaderText(isNew ? "Enter part details" : "Update part details");

        TextField tfName   = new TextField(isNew ? "" : nvl(existing.getName()));
        TextField tfPN     = new TextField(isNew ? "" : nvl(existing.getPartNumber()));
        TextField tfDesc   = new TextField(isNew ? "" : nvl(existing.getDescription()));
        Spinner<Integer> spQty = new Spinner<>(0, 999999,
                                               isNew ? 0 : existing.getQtyOnHand());
        Spinner<Integer> spMin = new Spinner<>(0, 999999,
                                               isNew ? 0 : existing.getMinQty());
        TextField tfCost   = new TextField(isNew ? "0.00" :
                                           String.format("%.2f", existing.getUnitCost()));

        // Supplier combo
        ComboBox<String> cbSup = new ComboBox<>();
        cbSup.getItems().add("— None —");
        List<Supplier> suppliers = List.of();
        try { suppliers = supDAO.findAll(); } catch (SQLException ignored) {}
        for (Supplier s : suppliers) cbSup.getItems().add(s.getId() + " — " + s.getName());
        cbSup.setValue("— None —");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.addRow(0, label("Name *"),      tfName);
        grid.addRow(1, label("Part #"),      tfPN);
        grid.addRow(2, label("Description"), tfDesc);
        grid.addRow(3, label("Qty on Hand"), spQty);
        grid.addRow(4, label("Min Qty"),     spMin);
        grid.addRow(5, label("Unit Cost $"), tfCost);
        grid.addRow(6, label("Supplier"),    cbSup);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Parts p = isNew ? new Parts() : existing;
                p.setName(tfName.getText().strip());
                p.setPartNumber(tfPN.getText().strip());
                p.setDescription(tfDesc.getText().strip());
                p.setQtyOnHand(spQty.getValue());
                p.setMinQty(spMin.getValue());
                try { p.setUnitCost(Double.parseDouble(tfCost.getText())); }
                catch (NumberFormatException ignored) { p.setUnitCost(0); }
                // Parse supplier id from combo selection
                String supSel = cbSup.getValue();
                if (supSel != null && supSel.contains(" — ") && !supSel.startsWith("—")) {
                    try { p.setSupplierId(Integer.parseInt(supSel.split(" — ")[0])); }
                    catch (NumberFormatException ignored) { p.setSupplierId(0); }
                }
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            if (p.getName().isBlank()) { showError("Name is required."); return; }
            try {
                if (isNew) dao.insert(p);
                else       dao.update(p);
                loadData();
            } catch (SQLException e) {
                showError("Save failed: " + e.getMessage());
            }
        });
    }

    // ── Delete ───────────────────────────────────────────────

    private void deleteSelected() {
        Parts sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + sel.getName() + "\"?", ButtonType.YES, ButtonType.CANCEL);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); loadData(); }
            catch (SQLException e) { showError("Delete failed: " + e.getMessage()); }
        });
    }

    // ── Utility ──────────────────────────────────────────────

    private <T> TableColumn<Parts, T> col(String title, double width) {
        TableColumn<Parts, T> c = new TableColumn<>(title);
        c.setMinWidth(width);
        return c;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setMinWidth(120);
        return l;
    }

    private Button styledBtn(String text, String bg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white; " +
                   "-fx-background-radius: 6; -fx-padding: 6 14 6 14;");
        return b;
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
