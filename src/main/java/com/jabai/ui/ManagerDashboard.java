package com.jabai.ui;

import com.jabai.model.Manager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class ManagerDashboard extends BorderPane {

    public ManagerDashboard(Manager manager, Runnable onLogout) {
        Button productsBtn = new Button("Products");
        Button inventoryBtn = new Button("Inventory");
        productsBtn.setOnAction(e -> showProducts());
        inventoryBtn.setOnAction(e -> setCenter(new InventoryScreen()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label who = new Label("Manager: " + manager.getFullName());
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> onLogout.run());

        HBox nav = new HBox(8, productsBtn, inventoryBtn, spacer, who, logoutBtn);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(10));
        nav.setStyle("-fx-background-color: #e9ecef;");

        setTop(nav);
        showProducts();
    }

    private void showProducts() {
        setCenter(new ProductScreen(this::showAddProduct));
    }

    private void showAddProduct() {
        setCenter(new AddProductScreen(this::showProducts));
    }
}