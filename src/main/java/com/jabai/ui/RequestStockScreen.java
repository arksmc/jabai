package com.jabai.ui;

import com.jabai.db.ProductDao;
import com.jabai.model.Product;
import com.jabai.model.RequestItemView;
import com.jabai.model.RequestLine;
import com.jabai.model.StockRequestSummary;
import com.jabai.model.User;
import com.jabai.service.StockService;
import com.jabai.util.Format;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
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

public class RequestStockScreen extends VBox {

    private static final Set<String> WHOLE_UNITS = Set.of("pc", "packet");

    private record CartLine(Product product, double qty) {}

    private final User actor;
    private final int locationId;
    private final ProductDao productDao = new ProductDao();
    private final StockService stockService = new StockService();

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final FilteredList<Product> filtered = new FilteredList<>(products, p -> true);
    private final TableView<Product> productTable = new TableView<>();
    private final TextField searchField = new TextField();
    private final TextField qtyField = new TextField("1");

    private final ObservableList<CartLine> cart = FXCollections.observableArrayList();
    private final TableView<CartLine> cartTable = new TableView<>();
    private final TextField noteField = new TextField();
    private final Label errorLabel = new Label();
    private final Button submitBtn = new Button("Send request");

    private final ObservableList<StockRequestSummary> history = FXCollections.observableArrayList();
    private final TableView<StockRequestSummary> historyTable = new TableView<>();
    private final ObservableList<RequestItemView> detailItems = FXCollections.observableArrayList();
    private final TableView<RequestItemView> detailTable = new TableView<>();
    private final Label detailTitle = new Label("Select a request to see its items.");

    public RequestStockScreen(User actor, int locationId) {
        this.actor = actor;
        this.locationId = locationId;
        setSpacing(12);
        setPadding(new Insets(10));

        Label heading = new Label("Request Stock");
        heading.getStyleClass().add("page-title");

        HBox form = new HBox(16, buildProductSide(), buildCartSide());
        form.setPrefHeight(320);
        form.setMinHeight(320);

        Label historyCaption = new Label("PAST REQUESTS");
        historyCaption.getStyleClass().add("muted-label");

        HBox historyRow = new HBox(16, buildHistorySide(), buildDetailSide());
        VBox.setVgrow(historyRow, Priority.ALWAYS);

        getChildren().addAll(heading, form, historyCaption, historyRow);

        loadProducts();
        loadHistory();
    }

