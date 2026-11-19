package com.jabai.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class WelcomeScreen extends VBox {

    public WelcomeScreen(Runnable onManager, Runnable onClerk) {
        setAlignment(Pos.CENTER);
        setSpacing(18);
        setPadding(new Insets(40));
        setStyle("-fx-background-color: #f1f8f2;");

        Label title = new Label("BAGSAKAN");
        title.setStyle("-fx-font-size: 44px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");

        Label subtitle = new Label("Agriculture Data Management System");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555;");

        Label prompt = new Label("Select your role to continue");
        prompt.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");
        prompt.setPadding(new Insets(24, 0, 0, 0));

        Button managerBtn = roleButton("MANAGER", "#2e7d32");
        Button clerkBtn = roleButton("CLERK", "#1565c0");
        managerBtn.setOnAction(e -> onManager.run());
        clerkBtn.setOnAction(e -> onClerk.run());

        HBox buttons = new HBox(24, managerBtn, clerkBtn);
        buttons.setAlignment(Pos.CENTER);

        getChildren().addAll(title, subtitle, prompt, buttons);
    }

    private Button roleButton(String text, String color) {
        Button b = new Button(text);
        b.setPrefSize(220, 80);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
                + "-fx-font-size: 20px; -fx-font-weight: bold; "
                + "-fx-background-radius: 8; -fx-cursor: hand;");
        return b;
    }
}