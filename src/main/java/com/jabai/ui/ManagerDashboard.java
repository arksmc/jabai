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

    private final Button productsBtn = navButton("Products");
    private final Button inventoryBtn = navButton("Inventory");

    public ManagerDashboard(Manager manager, Runnable onLogout) {
        getStyleClass().add("app-root");

        Label brand = new Label("BAGSAKAN");
        brand.getStyleClass().add("sidebar-brand");
        Label subtitle = new Label("Agriculture Data Management");
        subtitle.getStyleClass().add("sidebar-subtitle");
        Label section = new Label("MANAGE");
        section.getStyleClass().add("sidebar-section");

        productsBtn.setOnAction(e -> showProducts());
        inventoryBtn.setOnAction(e -> showInventory());

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

        VBox sidebar = new VBox(4, brand, subtitle, section,
                productsBtn, inventoryBtn, spacer, who, role, logoutBtn);
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
        for (Button b : List.of(productsBtn, inventoryBtn)) {
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
        showContent(new ProductScreen(this::showAddProduct));
    }

    private void showAddProduct() {
        setActive(productsBtn);
        showContent(new AddProductScreen(this::showProducts));
    }

    private void showInventory() {
        setActive(inventoryBtn);
        showContent(new InventoryScreen());
    }
}