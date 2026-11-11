package com.jabai.ui;

import com.jabai.db.ProductDao;
import com.jabai.model.Product;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AddProductScreen extends VBox {
    private final ProductDao dao = new ProductDao();

    private final TextField name = new TextField();
    private final TextField code = new TextField();
    private final ComboBox<String> category = new ComboBox<>();
    private final ComboBox<String> unit = new ComboBox<>();
    private final ComboBox<String> status = new ComboBox<>();
    private final TextField sellingPrice = new TextField();
    private final TextField costPrice = new TextField();
    private final TextField reorderLevel = new TextField();

    public AddProductScreen(Runnable onDone) {
        setSpacing(12);
        setPadding(new Insets(16));

        category.getItems().addAll("Rice Grains", "Fruits", "Root Crops", "Vegetables", "Fish",
                "Dairy Products", "Planting Seeds", "Seafood", "Poultry", "Pork", "Beef");
        unit.getItems().addAll("kg", "L", "pc", "packet", "500g");
        status.getItems().addAll("Active", "Inactive");
        status.setValue("Active");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(8);

        form.addRow(0, new Label("Product name *"), name);
        form.addRow(1, new Label("Product code *"), code);
        form.addRow(2, new Label("Category *"), category);
        form.addRow(3, new Label("Unit of measure *"), unit);
        form.addRow(4, new Label("Status"), status);
        form.addRow(5, new Label("Selling price (PHP) *"), sellingPrice);
        form.addRow(6, new Label("Cost price (PHP)"), costPrice);
        form.addRow(7, new Label("Reorder level"), reorderLevel);

        Button cancel = new Button("Cancel");
        Button save = new Button("Save product");
        cancel.setOnAction(e -> onDone.run());
        save.setOnAction(e -> {
            if (save()) onDone.run();
        });

        getChildren().addAll(new Label("Add product"), form, new HBox(8, cancel, save));
    }

    private boolean save() {
        try {
            String n = name.getText().trim();
            String c = code.getText().trim();
            if (n.isEmpty() || c.isEmpty() || category.getValue() == null
                    || unit.getValue() == null || sellingPrice.getText().trim().isEmpty()) {
                error("Please fill in all required fields (*).");
                return false;
            }
            if (dao.codeExists(c)) {
                error("Product code must be unique. \"" + c + "\" already exists.");
                return false;
            }
            dao.add(new Product(0, c, n, category.getValue(), unit.getValue(), status.getValue(),
                    Double.parseDouble(sellingPrice.getText().trim()),
                    parseOrZero(costPrice.getText()),
                    parseOrZero(reorderLevel.getText())));
            return true;
        } catch (NumberFormatException ex) {
            error("Prices and reorder level must be numbers.");
        } catch (Exception ex) {
            error(ex.getMessage());
        }
        return false;
    }

    private double parseOrZero(String s) {
        s = s.trim();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    private void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
}