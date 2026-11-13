package com.jabai;

import com.jabai.db.Database;
import com.jabai.ui.InventoryScreen;
import com.jabai.ui.ProductScreen;
import com.jabai.ui.AddProductScreen;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Main extends Application {

    private final BorderPane root = new BorderPane();

    @Override
    public void start(Stage stage) {
        Database.init();

        Button productsBtn = new Button("Products");
        Button inventoryBtn = new Button("Inventory");
        productsBtn.setOnAction(e -> showProducts());
        inventoryBtn.setOnAction(e -> root.setCenter(new InventoryScreen()));

        HBox nav = new HBox(8, productsBtn, inventoryBtn);
        nav.setPadding(new Insets(10));
        nav.setStyle("-fx-background-color: #e9ecef;");

        root.setTop(nav);
        showProducts();

        stage.setTitle("Bagsakan");
        stage.setScene(new Scene(root, 900, 550));
        stage.show();
    }

    private void showProducts() {
        root.setCenter(new ProductScreen(this::showAddProduct));
    }

    private void showAddProduct() {
        // after Save or Cancel, go back to the product list
        root.setCenter(new AddProductScreen(this::showProducts));
    }

    public static void main(String[] args) {
        launch(args);
    }
}