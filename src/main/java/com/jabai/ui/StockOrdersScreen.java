package com.jabai.ui;

import com.jabai.db.StockOrderDao;
import com.jabai.model.Manager;
import com.jabai.model.OrderItemView;
import com.jabai.model.StockOrderSummary;
import com.jabai.service.StockService;
import com.jabai.util.Format;
import com.jabai.util.Money;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class StockOrdersScreen extends VBox {
    private static final String ALL = "All statuses";

    private final StockOrderDao dao = new StockOrderDao();
    private final StockService service = new StockService();
    private final Manager manager;

    private final ObservableList<StockOrderSummary> orders = FXCollections.observableArrayList();
    private final FilteredList<StockOrderSummary> filtered = new FilteredList<>(orders, o -> true);
    private final TableView<StockOrderSummary> orderTable = new TableView<>();

    private final ObservableList<OrderItemView> items = FXCollections.observableArrayList();
    private final TableView<OrderItemView> itemTable = new TableView<>(items);

    private final ComboBox<String> statusBox = new ComboBox<>();
    private final Label countLabel = new Label();
    private final Label itemsHeading = new Label();
    private final Button receiveBtn = new Button("Mark received");
    private final Button cancelBtn = new Button("Cancel order");

    public StockOrdersScreen(Manager manager) {
        this.manager = manager;
        setSpacing(12);
        setPadding(new Insets(10));

        // --- header ---
        Label heading = new Label("Stock Orders");
        heading.getStyleClass().add("page-title");
        Button newBtn = new Button("+ New order");
        newBtn.getStyleClass().add("btn-primary");
        newBtn.setOnAction(e -> openNewOrderDialog());
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, heading, headerSpacer, newBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // --- controls ---
        statusBox.getItems().addAll(ALL, "Ordered", "Received", "Cancelled");
        statusBox.setValue(ALL);
        statusBox.setOnAction(e -> applyFilter());
        countLabel.getStyleClass().add("muted-label");

        receiveBtn.getStyleClass().add("btn-primary");
        receiveBtn.setOnAction(e -> receiveSelected());
        cancelBtn.getStyleClass().add("btn-danger");
        cancelBtn.setOnAction(e -> cancelSelected());
        receiveBtn.setDisable(true);
        cancelBtn.setDisable(true);

        Region controlSpacer = new Region();
        HBox.setHgrow(controlSpacer, Priority.ALWAYS);
        HBox controls = new HBox(10, statusBox, countLabel, controlSpacer, receiveBtn, cancelBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        // --- orders table ---
        SortedList<StockOrderSummary> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(orderTable.comparatorProperty());
        orderTable.setItems(sorted);
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        orderTable.setPlaceholder(new Label("No stock orders yet. Click \"+ New order\" to create one."));

        orderTable.getColumns().add(TableCols.<StockOrderSummary>text("Order", StockOrdersScreen::orderCode));
        orderTable.getColumns().add(TableCols.<StockOrderSummary>text("Supplier", StockOrderSummary::supplier));
        orderTable.getColumns().add(TableCols.<StockOrderSummary>status("Status", StockOrderSummary::status,
                s -> switch (s) {
                    case "Received" -> "status-ok";
                    case "Ordered" -> "status-info";
                    default -> "status-muted";
                }));
        orderTable.getColumns().add(TableCols.<StockOrderSummary>number("Items",
                o -> o.itemCount(), v -> String.valueOf((long) v)));
        orderTable.getColumns().add(TableCols.<StockOrderSummary>number("Total",
                o -> o.totalCentavos(), v -> Money.format((long) v)));
        orderTable.getColumns().add(TableCols.<StockOrderSummary>text("Ordered by", StockOrderSummary::orderedBy));
        orderTable.getColumns().add(TableCols.<StockOrderSummary>text("Ordered",
                o -> shortDate(o.createdAt())));
        orderTable.getColumns().add(TableCols.<StockOrderSummary>text("Received",
                o -> o.receivedAt() == null ? "—" : shortDate(o.receivedAt())));

        orderTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> onSelect(sel));

        // --- items table (bottom half) ---
        itemsHeading.getStyleClass().add("section-title");
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        itemTable.setPlaceholder(new Label("Nothing to show."));
        VBox.setVgrow(itemTable, Priority.ALWAYS);

        itemTable.getColumns().add(TableCols.<OrderItemView>text("Barcode", OrderItemView::code));
        itemTable.getColumns().add(TableCols.<OrderItemView>text("Product", OrderItemView::name));
        itemTable.getColumns().add(TableCols.<OrderItemView>text("Quantity",
                i -> Format.qty(i.quantity()) + " " + i.unit()));
        itemTable.getColumns().add(TableCols.<OrderItemView>number("Unit cost",
                i -> i.unitCostCentavos(), v -> Money.format((long) v)));
        itemTable.getColumns().add(TableCols.<OrderItemView>number("Line total",
                i -> i.lineTotalCentavos(), v -> Money.format((long) v)));

        VBox bottom = new VBox(8, itemsHeading, itemTable);
        bottom.setPadding(new Insets(8, 0, 0, 0));

        SplitPane split = new SplitPane(orderTable, bottom);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.58);
        VBox.setVgrow(split, Priority.ALWAYS);

        getChildren().addAll(header, controls, split);

        onSelect(null);
        refresh();
    }

    // ---------- Columns ----------

    

    /** Date + id, e.g. created 2026-11-23 with id 12 -> "2611230012". */
    private static String orderCode(StockOrderSummary o) {
        String d = o.createdAt(); // "2026-11-23 09:03:11"
        String ymd = (d != null && d.length() >= 10)
                ? d.substring(2, 4) + d.substring(5, 7) + d.substring(8, 10)
                : "000000";
        return ymd + String.format("%04d", o.id());
    }
    
    private static String shortDate(String s) {
        return s != null && s.length() >= 16 ? s.substring(0, 16) : s;
    }

    // ---------- Data ----------

    private void refresh() {
        try {
            orders.setAll(dao.findAll());
            applyFilter();
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void applyFilter() {
        String status = statusBox.getValue();
        filtered.setPredicate(o -> status == null || status.equals(ALL) || o.status().equals(status));
        countLabel.setText(filtered.size() + " of " + orders.size() + " orders");
    }

    private void onSelect(StockOrderSummary sel) {
        if (sel == null) {
            items.clear();
            itemsHeading.setText("Select an order to see its items.");
            receiveBtn.setDisable(true);
            cancelBtn.setDisable(true);
            return;
        }
        try {
            items.setAll(dao.findItems(sel.id()));
        } catch (Exception ex) {
            error(ex.getMessage());
        }
        itemsHeading.setText(orderCode(sel) + "  ·  " + sel.supplier());
        boolean waiting = "Ordered".equals(sel.status());
        receiveBtn.setDisable(!waiting);
        cancelBtn.setDisable(!waiting);
    }

    // ---------- Actions ----------

    private void openNewOrderDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("New stock order");

        NewOrderForm form = new NewOrderForm(manager, () -> {
            dialog.close();
            refresh();
        });

        Scene scene = new Scene(form, 720, 640);
        scene.getStylesheets().addAll(getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void receiveSelected() {
        StockOrderSummary o = orderTable.getSelectionModel().getSelectedItem();
        if (o == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Mark " + orderCode(o) + " as received?\n\n"
              + "Its items will be added to the Main Warehouse stock. Outlet stock is not affected.");
        confirm.initOwner(getScene().getWindow());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            service.receiveOrder(o.id(), manager.getId());
        } catch (Exception ex) {
            error(ex.getMessage());
        }
        refresh();
    }

    private void cancelSelected() {
        StockOrderSummary o = orderTable.getSelectionModel().getSelectedItem();
        if (o == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel " + orderCode(o) + "? This can't be undone.");
        confirm.initOwner(getScene().getWindow());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            service.cancelOrder(o.id());
        } catch (Exception ex) {
            error(ex.getMessage());
        }
        refresh();
    }

    private void error(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        Window w = getScene() == null ? null : getScene().getWindow();
        if (w != null) alert.initOwner(w);
        alert.show();
    }
}