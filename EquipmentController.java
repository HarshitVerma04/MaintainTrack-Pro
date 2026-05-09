
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Day 3 — Equipment CRUD screen.
 *
 * Returned by {@link #buildView()} as a ready-to-embed {@link BorderPane}.
 * The MainApp navigation wires this in when "Equipment" is selected.
 *
 * Layout:
 *   TOP    — toolbar (Add / Edit / Delete buttons + search field)
 *   CENTER — TableView of all equipment
 *   BOTTOM — Status bar
 */
public class EquipmentController {

    private final EquipmentDAO dao = new EquipmentDAO();

    private TableView<Equipment>         table;
    private ObservableList<Equipment>    data;
    private Label                        statusLabel;

    // - Public factory ---------------------------------

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

    // - Toolbar --------------------------------------------

    private HBox buildToolbar() {
        Button addBtn  = styledBtn("+ Add",  "#2e86de");
        Button editBtn = styledBtn("✏ Edit", "#27ae60");
        Button delBtn  = styledBtn("🗑 Delete","#e74c3c");

        TextField search = new TextField();
        search.setPromptText("Search equipment…");
        search.setPrefWidth(220);

        addBtn.setOnAction(e -> openDialog(null));
        editBtn.setOnAction(e -> {
            Equipment sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) openDialog(sel);
        });
        delBtn.setOnAction(e -> deleteSelected());

        search.textProperty().addListener((obs, old, text) -> filterTable(text));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, addBtn, editBtn, delBtn, spacer, search);
        toolbar.setPadding(new Insets(0, 0, 12, 0));
        return toolbar;
    }

    // - Table --------------------------------------------

    @SuppressWarnings("unchecked")
    private TableView<Equipment> buildTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Equipment, Integer> colId   = col("ID", 50);
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Equipment, String> colName  = col("Name", 180);
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Equipment, String> colModel = col("Model", 140);
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));

        TableColumn<Equipment, String> colSerial = col("Serial #", 140);
        colSerial.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));

        TableColumn<Equipment, String> colLoc   = col("Location", 140);
        colLoc.setCellValueFactory(new PropertyValueFactory<>("location"));

        TableColumn<Equipment, String> colStatus = col("Status", 100);
        colStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatus().name()));

        TableColumn<Equipment, String> colNext   = col("Next Maint.", 120);
        colNext.setCellValueFactory(c -> {
            LocalDate d = c.getValue().getNextMaintDate();
            return new SimpleStringProperty(d == null ? "—" : d.toString());
        });

        table.getColumns().addAll(colId, colName, colModel, colSerial,
                                  colLoc, colStatus, colNext);
        data = FXCollections.observableArrayList();
        table.setItems(data);
        table.setStyle("-fx-background-radius: 8; -fx-border-radius: 8;");
        return table;
    }

    // - Status bar --------------------------------------------

    private HBox buildStatusBar() {
        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(8, 0, 0, 0));
        return bar;
    }

    // - Data loading ---------------------------------------------

    private void loadData() {
        try {
            List<Equipment> list = dao.findAll();
            data.setAll(list);
            statusLabel.setText(list.size() + " equipment record(s) loaded.");
        } catch (SQLException e) {
            showError("Load failed: " + e.getMessage());
        }
    }

    private void filterTable(String text) {
        if (text == null || text.isBlank()) {
            loadData();
            return;
        }
        String lower = text.toLowerCase();
        try {
            List<Equipment> filtered = dao.findAll().stream()
                .filter(eq -> (eq.getName()         != null && eq.getName().toLowerCase().contains(lower))
                           || (eq.getModel()        != null && eq.getModel().toLowerCase().contains(lower))
                           || (eq.getSerialNumber() != null && eq.getSerialNumber().toLowerCase().contains(lower))
                           || (eq.getLocation()     != null && eq.getLocation().toLowerCase().contains(lower)))
                .toList();
            data.setAll(filtered);
            statusLabel.setText(filtered.size() + " result(s) for \"" + text + "\"");
        } catch (SQLException e) {
            showError("Filter failed: " + e.getMessage());
        }
    }

    // - Add / Edit dialog --------------------------------------------

    private void openDialog(Equipment existing) {
        boolean isNew = (existing == null);
        Dialog<Equipment> dialog = new Dialog<>();
        dialog.setTitle(isNew ? "Add Equipment" : "Edit Equipment");
        dialog.setHeaderText(isNew ? "Enter equipment details" : "Update equipment details");

        // Form fields
        TextField tfName   = new TextField(isNew ? "" : nvl(existing.getName()));
        TextField tfModel  = new TextField(isNew ? "" : nvl(existing.getModel()));
        TextField tfSerial = new TextField(isNew ? "" : nvl(existing.getSerialNumber()));
        TextField tfLoc    = new TextField(isNew ? "" : nvl(existing.getLocation()));
        DatePicker dpPurch = new DatePicker(isNew ? null : existing.getPurchaseDate());
        DatePicker dpNext  = new DatePicker(isNew ? null : existing.getNextMaintDate());
        ComboBox<Equipment.Status> cbStatus = new ComboBox<>(
                FXCollections.observableArrayList(Equipment.Status.values()));
        cbStatus.setValue(isNew ? Equipment.Status.ACTIVE : existing.getStatus());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.addRow(0, label("Name *"),         tfName);
        grid.addRow(1, label("Model"),           tfModel);
        grid.addRow(2, label("Serial #"),        tfSerial);
        grid.addRow(3, label("Location"),        tfLoc);
        grid.addRow(4, label("Purchase Date"),   dpPurch);
        grid.addRow(5, label("Next Maint Date"), dpNext);
        grid.addRow(6, label("Status"),          cbStatus);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Equipment eq = isNew ? new Equipment() : existing;
                eq.setName(tfName.getText().strip());
                eq.setModel(tfModel.getText().strip());
                eq.setSerialNumber(tfSerial.getText().strip());
                eq.setLocation(tfLoc.getText().strip());
                eq.setPurchaseDate(dpPurch.getValue());
                eq.setNextMaintDate(dpNext.getValue());
                eq.setStatus(cbStatus.getValue());
                return eq;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(eq -> {
            if (eq.getName().isBlank()) {
                showError("Name is required.");
                return;
            }
            try {
                if (isNew) dao.insert(eq);
                else       dao.update(eq);
                loadData();
                statusLabel.setText(isNew ? "Equipment added." : "Equipment updated.");
            } catch (SQLException e) {
                showError("Save failed: " + e.getMessage());
            }
        });
    }

    // - Delete ---------------------------------------------

    private void deleteSelected() {
        Equipment sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + sel.getName() + "\"? This will also remove its maintenance and breakdown logs.",
            ButtonType.YES, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm deletion");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                dao.delete(sel.getId());
                loadData();
                statusLabel.setText("Equipment deleted.");
            } catch (SQLException e) {
                showError("Delete failed: " + e.getMessage());
            }
        });
    }

    // - Utility ---------------------------------------------

    private <T> TableColumn<Equipment, T> col(String title, double width) {
        TableColumn<Equipment, T> c = new TableColumn<>(title);
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
