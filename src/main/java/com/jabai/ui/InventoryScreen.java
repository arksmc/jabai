package com.jabai.ui;

import com.jabai.model.Location;
import com.jabai.model.StockRow;
import com.jabai.model.Inventory;
import com.jabai.service.BagsakanSystem;
import com.jabai.util.Format;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class InventoryScreen extends VBox {

    private final BagsakanSystem system = BagsakanSystem.getInstance();
    private final Integer fixedLocationId; // null = manager picks a location

    private final TableView<StockRow> table = new TableView<>();
    private final ObservableList<StockRow> data = FXCollections.observableArrayList();
    private final FilteredList<StockRow> filtered = new FilteredList<>(data, i -> true);

    private final ComboBox<Location> locationBox = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final CheckBox lowOnly = new CheckBox("Low / out of stock only");
    private final Label countLabel = new Label();

    /** Manager version: choose any location. */
    public InventoryScreen() {
        this(null);
    }

    /** Clerk version: locked to one location. */
    public InventoryScreen(Integer fixedLocationId) {
        this.fixedLocationId = fixedLocationId;
        setSpacing(12);
        setPadding(new Insets(10));

        Label heading = new Label(fixedLocationId == null ? "Inventory Management" : "Outlet Inventory");
        heading.getStyleClass().add("page-title");

        searchField.setPromptText("Search barcode, product or category...");
        searchField.setPrefWidth(280);
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        lowOnly.setOnAction(e -> applyFilter());
        countLabel.getStyleClass().add("muted-label");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refresh());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        if (fixedLocationId == null) {
            try {
                locationBox.getItems().setAll(system.locations());
                if (!locationBox.getItems().isEmpty()) locationBox.setValue(locationBox.getItems().get(0));
            } catch (SQLException ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to load locations: " + ex.getMessage()).show();
            }
            locationBox.setOnAction(e -> refresh());
            controls.getChildren().addAll(new Label("Location:"), locationBox);
        }
        controls.getChildren().addAll(searchField, lowOnly, spacer, countLabel, refreshBtn);

        SortedList<StockRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No items match your filters."));
        VBox.setVgrow(table, Priority.ALWAYS);

        table.getColumns().add(TableCols.<StockRow>primary("Product", StockRow::name));
        table.getColumns().add(TableCols.<StockRow>text("Category", StockRow::category));
        table.getColumns().add(TableCols.<StockRow>muted("Barcode", StockRow::code));
        table.getColumns().add(TableCols.<StockRow>muted("Unit", StockRow::unit));
        table.getColumns().add(TableCols.<StockRow>number("Quantity", StockRow::quantity, Format::qty));
        table.getColumns().add(TableCols.<StockRow>number("Reorder Level", StockRow::reorderLevel, Format::qty));
        table.getColumns().add(TableCols.<StockRow>status("Stock Status", StockRow::stockStatus,
                s -> switch (s) {
                    case "Out of Stock" -> "status-out";
                    case "Low Stock" -> "status-low";
                    default -> "status-quiet";
                }));

        getChildren().addAll(heading, controls, table);
        refresh();
    }

    private Integer currentLocationId() {
        if (fixedLocationId != null) return fixedLocationId;
        Location l = locationBox.getValue();
        return l == null ? null : l.getId();
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        boolean low = lowOnly.isSelected();
        filtered.setPredicate(item -> {
            if (low && item.stockStatus().equals("In Stock")) return false;
            if (q.isEmpty()) return true;
            return item.code().toLowerCase().contains(q)
                    || item.name().toLowerCase().contains(q)
                    || item.category().toLowerCase().contains(q);
        });
        countLabel.setText(filtered.size() + " of " + data.size() + " items");
    }

    private void refresh() {
        Integer locId = currentLocationId();
        if (locId == null) {
            data.clear();
            applyFilter();
            return;
        }
        try {
            Inventory inventory = system.inventoryAt(locId);
            data.setAll(inventory.getRows());
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load inventory: " + e.getMessage()).show();
        }
        applyFilter();
    }
}