package com.jabai.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class WelcomeScreen extends AuthLayout {

    public WelcomeScreen(Runnable onManager, Runnable onClerk) {
        Label title = new Label("Log in to Bagsakan");
        title.getStyleClass().add("auth-title");

        Label subtitle = new Label("Choose how you're signing in.");
        subtitle.getStyleClass().add("auth-subtitle");

        Button managerBtn = primaryButton("Log in as Manager");
        managerBtn.setOnAction(e -> onManager.run());

        Button clerkBtn = outlineButton("Log in as Clerk");
        clerkBtn.setOnAction(e -> onClerk.run());

        Label footnote = new Label("Agriculture Data Management System");
        footnote.getStyleClass().add("auth-footnote");
        footnote.setMaxWidth(Double.MAX_VALUE);
        footnote.setAlignment(Pos.CENTER);
        VBox.setMargin(footnote, new Insets(18, 0, 0, 0));

        setForm(new VBox(14, title, subtitle, managerBtn, clerkBtn, footnote));
    }
}