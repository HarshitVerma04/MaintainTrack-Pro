package com.maintaintrack.controllers;

import com.maintaintrack.auth.ApiAuthService;
import com.maintaintrack.auth.AuthContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.Objects;
import com.maintaintrack.auth.TokenRefreshService;
import com.maintaintrack.sync.SyncPullService;
import com.maintaintrack.sync.NetworkUtil;

import java.util.concurrent.CompletableFuture;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;

    @FXML
    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter username and password.");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Connecting...");
        errorLabel.setText("Waking up server — this may take 30s on first use...");

        CompletableFuture.supplyAsync(() -> {
            // Ping health endpoint first — this wakes Render if it's cold
            // NetworkUtil.isOnline() already does this, so we just call it
            NetworkUtil.isOnline();
            // Now do the actual login — Render is warm
            try {
                return ApiAuthService.login(username, password);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }).thenAcceptAsync(result -> {
            AuthContext.getInstance().setSession(
                    result.token(), username, result.role());
            TokenRefreshService.getInstance().scheduleRefresh();
            //pull cloud changes on every login
            CompletableFuture.runAsync(() -> {
                String pullResult = SyncPullService.pull();
                System.out.println("[Login] " + pullResult);
            });
            navigateToMain();
        }, Platform::runLater).exceptionally(ex -> {
            Platform.runLater(() -> {
                String msg = ex.getCause() != null
                        ? ex.getCause().getMessage()
                        : "Login failed. Check your connection.";
                errorLabel.setText(msg);
                loginButton.setDisable(false);
                loginButton.setText("Sign In");
            });
            return null;
        });
    }

    @FXML
    private void onOffline() {
        // Skip auth — continue with local SQLite only
        AuthContext.getInstance().clearSession();
        navigateToMain();
    }

    private void navigateToMain() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/MainLayout.fxml"));
                Scene scene = new Scene(loader.load(), 1100, 720);
                scene.getStylesheets().add(
                        Objects.requireNonNull(
                                getClass().getResource("/styles/app.css")
                        ).toExternalForm());

                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setMinWidth(1100);
                stage.setMinHeight(720);
                stage.setWidth(1100);
                stage.setHeight(720);
                stage.setScene(scene);
                stage.setTitle("MaintainTrack Pro v2.0");
            } catch (Exception e) {
                errorLabel.setText("Failed to load: " + e.getMessage());
            }
        });
    }
}
