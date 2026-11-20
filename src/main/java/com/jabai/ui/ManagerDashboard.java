package com.jabai.ui;

import com.jabai.model.Manager;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class ManagerDashboard extends BorderPane {

    private final Manager manager;
    private final Button productsBtn = navButton("Products");
    private final Button inventoryBtn = navButton("Inventory");
    private final Button ordersBtn = navButton("Stock Orders");

    public ManagerDashboard(Manager manager, Runnable onLogout) {
        this.manager = manager;
        getStyleClass().add("app-root");

        Label brand = new Label("BAGSAKAN");
        brand.getStyleClass().add("sidebar-brand");
        Label subtitle = new Label("Agriculture Data Management");
        subtitle.getStyleClass().add("sidebar-subtitle");

        Label manageSection = new Label("MANAGE");
        manageSection.getStyleClass().add("sidebar-section");
        Label warehouseSection = new Label("WAREHOUSE");
        warehouseSection.getStyleClass().add("sidebar-section");

        productsBtn.setOnAction(e -> showProducts());
        inventoryBtn.setOnAction(e -> showInventory());
        ordersBtn.setOnAction(e -> showOrders());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label who = new Label(manager.getFullName());
        who.getStyleClass().add("sidebar-user");
        Label role = new Label("Manager");
        role.getStyleClass().add("sidebar-role");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("sidebar-logout");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> onLogout.run());

        VBox sidebar = new VBox(4, brand, subtitle,
                manageSection, productsBtn, inventoryBtn,
                warehouseSection, ordersBtn,
                spacer, who, role, logoutBtn);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(230);

        setLeft(sidebar);
        showProducts();
    }

    private static Button navButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private void setActive(Button active) {
        for (Button b : List.of(productsBtn, inventoryBtn, ordersBtn)) {
            b.getStyleClass().remove("nav-button-active");
        }
        active.getStyleClass().add("nav-button-active");
    }

    private void showContent(Node content) {
        BorderPane.setMargin(content, new Insets(24));
        setCenter(content);
    }

    private void showProducts() {
        setActive(productsBtn);
        showContent(new ProductScreen());
    }

    private void showInventory() {
        setActive(inventoryBtn);
        showContent(new InventoryScreen());
    }

    private void showOrders() {
        setActive(ordersBtn);
        showContent(new StockOrdersScreen(manager));
    }
}