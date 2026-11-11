package com.jabai;

import com.jabai.db.Database;
import com.jabai.ui.AddProductScreen;
import com.jabai.ui.ProductScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private Scene scene;

    @Override
    public void start(Stage stage) {
        Database.init();
        scene = new Scene(new ProductScreen(this::showAddProduct), 900, 500);
        stage.setTitle("Bagsakan");
        stage.setScene(scene);
        stage.show();
    }

    private void showProducts() {
        scene.setRoot(new ProductScreen(this::showAddProduct));
    }

    private void showAddProduct() {
        scene.setRoot(new AddProductScreen(this::showProducts));
    }

    public static void main(String[] args) {
        launch(args);
    }
}