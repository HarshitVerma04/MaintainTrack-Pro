package com.maintaintrack;

import com.maintaintrack.controllers.MainLayoutController;
import com.maintaintrack.dao.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.maintaintrack.auth.TokenRefreshService;
import com.maintaintrack.sync.SyncService;

import java.io.IOException;
import java.util.Objects;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

/**
 * MaintainTrack Pro — JavaFX entry point.
 *
 * Loads the main layout FXML and launches the primary window.
 * All navigation between screens (Equipment, Parts, Suppliers,
 * Maintenance, Breakdown, Issues, Dashboard) is handled inside
 * MainLayoutController by swapping the center pane content.
 */
public class MainApp extends Application {

    private static final String APP_TITLE   = "MaintainTrack Pro";
    private static final double MIN_WIDTH   = 1100;
    private static final double MIN_HEIGHT  = 720;

    @Override
    public void start(Stage primaryStage) throws IOException {
        DatabaseInitializer.initialize();
        SyncService.getInstance().start();

        // ── Show Login screen first ───────────────────────────────────────
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/Login.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/styles/app.css")
                ).toExternalForm());

        primaryStage.setTitle("MaintainTrack Pro — Sign In");
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        primaryStage.setScene(scene);

        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(Math.min(900, bounds.getWidth()));
        primaryStage.setHeight(Math.min(600, bounds.getHeight()));

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        TokenRefreshService.getInstance().shutdown();
        SyncService.getInstance().shutdown();
        super.stop();
    }

    /**
     * Required launcher wrapper for modular JavaFX.
     * Some environments need this instead of calling Application.launch() directly.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
