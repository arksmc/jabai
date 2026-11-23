package com.jabai.ui;

import com.jabai.db.InventoryDao;
import com.jabai.model.SaleRequestLine;
import com.jabai.model.StockRow;
import com.jabai.model.User;
import com.jabai.service.SaleService;
import com.jabai.util.Format;
import com.jabai.util.Money;
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
import java.util.Set;

public class SalesScreen extends HBox {

    private static final Set<String> WHOLE_UNITS = Set.of("pc", "packet");

    private record CartLine(StockRow row, double qty) {
        long lineTotal() { return Math.round(qty * row.priceCentavos()); }
    }

    private final User actor;
    private final int locationId;
    private final InventoryDao inventoryDao = new InventoryDao();
    private final SaleService saleService = new SaleService();

    // product side
    private final ObservableList<StockRow> products = FXCollections.observableArrayList();
    private final FilteredList<StockRow> filtered = new FilteredList<>(products, r -> true);
    private final TableView<StockRow> productTable = new TableView<>();
    private final TextField searchField = new TextField();
    private final TextField qtyField = new TextField("1");

    // cart side
    private final ObservableList<CartLine> cart = FXCollections.observableArrayList();
    private final TableView<CartLine> cartTable = new TableView<>();
    private final Label totalLabel = new Label(Money.format(0));
    private final TextField tenderedField = new TextField();
    private final Label changeLabel = new Label("Change: " + Money.format(0));
    private final Label errorLabel = new Label();
    private final Button chargeBtn = new Button("Charge");

    public SalesScreen(User actor, int locationId) {
        this.actor = actor;
        this.locationId = locationId;
        setSpacing(16);
        setPadding(new Insets(10));

        getChildren().addAll(buildProductSide(), buildCartSide());
        loadProducts();
        updateTotals();
    }

    // ---------- layout ----------

    private VBox buildProductSide() {
        Label heading = new Label("Sales");
        heading.getStyleClass().add("page-title");

        searchField.setPromptText("Search barcode, product or category...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((o, a, b) -> applyFilter());

        qtyField.setPrefWidth(70);
        qtyField.setOnAction(e -> addSelected());
        Button addBtn = new Button("Add to cart");
        addBtn.getStyleClass().add("btn-secondary");
        addBtn.setOnAction(e -> addSelected());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controls = new HBox(10, searchField, spacer, new Label("Qty:"), qtyField, addBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        SortedList<StockRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(productTable.comparatorProperty());
        productTable.setItems(sorted);
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        productTable.setPlaceholder(new Label("No products match."));
        productTable.getColumns().add(TableCols.<StockRow>primary("Product", StockRow::name));
        productTable.getColumns().add(TableCols.<StockRow>text("Category", StockRow::category));
        productTable.getColumns().add(TableCols.<StockRow>muted("Unit", StockRow::unit));
        productTable.getColumns().add(TableCols.<StockRow>number("Price",
                r -> r.priceCentavos(), v -> Money.format((long) v)));
        productTable.getColumns().add(TableCols.<StockRow>number("In stock", StockRow::quantity, Format::qty));
        productTable.setRowFactory(tv -> {
            TableRow<StockRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) addSelected();
            });
            return row;
        });
        VBox.setVgrow(productTable, Priority.ALWAYS);

        VBox box = new VBox(12, heading, controls, productTable);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox buildCartSide() {
        Label cartTitle = new Label("Current sale");
        cartTitle.getStyleClass().add("page-title");

        cartTable.setItems(cart);
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        cartTable.setPlaceholder(new Label("Cart is empty."));
        cartTable.getColumns().add(TableCols.<CartLine>primary("Item", l -> l.row().name()));
        cartTable.getColumns().add(TableCols.<CartLine>number("Qty", CartLine::qty, Format::qty));
        cartTable.getColumns().add(TableCols.<CartLine>number("Total",
                l -> l.lineTotal(), v -> Money.format((long) v)));
        cartTable.getColumns().add(removeColumn());
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        Label totalCaption = new Label("TOTAL");
        totalCaption.getStyleClass().add("muted-label");
        totalLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        tenderedField.setPromptText("Cash tendered");
        tenderedField.textProperty().addListener((o, a, b) -> updateTotals());
        tenderedField.setOnAction(e -> charge());
        Button exactBtn = new Button("Exact");
        exactBtn.getStyleClass().add("btn-secondary");
        exactBtn.setOnAction(e -> tenderedField.setText(plainAmount(cartTotal())));
        HBox tenderRow = new HBox(8, tenderedField, exactBtn);
        HBox.setHgrow(tenderedField, Priority.ALWAYS);

        changeLabel.getStyleClass().add("muted-label");

        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setWrapText(true);
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        chargeBtn.getStyleClass().add("btn-primary");
        chargeBtn.setMaxWidth(Double.MAX_VALUE);
        chargeBtn.setOnAction(e -> charge());

        Button clearBtn = new Button("Clear cart");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> clearCart());

