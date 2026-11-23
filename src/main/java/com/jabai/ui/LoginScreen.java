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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Consumer;

public class LoginScreen extends AuthLayout {
    private final UserDao dao = new UserDao();
    private final Role role;
    private final Location outlet;
    private final Consumer<User> onSuccess;

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField revealField = new TextField(); // shows the password as plain text
    private final Label errorLabel = new Label();

    public LoginScreen(Role role, Location outlet, Runnable onBack, Consumer<User> onSuccess) {
        this.role = role;
        this.outlet = outlet;
        this.onSuccess = onSuccess;

        // "role-clerk" switches the accent color to blue in the stylesheet
        getStyleClass().add(role == Role.MANAGER ? "role-manager" : "role-clerk");

        VBox form = new VBox(14);
        form.getChildren().add(header(role == Role.MANAGER ? "Log in as Manager" : "Log in as Clerk", onBack));

        if (outlet != null) {
            Label chip = new Label(outlet.getName());
            chip.getStyleClass().add("auth-chip");
            form.getChildren().add(chip);
        }

        // --- username ---
        usernameField.setPromptText("Enter your username");
        usernameField.getStyleClass().add("auth-field");

        // --- password (with Show / Hide) ---
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().addAll("auth-field", "auth-field-password");
        revealField.setPromptText("Enter your password");
        revealField.getStyleClass().addAll("auth-field", "auth-field-password");
        revealField.textProperty().bindBidirectional(passwordField.textProperty());
        revealField.setVisible(false);
        revealField.setManaged(false);

        Button toggle = new Button("Show");
        toggle.getStyleClass().add("auth-toggle");
        toggle.setFocusTraversable(false);
        toggle.setOnAction(e -> {
            boolean show = !revealField.isVisible();
            revealField.setVisible(show);
            revealField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);
            toggle.setText(show ? "Hide" : "Show");
            TextField active = show ? revealField : passwordField;
            active.requestFocus();
            active.positionCaret(active.getLength());
        });

        StackPane passwordStack = new StackPane(passwordField, revealField, toggle);
        StackPane.setAlignment(toggle, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggle, new Insets(0, 8, 0, 0));

        // --- error + button ---
        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        Button loginBtn = primaryButton("Log in");
        VBox.setMargin(loginBtn, new Insets(6, 0, 0, 0));

        loginBtn.setOnAction(e -> attempt());
        usernameField.setOnAction(e -> focusPassword());
        passwordField.setOnAction(e -> attempt());
        revealField.setOnAction(e -> attempt());

        form.getChildren().addAll(
                fieldGroup("Username", usernameField),
                fieldGroup("Password", passwordStack),
                errorLabel,
                loginBtn);

        setForm(form);
        Platform.runLater(usernameField::requestFocus);
    }

    private static VBox fieldGroup(String labelText, javafx.scene.Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("auth-field-label");
        return new VBox(6, label, field);
    }

    private void focusPassword() {
        (revealField.isVisible() ? revealField : passwordField).requestFocus();
    }

    private void attempt() {
        errorLabel.setText("");
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
                focusPassword();
            }
        } catch (SQLException ex) {
            errorLabel.setText("Login failed: " + ex.getMessage());
        }
    }
}