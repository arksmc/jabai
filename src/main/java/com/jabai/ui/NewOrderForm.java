/*
 * this file is the screen where the manager builds a stock order by choosing a supplier, products, quantities and unit costs. when the order is placed, it passes the order to stockservice, which saves it and updates the warehouse stock.
 */

package com.jabai.ui;

import com.jabai.model.Manager;
import com.jabai.model.OrderLine;
import com.jabai.model.Product;
import com.jabai.db.ProductDao;
import com.jabai.service.StockService;
import com.jabai.util.Format;
import com.jabai.util.Money;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.List;

public class NewOrderForm extends VBox {

    private record Line(Product product, double quantity, long unitCostCentavos) {
        long total() {
            return Math.round(quantity * unitCostCentavos);
        }
    }

    private final ProductDao productDao = new ProductDao();
    private final StockService service = new StockService();
    private final Manager manager;
    private final Runnable onDone;

    private final TextField supplier = new TextField();
    private final TextField notes = new TextField();
    private final ComboBox<Product> productBox = new ComboBox<>();
    private final TextField quantity = new TextField();
    private final TextField unitCost = new TextField();
    private final Label totalLabel = new Label();

    private final ObservableList<Line> lines = FXCollections.observableArrayList();
    private final TableView<Line> table = new TableView<>(lines);

    public NewOrderForm(Manager manager, Runnable onDone) {
        this.manager = manager;
        this.onDone = onDone;

        setSpacing(12);
        setPadding(new Insets(20));
        getStyleClass().add("app-root");

        Label heading = new Label("New stock order");
        heading.getStyleClass().add("page-title");

        // --- order details ---
        supplier.setPromptText("e.g. Benguet Farms Coop");
        supplier.setPrefWidth(380);
        notes.setPromptText("Optional");
        notes.setPrefWidth(380);

        GridPane details = new GridPane();
        details.setHgap(12);
        details.setVgap(10);
        details.addRow(0, new Label("Supplier *"), supplier);
        details.addRow(1, new Label("Notes"), notes);

        // --- add-item row ---
        Label itemsLabel = new Label("Items");
        itemsLabel.getStyleClass().add("section-title");

        try {
            productBox.getItems().setAll(productDao.findAll().stream()
                    .filter(p -> "Active".equalsIgnoreCase(p.getStatus()))
                    .toList());
        } catch (Exception ex) {
            showError("Could not load products: " + ex.getMessage());
        }
        productBox.setPromptText("Choose product");
        productBox.setPrefWidth(250);
        productBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product p) {
                return p == null ? "" : p.getName() + " (" + p.getCode() + ")";
            }

            @Override
            public Product fromString(String s) {
                return null;
            }
        });
        productBox.valueProperty().addListener((obs, old, p) -> onProductChosen(p));

        quantity.setPromptText("Quantity");
        quantity.setPrefWidth(130);
        unitCost.setPromptText("Unit cost (₱)");
        unitCost.setPrefWidth(150);
        quantity.setOnAction(e -> addLine());
        unitCost.setOnAction(e -> addLine());

        Button addBtn = new Button("Add item");
        addBtn.getStyleClass().add("btn-secondary");
        addBtn.setOnAction(e -> addLine());

        HBox addRow = new HBox(8, productBox, quantity, unitCost, addBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);

        // --- lines table ---
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No items yet. Choose a product above and click Add item."));
        table.setPrefHeight(220);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getColumns().add(TableCols.<Line>text("Product", l -> l.product().getName()));
        table.getColumns().add(TableCols.<Line>text("Quantity",
                l -> Format.qty(l.quantity()) + " " + l.product().getUnit()));
        table.getColumns().add(TableCols.<Line>number("Unit cost",
                l -> l.unitCostCentavos(), v -> Money.format((long) v)));
        table.getColumns().add(TableCols.<Line>number("Line total",
                l -> l.total(), v -> Money.format((long) v)));

        Button removeBtn = new Button("Remove selected");
        removeBtn.getStyleClass().add("btn-secondary");
        removeBtn.setOnAction(e -> {
            Line sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                lines.remove(sel);
                updateTotal();
            }
        });

        totalLabel.getStyleClass().add("total-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox tableFooter = new HBox(10, removeBtn, spacer, totalLabel);
        tableFooter.setAlignment(Pos.CENTER_LEFT);

        // --- buttons ---
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("btn-secondary");
        cancel.setOnAction(e -> onDone.run());
        Button place = new Button("Place order");
        place.getStyleClass().add("btn-primary");
        place.setOnAction(e -> placeOrder());
        HBox buttons = new HBox(8, cancel, place);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(heading, details, itemsLabel, addRow, table, tableFooter, buttons);
        updateTotal();
    }

    private void onProductChosen(Product p) {
        if (p == null) {
            quantity.setPromptText("Quantity");
            unitCost.setPromptText("Unit cost (₱)");
            return;
        }
        quantity.setPromptText("Qty (" + p.getUnit() + ")");
        unitCost.setPromptText("Cost per " + p.getUnit() + " (₱)");
        unitCost.setText(p.getCostPriceCentavos() > 0
                ? BigDecimal.valueOf(p.getCostPriceCentavos(), 2).toPlainString()
                : "");
        quantity.requestFocus();
    }

    private void addLine() {
        Product p = productBox.getValue();
        if (p == null) {
            showError("Choose a product first.");
            return;
        }
        try {
            double qty = Double.parseDouble(quantity.getText().trim());
            if (!(qty > 0) || Double.isInfinite(qty)) {
                showError("Quantity must be greater than zero.");
                return;
            }
            long cost = unitCost.getText().isBlank() ? 0 : Money.toCentavos(unitCost.getText());
            if (cost < 0) {
                showError("Unit cost cannot be negative.");
                return;
            }

            boolean merged = false;
            for (int i = 0; i < lines.size(); i++) {
                Line existing = lines.get(i);
                if (existing.product().getId() == p.getId()) {
                	double newQty = existing.quantity() + qty;
                	long blendedCost = Math.round(
                	        (existing.quantity() * existing.unitCostCentavos() + qty * cost) / newQty);
                	lines.set(i, new Line(p, newQty, blendedCost));
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                lines.add(new Line(p, qty, cost));
            }

            productBox.setValue(null);
            quantity.clear();
            unitCost.clear();
            updateTotal();
        } catch (NumberFormatException | ArithmeticException ex) {
            showError("Quantity and cost must be valid numbers.");
        }
    }

    private void updateTotal() {
        long sum = lines.stream().mapToLong(Line::total).sum();
        totalLabel.setText("Order total: " + Money.format(sum));
    }

    private void placeOrder() {
        if (supplier.getText().isBlank()) {
            showError("Supplier is required.");
            return;
        }
        if (lines.isEmpty()) {
            showError("Add at least one item.");
            return;
        }
        try {
            List<OrderLine> orderLines = lines.stream()
                    .map(l -> new OrderLine(l.product().getId(), l.quantity(), l.unitCostCentavos()))
                    .toList();
            String note = notes.getText().isBlank() ? null : notes.getText().trim();
            service.createOrder(manager, supplier.getText(), note, orderLines);
            onDone.run();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        Window w = getScene() == null ? null : getScene().getWindow();
        if (w != null) alert.initOwner(w);
        alert.show();
    }
}