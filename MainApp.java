
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Day 1 — JavaFX application entry point.
 *
 * Bootstraps the database connection (which also runs schema.sql),
 * then displays the main window with a left-hand navigation bar.
 *
 * Each nav button swaps the centre {@link StackPane} content area.
 * The actual CRUD screens are wired in on Days 3–5.
 */
public class MainApp extends Application {

    // ----- Colour tokens ----------------------------------------
    private static final String BG_DARK   = "#1e2d3d";
    private static final String BG_LIGHT  = "#f4f6f9";
    private static final String ACCENT    = "#2e86de";
    private static final String TEXT_MUTED = "#7f8c8d";

    @Override
    public void start(Stage primaryStage) {
        // DB init happens here — throws RuntimeException on failure
        DatabaseManager.getConnection();

        // ----- Layout --------------------------------------------
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_LIGHT + ";");

        // Left nav
        VBox nav = buildNav(root);
        root.setLeft(nav);

        // Top bar
        HBox topBar = buildTopBar();
        root.setTop(topBar);

        // Default centre view
        root.setCenter(buildWelcomePane());

        // ----- Stage --------------------------------------------
        Scene scene = new Scene(root, 1100, 700);
        primaryStage.setTitle("MaintainTrack Pro");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> DatabaseManager.close());
        primaryStage.show();
    }

    // ----- Nav bar --------------------------------------------

    private VBox buildNav(BorderPane root) {
        VBox nav = new VBox(4);
        nav.setPrefWidth(200);
        nav.setPadding(new Insets(20, 10, 20, 10));
        nav.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label logo = new Label("MaintainTrack");
        logo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        logo.setPadding(new Insets(0, 0, 20, 8));

        nav.getChildren().add(logo);

        // Nav items: label → placeholder content
        String[][] items = {
            {"🏠  Dashboard",   "Dashboard — coming Day 20"},
            {"⚙️  Equipment",    "Equipment CRUD — Day 3"},
            {"🔩  Parts",        "Parts CRUD — Day 4"},
            {"🏭  Suppliers",    "Suppliers CRUD — Day 5"},
            {"📋  Maintenance",  "Maintenance Log — Day 6"},
            {"🔧  Work Orders",  "Work Orders — Day 9"},
            {"🔔  Alerts",       "Alert Engine — Day 13-19"},
            {"📊  Reports",      "Reports & Export — Day 24-26"},
        };

        ToggleGroup group = new ToggleGroup();
        for (String[] item : items) {
            ToggleButton btn = new ToggleButton(item[0]);
            btn.setToggleGroup(group);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle(navBtnStyle(false));
            btn.selectedProperty().addListener((obs, wasSelected, isNowSelected) ->
                btn.setStyle(navBtnStyle(isNowSelected)));
            btn.setOnAction(e ->
                root.setCenter(buildPlaceholder(item[1])));
            nav.getChildren().add(btn);
        }

        return nav;
    }

    // ----- Top bar --------------------------------------------

    private HBox buildTopBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: white; -fx-border-color: #dde1e7; " +
                     "-fx-border-width: 0 0 1 0;");

        Label title = new Label("Operations Dashboard");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label version = new Label("v1.0 — MVP");
        version.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 12px;");

        bar.getChildren().addAll(title, spacer, version);
        return bar;
    }

    // ----- Placeholder / welcome --------------------------------------------

    private StackPane buildWelcomePane() {
        Label lbl = new Label("Welcome to MaintainTrack Pro\nSelect a module from the left panel.");
        lbl.setStyle("-fx-font-size: 20px; -fx-text-fill: " + TEXT_MUTED +
                     "; -fx-text-alignment: center;");
        lbl.setWrapText(true);
        StackPane pane = new StackPane(lbl);
        pane.setStyle("-fx-background-color: " + BG_LIGHT + ";");
        return pane;
    }

    private StackPane buildPlaceholder(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: " + TEXT_MUTED + ";");
        StackPane pane = new StackPane(lbl);
        pane.setStyle("-fx-background-color: " + BG_LIGHT + ";");
        return pane;
    }

    // ----- Style helpers -----------------------------------

    private String navBtnStyle(boolean selected) {
        String bg    = selected ? ACCENT : "transparent";
        String color = "white";
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; " +
               "-fx-background-radius: 6; -fx-font-size: 13px; " +
               "-fx-alignment: CENTER_LEFT; -fx-padding: 8 12 8 12;";
    }

    // ----- Main ---------------------------------------------

    public static void main(String[] args) {
        launch(args);
    }
}
