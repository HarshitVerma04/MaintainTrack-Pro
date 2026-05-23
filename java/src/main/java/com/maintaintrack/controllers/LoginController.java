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

        // Disable UI while logging in
        loginButton.setDisable(true);
        loginButton.setText("Signing in...");
        errorLabel.setText("");

        // Call API on background thread — never block JavaFX thread
        CompletableFuture.supplyAsync(() -> {
            try {
                return ApiAuthService.login(username, password);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }).thenAcceptAsync(token -> {
            // Success — store session and navigate to main layout
            AuthContext.getInstance().setSession(token, username, "ADMIN");
            navigateToMain();
        }, Platform::runLater).exceptionally(ex -> {
            // Failure — show error on JavaFX thread
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
