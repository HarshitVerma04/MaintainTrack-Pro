package com.maintaintrack;

import com.maintaintrack.controllers.MainLayoutController;
import com.maintaintrack.dao.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

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

        // ── Initialize database (creates tables if not exist) ─────────
        DatabaseInitializer.initialize();

        // ── Load the root layout ──────────────────────────────────────
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/MainLayout.fxml")
        );
        Scene scene = new Scene(loader.load(), MIN_WIDTH, MIN_HEIGHT);

        // ── Attach global stylesheet ──────────────────────────────────
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/styles/app.css")
                ).toExternalForm()
        );

        // ── Stage config ──────────────────────────────────────────────
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setScene(scene);

        // ── Wire window-close → stop the alert polling thread ─────────
        MainLayoutController layoutCtrl = loader.getController();
        primaryStage.setOnCloseRequest(layoutCtrl::onWindowClose);

        primaryStage.show();
    }

    /**
     * Required launcher wrapper for modular JavaFX.
     * Some environments need this instead of calling Application.launch() directly.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