    private VBox buildProductSide() {
        searchField.setPromptText("Search barcode, product or category...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((o, a, b) -> applyFilter());

        qtyField.setPrefWidth(70);
        qtyField.setOnAction(e -> addSelected());
        Button addBtn = new Button("Add to request");
        addBtn.getStyleClass().add("btn-secondary");
        addBtn.setOnAction(e -> addSelected());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controls = new HBox(10, searchField, spacer, new Label("Qty:"), qtyField, addBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(productTable.comparatorProperty());
        productTable.setItems(sorted);
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        productTable.setPlaceholder(new Label("No products match."));
        productTable.getColumns().add(TableCols.<Product>primary("Product", Product::getName));
        productTable.getColumns().add(TableCols.<Product>text("Category", Product::getCategory));
        productTable.getColumns().add(TableCols.<Product>muted("Unit", Product::getUnit));
        productTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) addSelected();
            });
            return row;
        });
        VBox.setVgrow(productTable, Priority.ALWAYS);

        VBox box = new VBox(10, controls, productTable);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox buildCartSide() {
        cartTable.setItems(cart);
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        cartTable.setPlaceholder(new Label("No items in this request yet."));
        cartTable.getColumns().add(TableCols.<CartLine>primary("Item", l -> l.product().getName()));
        cartTable.getColumns().add(TableCols.<CartLine>number("Qty", CartLine::qty, Format::qty));
        cartTable.getColumns().add(TableCols.<CartLine>muted("Unit", l -> l.product().getUnit()));
        cartTable.getColumns().add(removeColumn());
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        noteField.setPromptText("Note for the manager (optional)");

        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setWrapText(true);
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.disableProperty().bind(Bindings.isEmpty(cart));
        submitBtn.setOnAction(e -> submit());

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setOnAction(e -> clearForm());

        HBox buttons = new HBox(8, submitBtn, clearBtn);
        HBox.setHgrow(submitBtn, Priority.ALWAYS);

        VBox box = new VBox(10, cartTable, noteField, errorLabel, buttons);
        box.setPrefWidth(400);
        box.setMinWidth(400);
        return box;
    }

    private VBox buildHistorySide() {
        historyTable.setItems(history);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        historyTable.setPlaceholder(new Label("No requests yet."));
        historyTable.getColumns().add(TableCols.<StockRequestSummary>primary("Request", r -> "#" + r.getId()));
        historyTable.getColumns().add(TableCols.<StockRequestSummary>muted("Sent", StockRequestSummary::getCreatedAt));
        historyTable.getColumns().add(TableCols.<StockRequestSummary>status("Status", StockRequestSummary::getStatus,
                s -> switch (s) {
                    case "Fulfilled" -> "status-ok";
                    case "Rejected" -> "status-out";
                    case "Pending" -> "status-low";
                    default -> "status-quiet";
                }));
        historyTable.getColumns().add(TableCols.<StockRequestSummary>text("Items",
                r -> String.valueOf(r.getItemCount())));
        historyTable.getColumns().add(TableCols.<StockRequestSummary>text("Your note",
                r -> r.getNote() == null ? "" : r.getNote()));
        historyTable.getColumns().add(TableCols.<StockRequestSummary>text("Manager's reply",
                r -> r.getResponseNote() == null ? "" : r.getResponseNote()));
        historyTable.getSelectionModel().selectedItemProperty()
                .addListener((o, a, b) -> loadDetail(b));
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadHistory());

        VBox box = new VBox(8, historyTable, refreshBtn);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox buildDetailSide() {
        detailTitle.getStyleClass().add("muted-label");

        detailTable.setItems(detailItems);
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        detailTable.setPlaceholder(new Label("No items to show."));
        detailTable.getColumns().add(TableCols.<RequestItemView>primary("Item", RequestItemView::getProductName));
        detailTable.getColumns().add(TableCols.<RequestItemView>text("Requested", RequestItemView::getRequestedDisplay));
        detailTable.getColumns().add(TableCols.<RequestItemView>text("Sent", RequestItemView::getSentDisplay));
        VBox.setVgrow(detailTable, Priority.ALWAYS);

        VBox box = new VBox(8, detailTitle, detailTable);
        box.setPrefWidth(400);
        box.setMinWidth(400);
        return box;
    }

    private TableColumn<CartLine, CartLine> removeColumn() {
        TableColumn<CartLine, CartLine> col = new TableColumn<>("");
        col.setMaxWidth(60);
        col.setMinWidth(60);
        col.setSortable(false);
        col.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            private final Button b = new Button("✕");
            { b.getStyleClass().add("btn-secondary"); }
            @Override protected void updateItem(CartLine item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                b.setOnAction(e -> cart.remove(item));
                setGraphic(b);
            }
        });
        return col;
    }

    private void loadProducts() {
        try {
            List<Product> active = productDao.findAll().stream()
                    .filter(p -> "Active".equalsIgnoreCase(p.getStatus())).toList();
            products.setAll(active);
        } catch (SQLException e) {
            errorLabel.setText("Failed to load products: " + e.getMessage());
        }
        applyFilter();
    }

    private void loadHistory() {
        try {
            history.setAll(stockService.requestsForLocation(actor, locationId));
        } catch (SQLException | RuntimeException e) {
            errorLabel.setText("Failed to load requests: " + e.getMessage());
        }
        loadDetail(historyTable.getSelectionModel().getSelectedItem());
    }

    private void loadDetail(StockRequestSummary request) {
        if (request == null) {
            detailItems.clear();
            detailTitle.setText("Select a request to see its items.");
            return;
        }
        try {
            detailItems.setAll(stockService.requestItems(actor, request.getId()));
            detailTitle.setText("Items in request #" + request.getId());
        } catch (SQLException | RuntimeException e) {
            detailItems.clear();
            errorLabel.setText("Failed to load items: " + e.getMessage());
        }
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        filtered.setPredicate(p -> q.isEmpty()
                || p.getCode().toLowerCase().contains(q)
                || p.getName().toLowerCase().contains(q)
                || p.getCategory().toLowerCase().contains(q));
    }

    private void addSelected() {
        errorLabel.setText("");
        Product p = productTable.getSelectionModel().getSelectedItem();
        if (p == null) { errorLabel.setText("Select a product first."); return; }

        double qty;
        try {
            qty = Double.parseDouble(qtyField.getText().trim());
        } catch (NumberFormatException ex) {
            errorLabel.setText("Enter a valid quantity.");
            return;
        }
        if (!Double.isFinite(qty) || qty <= 0) {
            errorLabel.setText("Quantity must be greater than zero.");
            return;
        }
        if (WHOLE_UNITS.contains(p.getUnit().toLowerCase()) && qty != Math.rint(qty)) {
            errorLabel.setText(p.getName() + " is counted by the " + p.getUnit() + " (whole numbers only).");
            return;
        }

        int idx = indexOf(p);
        double already = idx >= 0 ? cart.get(idx).qty() : 0;
        CartLine line = new CartLine(p, already + qty);
        if (idx >= 0) cart.set(idx, line); else cart.add(line);
        qtyField.setText("1");
    }

    private int indexOf(Product p) {
        for (int i = 0; i < cart.size(); i++) {
            if (cart.get(i).product().getId() == p.getId()) return i;
        }
        return -1;
    }

    private void clearForm() {
        cart.clear();
        noteField.clear();
        errorLabel.setText("");
    }

    private void submit() {
        errorLabel.setText("");
        if (cart.isEmpty()) return;

        List<RequestLine> lines = cart.stream()
                .map(l -> new RequestLine(l.product().getId(), l.qty())).toList();
        try {
            long id = stockService.createRequest(actor, locationId, noteField.getText(), lines);
            new Alert(Alert.AlertType.INFORMATION,
                    "Request #" + id + " sent to the Main Warehouse.").showAndWait();
            clearForm();
            loadHistory();
        } catch (SQLException | RuntimeException ex) {
            errorLabel.setText("Could not send request: " + ex.getMessage());
        }
    }
}