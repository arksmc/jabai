package com.jabai.ui;

import com.jabai.model.Clerk;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class ClerkDashboard extends BorderPane {

    public ClerkDashboard(Clerk clerk, Runnable onLogout) {
        Label who = new Label("Clerk: " + clerk.getFullName() + "  |  " + clerk.getLocationName());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> onLogout.run());

        HBox bar = new HBox(8, who, spacer, logoutBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10));
        bar.setStyle("-fx-background-color: #e9ecef;");

        Label welcome = new Label("Welcome, " + clerk.getFullName() + ".\nOutlet: "
                + clerk.getLocationName()
                + "\n\nSales, outlet inventory, and cash transactions will appear here in the next phases.");
        welcome.setPadding(new Insets(24));

        setTop(bar);
        setCenter(welcome);
    }
}