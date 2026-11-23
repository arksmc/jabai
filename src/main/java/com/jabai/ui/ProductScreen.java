package com.jabai.ui;

import com.jabai.db.ProductDao;
import com.jabai.model.Product;
import com.jabai.util.Money;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Comparator;
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
        setSpacing(16);
        setPadding(new Insets(10));

        // --- header ---
        Label heading = new Label("Products");
        heading.getStyleClass().add("page-title");
        Label subtitle = new Label("What the warehouse can buy from suppliers, and at what price.");
        subtitle.getStyleClass().add("muted-label");
        VBox titleBox = new VBox(2, heading, subtitle);

        Button addBtn = new Button("Add product");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setMinHeight(36);
        addBtn.setOnAction(e -> openDialog(null));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, titleBox, headerSpacer, addBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // --- toolbar ---
        searchField.setPromptText("Search barcode, name or category");
        searchField.setPrefWidth(300);
        searchField.setMinHeight(38);
        searchField.getStyleClass().add("form-field");
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        categoryBox.getItems().add(ALL);
        categoryBox.setValue(ALL);
        categoryBox.setPrefWidth(200);
        categoryBox.setMinHeight(38);
        categoryBox.getStyleClass().add("form-field");
        categoryBox.setOnAction(e -> applyFilter());

        countLabel.getStyleClass().add("muted-label");
        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);
        HBox toolbar = new HBox(12, searchField, categoryBox, toolbarSpacer, countLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(header, toolbar, table);
        refresh();
    }

    private void buildTable() {
        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No products match your filters."));

        // Name (+ "Inactive" tag)
        TableColumn<Product, Product> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        nameCol.setComparator(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
        nameCol.setCellFactory(c -> new TableCell<>() {
            {
                getStyleClass().add("cell-primary");
                setContentDisplay(ContentDisplay.RIGHT);
                setGraphicTextGap(10);
            }

            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(p.getName());
                if ("Active".equalsIgnoreCase(p.getStatus())) {
                    setGraphic(null);
                } else {
                    Label tag = new Label("Inactive");
                    tag.getStyleClass().add("inactive-tag");
                    setGraphic(tag);
                }
            }
        });
        nameCol.setPrefWidth(300);

        // Category chip, barcode
        TableColumn<Product, String> categoryCol = TableCols.<Product>chip("Category", Product::getCategory);
        categoryCol.setPrefWidth(180);
        TableColumn<Product, String> barcodeCol = TableCols.<Product>muted("Barcode", Product::getCode);
        barcodeCol.setPrefWidth(160);

        // Buying price + unit, e.g. "₱360.00 / kg" (sorts by the number, not the text)
        TableColumn<Product, Product> priceCol = new TableColumn<>("Buying price");
        priceCol.getStyleClass().add("col-right");
        priceCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        priceCol.setComparator(Comparator.comparingLong(Product::getCostPriceCentavos));
        priceCol.setCellFactory(c -> new TableCell<>() {
            {
                getStyleClass().addAll("cell-right", "cell-primary");
            }

            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    return;
                }
                setText(p.getCostPriceCentavos() > 0
                        ? Money.format(p.getCostPriceCentavos()) + " / " + p.getUnit()
                        : "—");
            }
        });
        priceCol.setPrefWidth(180);

        table.getColumns().addAll(nameCol, categoryCol, barcodeCol, priceCol);

        // Click a row to edit (or remove) it.
        table.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>() {
                @Override
                protected void updateItem(Product p, boolean empty) {
                    super.updateItem(p, empty);
                    getStyleClass().remove("row-inactive");
                    if (!empty && p != null && !"Active".equalsIgnoreCase(p.getStatus())) {
                        getStyleClass().add("row-inactive");
                    }
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && !row.isEmpty()) {
                    openDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String cat = categoryBox.getValue();

        filtered.setPredicate(p ->
                (cat == null || cat.equals(ALL) || p.getCategory().equals(cat))
                && (q.isEmpty()
                    || p.getCode().toLowerCase().contains(q)
                    || p.getName().toLowerCase().contains(q)
                    || p.getCategory().toLowerCase().contains(q)));

        countLabel.setText(filtered.size() == data.size()
                ? data.size() + " products"
                : filtered.size() + " of " + data.size() + " products");
    }

    /** Pass null to add a new product, or an existing product to edit it. */
    private void openDialog(Product existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle(existing == null ? "Add product" : "Edit product");

        Runnable done = () -> {
            dialog.close();
            refresh();
        };
        AddProductScreen form = existing == null
                ? new AddProductScreen(done)
                : new AddProductScreen(existing, done);

        Scene scene = new Scene(form);
        scene.getStylesheets().addAll(getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
        table.getSelectionModel().clearSelection();
    }

    private void refresh() {
        try {
            data.setAll(dao.findAll());

            String selected = categoryBox.getValue();
            List<String> cats = data.stream().map(Product::getCategory).distinct().sorted().toList();
            categoryBox.getItems().setAll(ALL);
            categoryBox.getItems().addAll(cats);
            categoryBox.setValue(cats.contains(selected) ? selected : ALL);

            applyFilter();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage());
            alert.show();
        }
    }
}