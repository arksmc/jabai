package com.jabai.ui;

import com.jabai.db.ProductDao;
import com.jabai.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProductScreen extends VBox {
    private final ProductDao dao = new ProductDao();
    private final ObservableList<Product> data = FXCollections.observableArrayList();
    private final TableView<Product> table = new TableView<>(data);

    public ProductScreen() {
        setSpacing(10);
        setPadding(new Insets(10));

        table.getColumns().add(column("Name", "name"));
        table.getColumns().add(column("Category", "category"));
        table.getColumns().add(column("Description", "description"));
        table.getColumns().add(column("Price", "price"));
        table.getColumns().add(column("Unit", "unit"));

        TextField name = new TextField();      name.setPromptText("Name");
        TextField category = new TextField();  category.setPromptText("Category");
        TextField desc = new TextField();      desc.setPromptText("Description");
        TextField price = new TextField();     price.setPromptText("Price");
        TextField unit = new TextField();      unit.setPromptText("Unit (kg, L...)");
        Button addBtn = new Button("Add");
        Button delBtn = new Button("Delete selected");

        addBtn.setOnAction(e -> {
            try {
                dao.add(name.getText().trim(), category.getText().trim(), desc.getText().trim(),
                        Double.parseDouble(price.getText().trim()), unit.getText().trim());
                name.clear(); category.clear(); desc.clear(); price.clear(); unit.clear();
                refresh();
            } catch (NumberFormatException ex) {
                error("Price must be a number.");
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });

        delBtn.setOnAction(e -> {
            Product p = table.getSelectionModel().getSelectedItem();
            if (p == null) return;
            try {
                dao.delete(p.getId());
                refresh();
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });

        getChildren().addAll(table, new HBox(8, name, category, desc, price, unit), new HBox(8, addBtn, delBtn));
        refresh();
    }

    private <T> TableColumn<Product, T> column(String title, String property) {
        TableColumn<Product, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        return col;
    }

    private void refresh() {
        try {
            data.setAll(dao.findAll());
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
}