package com.jabai.ui;

import com.jabai.db.Database;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.*;

public class InventoryScreen extends VBox {

    private final TableView<InventoryItem> table = new TableView<>();
    private final ObservableList<InventoryItem> data =
            FXCollections.observableArrayList();

    private final ComboBox<String> locationBox = new ComboBox<>();

    public InventoryScreen() {

        setSpacing(12);
        setPadding(new Insets(10));

        // Location selector
        locationBox.getItems().addAll("Main Warehouse", "Outlet 1");
        locationBox.setValue("Main Warehouse");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refresh());

        HBox controls = new HBox(8,
                new Label("Location:"),
                locationBox,
                refreshBtn
        );

        locationBox.setOnAction(e -> refresh());

        // Table columns
        table.setItems(data);

        table.getColumns().add(column("Code", "code"));
        table.getColumns().add(column("Product", "name"));
        table.getColumns().add(column("Category", "category"));
        table.getColumns().add(column("Unit", "unit"));
        table.getColumns().add(column("Quantity", "quantity"));
        table.getColumns().add(column("Reorder Level", "reorderLevel"));
        table.getColumns().add(column("Stock Status", "stockStatus"));

        getChildren().addAll(
                new Label("Inventory Management"),
                controls,
                table
        );

        refresh();
    }

    private <T> TableColumn<InventoryItem, T> column(
            String title, String property) {

        TableColumn<InventoryItem, T> col =
                new TableColumn<>(title);

        col.setCellValueFactory(
                new PropertyValueFactory<>(property)
        );

        return col;
    }

    private void refresh() {

        data.clear();

        String sql = """
            SELECT
                p.code,
                p.name,
                p.category,
                p.unit,
                i.quantity,
                p.reorder_level
            FROM inventory i
            JOIN products p ON i.product_id = p.id
            JOIN locations l ON i.location_id = l.id
            WHERE l.name = ?
            ORDER BY p.category, p.name
            """;

        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, locationBox.getValue());

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    data.add(new InventoryItem(
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getString("unit"),
                            rs.getDouble("quantity"),
                            rs.getDouble("reorder_level")
                    ));

                }
            }

        } catch (SQLException e) {

            new Alert(
                    Alert.AlertType.ERROR,
                    "Failed to load inventory: " + e.getMessage()
            ).show();

        }
    }

    public static class InventoryItem {

        private final String code;
        private final String name;
        private final String category;
        private final String unit;
        private final double quantity;
        private final double reorderLevel;

        public InventoryItem(
                String code,
                String name,
                String category,
                String unit,
                double quantity,
                double reorderLevel) {

            this.code = code;
            this.name = name;
            this.category = category;
            this.unit = unit;
            this.quantity = quantity;
            this.reorderLevel = reorderLevel;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String getUnit() {
            return unit;
        }

        public double getQuantity() {
            return quantity;
        }

        public double getReorderLevel() {
            return reorderLevel;
        }

        public String getStockStatus() {

            if (quantity <= 0) {
                return "Out of Stock";
            }

            if (quantity <= reorderLevel) {
                return "Low Stock";
            }

            return "In Stock";
        }
    }
}