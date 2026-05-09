
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

/**
 * Day 5 — Suppliers CRUD screen.
 */
public class SupplierController {

    private final SupplierDAO          dao = new SupplierDAO();
    private TableView<Supplier>        table;
    private ObservableList<Supplier>   data;
    private Label                      statusLabel;

    // ----- Public factory ------------------------------------

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

    // ----- Toolbar -----------------------------------------

    private HBox buildToolbar() {
        Button addBtn  = styledBtn("+ Add",    "#2e86de");
        Button editBtn = styledBtn("✏ Edit",   "#27ae60");
        Button delBtn  = styledBtn("🗑 Delete", "#e74c3c");

        addBtn.setOnAction(e -> openDialog(null));
        editBtn.setOnAction(e -> {
            Supplier sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) openDialog(sel);
        });
        delBtn.setOnAction(e -> deleteSelected());

        HBox toolbar = new HBox(8, addBtn, editBtn, delBtn);
        toolbar.setPadding(new Insets(0, 0, 12, 0));
        return toolbar;
    }

    // ----- Table -----------------------------------------

    @SuppressWarnings("unchecked")
    private TableView<Supplier> buildTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Supplier, Integer> colId      = col("ID", 50);
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Supplier, String> colName     = col("Company Name", 200);
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Supplier, String> colContact  = col("Contact", 160);
        colContact.setCellValueFactory(new PropertyValueFactory<>("contactName"));

        TableColumn<Supplier, String> colPhone    = col("Phone", 130);
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Supplier, String> colEmail    = col("Email", 200);
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Supplier, String> colAddr     = col("Address", 200);
        colAddr.setCellValueFactory(new PropertyValueFactory<>("address"));

        table.getColumns().addAll(colId, colName, colContact, colPhone, colEmail, colAddr);
        data = FXCollections.observableArrayList();
        table.setItems(data);
        return table;
    }

    // ----- Status bar -----------------------------------------

    private HBox buildStatusBar() {
        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(8, 0, 0, 0));
        return bar;
    }

    // ----- Data loading -----------------------------------------

    private void loadData() {
        try {
            List<Supplier> list = dao.findAll();
            data.setAll(list);
            statusLabel.setText(list.size() + " supplier(s) loaded.");
        } catch (SQLException e) {
            showError("Load failed: " + e.getMessage());
        }
    }

    // ----- Add / Edit dialog -----------------------------------

    private void openDialog(Supplier existing) {
        boolean isNew = (existing == null);
        Dialog<Supplier> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Add Supplier" : "Edit Supplier");

        TextField tfName    = new TextField(isNew ? "" : nvl(existing.getName()));
        TextField tfContact = new TextField(isNew ? "" : nvl(existing.getContactName()));
        TextField tfPhone   = new TextField(isNew ? "" : nvl(existing.getPhone()));
        TextField tfEmail   = new TextField(isNew ? "" : nvl(existing.getEmail()));
        TextField tfAddr    = new TextField(isNew ? "" : nvl(existing.getAddress()));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.addRow(0, label("Company *"),  tfName);
        grid.addRow(1, label("Contact"),    tfContact);
        grid.addRow(2, label("Phone"),      tfPhone);
        grid.addRow(3, label("Email"),      tfEmail);
        grid.addRow(4, label("Address"),    tfAddr);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Supplier s = isNew ? new Supplier() : existing;
                s.setName(tfName.getText().strip());
                s.setContactName(tfContact.getText().strip());
                s.setPhone(tfPhone.getText().strip());
                s.setEmail(tfEmail.getText().strip());
                s.setAddress(tfAddr.getText().strip());
                return s;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(s -> {
            if (s.getName().isBlank()) { showError("Company name is required."); return; }
            try {
                if (isNew) dao.insert(s);
                else       dao.update(s);
                loadData();
            } catch (SQLException e) {
                showError("Save failed: " + e.getMessage());
            }
        });
    }

    // ----- Delete -------------------------------------------

    private void deleteSelected() {
        Supplier sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + sel.getName() + "\"?", ButtonType.YES, ButtonType.CANCEL);
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); loadData(); }
            catch (SQLException e) { showError("Delete failed: " + e.getMessage()); }
        });
    }

    // ------ Utility ----------------------------------------------

    private <T> TableColumn<Supplier, T> col(String title, double width) {
        TableColumn<Supplier, T> c = new TableColumn<>(title);
        c.setMinWidth(width);
        return c;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setMinWidth(110);
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
