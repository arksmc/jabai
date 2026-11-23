package com.jabai.ui;

import com.jabai.db.Database;
import com.jabai.db.LocationDao;
import com.jabai.model.Location;
import com.jabai.util.Format;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.collections.transformation.SortedList;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class InventoryScreen extends VBox {

    private final TableView<InventoryItem> table = new TableView<>();
    private final ObservableList<InventoryItem> data = FXCollections.observableArrayList();
    private final FilteredList<InventoryItem> filtered = new FilteredList<>(data, i -> true);

    private final LocationDao locationDao = new LocationDao();
    private final ComboBox<Location> locationBox = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final CheckBox lowOnly = new CheckBox("Low / out of stock only");
    private final Label countLabel = new Label();

    public InventoryScreen() {
        setSpacing(12);
        setPadding(new Insets(10));

        Label heading = new Label("Inventory Management");
        heading.getStyleClass().add("page-title");

        // Controls
        try {
            locationBox.getItems().setAll(locationDao.findAll());
            if (!locationBox.getItems().isEmpty()) {
                locationBox.setValue(locationBox.getItems().get(0)); // warehouse comes first
            }
        } catch (SQLException ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to load locations: " + ex.getMessage()).show();
        }
        locationBox.setOnAction(e -> refresh());

        searchField.setPromptText("Search barcode, product or category...");
        searchField.setPrefWidth(280);
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());

        lowOnly.setOnAction(e -> applyFilter());

        countLabel.getStyleClass().add("muted-label");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refresh());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controls = new HBox(10,
                new Label("Location:"), locationBox,
                searchField, lowOnly,
                spacer, countLabel, refreshBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        // Table
        SortedList<InventoryItem> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No items match your filters."));
        VBox.setVgrow(table, Priority.ALWAYS);

        table.getColumns().add(TableCols.<InventoryItem>text("Barcode", InventoryItem::getCode));
        table.getColumns().add(TableCols.<InventoryItem>text("Product", InventoryItem::getName));
        table.getColumns().add(TableCols.<InventoryItem>text("Category", InventoryItem::getCategory));
        table.getColumns().add(TableCols.<InventoryItem>text("Unit", InventoryItem::getUnit));
        table.getColumns().add(TableCols.<InventoryItem>number("Quantity", InventoryItem::getQuantity, Format::qty));
        table.getColumns().add(TableCols.<InventoryItem>number("Reorder Level", InventoryItem::getReorderLevel, Format::qty));
        table.getColumns().add(TableCols.<InventoryItem>status("Stock Status", InventoryItem::getStockStatus,
                s -> switch (s) {
                    case "Out of Stock" -> "status-out";
                    case "Low Stock" -> "status-low";
                    default -> "status-ok";
                }));

        getChildren().addAll(heading, controls, table);

        refresh();
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        boolean low = lowOnly.isSelected();

        filtered.setPredicate(item -> {
            if (low && item.getStockStatus().equals("In Stock")) return false;
            if (q.isEmpty()) return true;
            return item.getCode().toLowerCase().contains(q)
                    || item.getName().toLowerCase().contains(q)
                    || item.getCategory().toLowerCase().contains(q);
        });

        countLabel.setText(filtered.size() + " of " + data.size() + " items");
    }

    private void refresh() {
        Location location = locationBox.getValue();
        if (location == null) {
            data.clear();
            applyFilter();
            return;
        }

        String sql = """
            SELECT p.code, p.name, p.category, p.unit,
                   COALESCE(i.quantity, 0) AS quantity,
                   p.reorder_level
            FROM products p
            CROSS JOIN locations l
            LEFT JOIN inventory i ON i.product_id = p.id AND i.location_id = l.id
            WHERE l.id = ?
            ORDER BY p.category, p.name
            """;

        List<InventoryItem> rows = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, location.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new InventoryItem(
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getString("unit"),
                            rs.getDouble("quantity"),
                            rs.getDouble("reorder_level")));
                }
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load inventory: " + e.getMessage()).show();
        }

        data.setAll(rows);
        applyFilter();
    }

    public static class InventoryItem {

        private final String code;
        private final String name;
        private final String category;
        private final String unit;
        private final double quantity;
        private final double reorderLevel;

        public InventoryItem(String code, String name, String category,
                             String unit, double quantity, double reorderLevel) {
            this.code = code;
            this.name = name;
            this.category = category;
            this.unit = unit;
            this.quantity = quantity;
            this.reorderLevel = reorderLevel;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getUnit() { return unit; }
        public double getQuantity() { return quantity; }
        public double getReorderLevel() { return reorderLevel; }

        public String getStockStatus() {
            if (quantity <= 0) return "Out of Stock";
            if (quantity <= reorderLevel) return "Low Stock";
            return "In Stock";
        }
    }
}