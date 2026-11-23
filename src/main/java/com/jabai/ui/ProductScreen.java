package com.jabai.ui;

import com.jabai.db.ProductDao;
import com.jabai.util.Format;
import com.jabai.model.Product;
import com.jabai.util.Money;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class ProductScreen extends VBox {
    private static final String ALL = "All categories";

    private final ProductDao dao = new ProductDao();
    private final ObservableList<Product> data = FXCollections.observableArrayList();
    private final FilteredList<Product> filtered = new FilteredList<>(data, p -> true);
    private final TableView<Product> table = new TableView<>();

    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryBox = new ComboBox<>();
    private final Label countLabel = new Label();

    public ProductScreen() {
        setSpacing(12);
        setPadding(new Insets(10));

        // Header: title + Add button
        Label heading = new Label("Products");
        heading.getStyleClass().add("page-title");

        Button addBtn = new Button("+ Add product");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> openAddDialog());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, heading, headerSpacer, addBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // Filters
        searchField.setPromptText("Search barcode, name or category...");
        searchField.setPrefWidth(280);
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        categoryBox.getItems().add(ALL);
        categoryBox.setValue(ALL);
        categoryBox.setOnAction(e -> applyFilter());

        countLabel.getStyleClass().add("muted-label");

        Button delBtn = new Button("Remove selected");
        delBtn.getStyleClass().add("btn-danger");
        delBtn.setOnAction(e -> removeSelected());

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);
        HBox controls = new HBox(10, searchField, categoryBox, filterSpacer, countLabel, delBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        // Table (SortedList lets header clicks sort the filtered rows)
        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No products match your filters."));
        table.setTableMenuButtonVisible(true); // "+" at the right of the header: show/hide columns
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Product, Number> costCol = TableCols.<Product>number("Cost",
                Product::getCostPriceCentavos, v -> Money.format((long) v));
        costCol.setVisible(false); // hidden by default; toggle from the header menu

        table.getColumns().add(TableCols.<Product>text("Barcode", Product::getCode));
        table.getColumns().add(TableCols.<Product>text("Name", Product::getName));
        table.getColumns().add(TableCols.<Product>text("Category", Product::getCategory));
        table.getColumns().add(TableCols.<Product>text("Unit", Product::getUnit));
        table.getColumns().add(TableCols.<Product>status("Status", Product::getStatus,
                s -> "Active".equalsIgnoreCase(s) ? "status-ok" : "status-muted"));
        table.getColumns().add(TableCols.<Product>number("Price",
                Product::getPriceCentavos, v -> Money.format((long) v)));
        table.getColumns().add(costCol);
        table.getColumns().add(TableCols.<Product>number("Reorder", Product::getReorderLevel, Format::qty));

        getChildren().addAll(header, controls, table);
        refresh();
    }
    
 

    // ---------- Filtering ----------

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String cat = categoryBox.getValue();

        filtered.setPredicate(p -> {
            if (cat != null && !cat.equals(ALL) && !p.getCategory().equals(cat)) return false;
            if (q.isEmpty()) return true;
            return p.getCode().toLowerCase().contains(q)
                    || p.getName().toLowerCase().contains(q)
                    || p.getCategory().toLowerCase().contains(q);
        });

        countLabel.setText(filtered.size() + " of " + data.size() + " products");
    }

    // ---------- Actions ----------

    private void openAddDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Add product");

        AddProductScreen form = new AddProductScreen(() -> {
            dialog.close();
            refresh();
        });

        Scene scene = new Scene(form);
        scene.getStylesheets().addAll(getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void removeSelected() {
        Product p = table.getSelectionModel().getSelectedItem();
        if (p == null) {
            new Alert(Alert.AlertType.INFORMATION, "Select a product first.").show();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove " + p.getName() + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            boolean deleted = dao.delete(p.getId());
            if (!deleted) {
            	new Alert(Alert.AlertType.INFORMATION,
            	        p.getName() + " has sales, order or stock history, so it was marked Inactive instead of deleted.").show();
            }
            refresh();
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void refresh() {
        try {
            data.setAll(dao.findAll());

            // rebuild the category dropdown from what's actually in the database
            String selected = categoryBox.getValue();
            List<String> cats = data.stream().map(Product::getCategory).distinct().sorted().toList();
            categoryBox.getItems().setAll(ALL);
            categoryBox.getItems().addAll(cats);
            categoryBox.setValue(cats.contains(selected) ? selected : ALL);

            applyFilter();
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
}