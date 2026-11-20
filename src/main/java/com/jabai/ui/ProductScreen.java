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
import javafx.scene.layout.Priority;

public class ProductScreen extends VBox {
    private final ProductDao dao = new ProductDao();
    private final ObservableList<Product> data = FXCollections.observableArrayList();
    private final TableView<Product> table = new TableView<>(data);

    public ProductScreen(Runnable onAddProduct) {
        setSpacing(10);
        setPadding(new Insets(10));
        Label heading = new Label("Products");
        heading.getStyleClass().add("page-title");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);
        

        table.getColumns().add(column("Code", "code"));
        table.getColumns().add(column("Name", "name"));
        table.getColumns().add(column("Category", "category"));
        table.getColumns().add(column("Unit", "unit"));
        table.getColumns().add(column("Status", "status"));
        table.getColumns().add(column("Price", "priceDisplay"));
        table.getColumns().add(column("Cost", "costPriceDisplay"));
        table.getColumns().add(column("Reorder", "reorderLevel"));

        Button addBtn = new Button("Add product");
        Button delBtn = new Button("Delete selected");
        
        addBtn.getStyleClass().add("btn-primary");
        delBtn.getStyleClass().add("btn-danger");

        addBtn.setOnAction(e -> onAddProduct.run());
        delBtn.setOnAction(e -> {
            Product p = table.getSelectionModel().getSelectedItem();
            if (p == null) {
                new Alert(Alert.AlertType.INFORMATION, "Select a product first.").show();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + p.getName() + "?");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            try {
                boolean deleted = dao.delete(p.getId());
                if (!deleted) {
                    new Alert(Alert.AlertType.INFORMATION,
                            p.getName() + " has sales history, so it was marked Inactive instead of deleted.").show();
                }
                refresh();
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });

        getChildren().addAll(heading, table, new HBox(8, addBtn, delBtn));
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