        VBox box = new VBox(10, cartTitle, cartTable, totalCaption, totalLabel,
                tenderRow, changeLabel, errorLabel, chargeBtn, clearBtn);
        box.setPrefWidth(400);
        box.setMinWidth(400);
        return box;
    }

    private TableColumn<CartLine, CartLine> removeColumn() {
        TableColumn<CartLine, CartLine> col = new TableColumn<>("");
        col.setMaxWidth(60);
        col.setMinWidth(60);
        col.setSortable(false);
        col.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            private final Button b = new Button("✕");
            { b.getStyleClass().add("btn-secondary"); }
            @Override protected void updateItem(CartLine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                b.setOnAction(e -> { cart.remove(item); updateTotals(); });
                setGraphic(b);
            }
        });
        return col;
    }

    // ---------- behavior ----------

    private void loadProducts() {
        try {
            List<StockRow> rows = inventoryDao.findStock(locationId).stream()
                    .filter(StockRow::isActive).toList();
            products.setAll(rows);
        } catch (SQLException e) {
            errorLabel.setText("Failed to load products: " + e.getMessage());
        }
        applyFilter();
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        filtered.setPredicate(r -> q.isEmpty()
                || r.code().toLowerCase().contains(q)
                || r.name().toLowerCase().contains(q)
                || r.category().toLowerCase().contains(q));
    }

    private void addSelected() {
        errorLabel.setText("");
        StockRow row = productTable.getSelectionModel().getSelectedItem();
        if (row == null) { errorLabel.setText("Select a product first."); return; }

        double qty;
        try {
            qty = Double.parseDouble(qtyField.getText().trim());
        } catch (NumberFormatException ex) {
            errorLabel.setText("Enter a valid quantity.");
            return;
        }
        if (!Double.isFinite(qty) || qty <= 0) { errorLabel.setText("Quantity must be greater than zero."); return; }
        if (WHOLE_UNITS.contains(row.unit().toLowerCase()) && qty != Math.rint(qty)) {
            errorLabel.setText(row.name() + " is sold by the " + row.unit() + " (whole numbers only).");
            return;
        }

        int idx = indexOf(row);
        double already = idx >= 0 ? cart.get(idx).qty() : 0;
        if (already + qty > row.quantity()) {
            errorLabel.setText("Only " + Format.qty(row.quantity()) + " " + row.unit()
                    + " of " + row.name() + " in stock"
                    + (already > 0 ? " (" + Format.qty(already) + " already in the cart)." : "."));
            return;
        }

        CartLine line = new CartLine(row, already + qty);
        if (idx >= 0) cart.set(idx, line); else cart.add(line);
        qtyField.setText("1");
        updateTotals();
    }

    private int indexOf(StockRow row) {
        for (int i = 0; i < cart.size(); i++) {
            if (cart.get(i).row().productId() == row.productId()) return i;
        }
        return -1;
    }

    private long cartTotal() {
        return cart.stream().mapToLong(CartLine::lineTotal).sum();
    }

    private static String plainAmount(long centavos) {
        return java.math.BigDecimal.valueOf(centavos, 2).toPlainString();
    }

    /** Returns tendered centavos, or null if the field is empty/invalid. */
    private Long tendered() {
        String t = tenderedField.getText();
        if (t == null || t.isBlank()) return null;
        try {
            long v = Money.toCentavos(t);
            return v < 0 ? null : v;
        } catch (NumberFormatException | ArithmeticException ex) {
            return null;
        }
    }

    private void updateTotals() {
        long total = cartTotal();
        totalLabel.setText(Money.format(total));
        Long t = tendered();
        if (t == null) {
            changeLabel.setText("Change: —");
        } else if (t < total) {
            changeLabel.setText("Short by " + Money.format(total - t));
        } else {
            changeLabel.setText("Change: " + Money.format(t - total));
        }
        chargeBtn.setDisable(cart.isEmpty());
    }

    private void clearCart() {
        cart.clear();
        tenderedField.clear();
        errorLabel.setText("");
        updateTotals();
    }

    private void charge() {
        errorLabel.setText("");
        if (cart.isEmpty()) return;
        long total = cartTotal();
        Long t = tendered();
        if (t == null) { errorLabel.setText("Enter the cash tendered."); return; }
        if (t < total) { errorLabel.setText("Cash tendered is less than the total."); return; }

        List<SaleRequestLine> lines = cart.stream()
                .map(l -> new SaleRequestLine(l.row().productId(), l.qty())).toList();
        try {
            long saleId = saleService.recordSale(actor, locationId, lines, t);
            new Alert(Alert.AlertType.INFORMATION,
                    "Sale #" + saleId + " recorded.\n\nTotal: " + Money.format(total)
                            + "\nCash: " + Money.format(t)
                            + "\nChange: " + Money.format(t - total)).showAndWait();
            clearCart();
            loadProducts();
        } catch (SQLException | RuntimeException ex) {
            errorLabel.setText("Could not record sale: " + ex.getMessage());
            loadProducts(); // stock may have changed
        }
    }
}