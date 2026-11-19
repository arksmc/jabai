package com.jabai.ui;

import com.jabai.db.UserDao;
import com.jabai.model.Location;
import com.jabai.model.Role;
import com.jabai.model.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Consumer;

public class LoginScreen extends VBox {
    private final UserDao dao = new UserDao();
    private final Role role;
    private final Location outlet;
    private final Consumer<User> onSuccess;

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label errorLabel = new Label();

    public LoginScreen(Role role, Location outlet, Runnable onBack, Consumer<User> onSuccess) {
        this.role = role;
        this.outlet = outlet;
        this.onSuccess = onSuccess;

        setAlignment(Pos.CENTER);
        setSpacing(12);
        setPadding(new Insets(40));
        setStyle("-fx-background-color: #f1f8f2;");

        String accent = role == Role.MANAGER ? "#2e7d32" : "#1565c0";

        Label title = new Label(role == Role.MANAGER ? "Manager Login" : "Clerk Login");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + accent + ";");
        getChildren().add(title);

        if (outlet != null) {
            Label outletLabel = new Label("Outlet: " + outlet.getName());
            outletLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #555555;");
            getChildren().add(outletLabel);
        }

        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(300);
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(300);

        errorLabel.setStyle("-fx-text-fill: #c62828;");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(300);

        Button loginBtn = new Button("Login");
        loginBtn.setPrefWidth(300);
        loginBtn.setStyle("-fx-background-color: " + accent + "; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 6;");

        Button backBtn = new Button("Back");
        backBtn.setPrefWidth(300);

        loginBtn.setOnAction(e -> attempt());
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> attempt());
        backBtn.setOnAction(e -> onBack.run());

        getChildren().addAll(usernameField, passwordField, errorLabel, loginBtn, backBtn);

        Platform.runLater(usernameField::requestFocus);
    }

    private void attempt() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Enter your username and password.");
            return;
        }

        Integer locationId = outlet == null ? null : outlet.getId();

        try {
            Optional<User> user = dao.authenticate(role, locationId, username, password);
            if (user.isPresent()) {
                onSuccess.accept(user.get());
            } else {
                errorLabel.setText("Invalid credentials for this role and outlet.");
                passwordField.clear();
                passwordField.requestFocus();
            }
        } catch (SQLException ex) {
            errorLabel.setText("Login failed: " + ex.getMessage());
        }
    }
}