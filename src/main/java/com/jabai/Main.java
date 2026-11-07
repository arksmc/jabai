package com.jabai;

import com.jabai.db.Database;
import com.jabai.ui.ProductScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Database.init();
        stage.setTitle("Bagsakan");
        stage.setScene(new Scene(new ProductScreen(), 900, 500));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}