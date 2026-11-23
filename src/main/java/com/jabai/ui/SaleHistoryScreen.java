package com.jabai.ui;

import com.jabai.model.FilterOption;
import com.jabai.model.SaleItemView;
import com.jabai.model.SaleSummary;
import com.jabai.model.User;
import com.jabai.service.SaleHistoryService;
import com.jabai.util.Format;
import com.jabai.util.Money;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SaleHistoryScreen extends VBox {
    private static final FilterOption ALL_OUTLETS = new FilterOption(null, "All outlets", null);
    private static final FilterOption ALL_CLERKS = new FilterOption(null, "All clerks", null);

    private final User actor;
    private final SaleHistoryService service = new SaleHistoryService();

    private final ComboBox<FilterOption> outletBox = new ComboBox<>();
    private final ComboBox<FilterOption> clerkBox = new ComboBox<>();
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker = new DatePicker();
    private final TableView<SaleSummary> salesTable = new TableView<>();
    private final TableView<SaleItemView> itemsTable = new TableView<>();
    private final Label errorLabel = new Label();
    private final Label summaryLabel = new Label();
    private final Label detailLabel = new Label();
    private final List<FilterOption> allClerks = new ArrayList<>();

    public SaleHistoryScreen(User actor) {
        this.actor = actor;

        setPadding(new Insets(10));
        setSpacing(12);

        Label title = new Label("Sale History");
        title.getStyleClass().add("page-title");

        Label caption = new Label("Sales recorded by every clerk, across all outlets.");
        caption.getStyleClass().add("muted-label");

        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setWrapText(true);
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        summaryLabel.getStyleClass().add("muted-label");

        detailLabel.getStyleClass().add("section-title");
        detailLabel.setWrapText(true);

        buildSalesTable();
        buildItemsTable();

        VBox detail = new VBox(8, detailLabel, itemsTable);
        VBox.setVgrow(itemsTable, Priority.ALWAYS);

        SplitPane split = new SplitPane(salesTable, detail);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.6);
        VBox.setVgrow(split, Priority.ALWAYS);

        getChildren().addAll(title, caption, buildFilters(), errorLabel, summaryLabel, split);

        loadFilterOptions();
        load();
    }

    private Node buildFilters() {
        outletBox.setPrefWidth(180);
        clerkBox.setPrefWidth(200);
        fromPicker.setPrefWidth(140);
        toPicker.setPrefWidth(140);

        outletBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshClerks());

        Button apply = new Button("Apply");
        apply.getStyleClass().add("btn-primary");
        apply.setOnAction(e -> load());

        Button clear = new Button("Clear");
        clear.getStyleClass().add("btn-secondary");
        clear.setOnAction(e -> clearFilters());

        HBox bar = new HBox(12,
            labeled("Outlet", outletBox),
            labeled("Clerk", clerkBox),
            labeled("From", fromPicker),
            labeled("To", toPicker),
            apply,
            clear);
        bar.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        return bar;
    }

    private VBox labeled(String caption, Node field) {
        Label label = new Label(caption);
        label.getStyleClass().add("muted-label");
        return new VBox(4, label, field);
    }

    private void buildSalesTable() {
        salesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        salesTable.setPlaceholder(new Label("No sales match these filters."));

        salesTable.getColumns().add(TableCols.<SaleSummary>muted("Date", SaleSummary::getCreatedAt));
        salesTable.getColumns().add(TableCols.<SaleSummary>primary("Sale", s -> "#" + s.getId()));
        salesTable.getColumns().add(TableCols.<SaleSummary>text("Outlet", SaleSummary::getLocationName));
        salesTable.getColumns().add(TableCols.<SaleSummary>text("Cashier", SaleSummary::getCashierName));
        salesTable.getColumns().add(right(TableCols.<SaleSummary>text("Total", SaleSummary::getTotalDisplay)));
        salesTable.getColumns().add(right(TableCols.<SaleSummary>text("Tendered", SaleSummary::getCashTenderedDisplay)));
        salesTable.getColumns().add(right(TableCols.<SaleSummary>text("Change", SaleSummary::getChangeGivenDisplay)));
        salesTable.getColumns().add(TableCols.<SaleSummary>status("Status", SaleSummary::getStatus,
            value -> "Voided".equals(value) ? "status-out" : "status-ok"));

        salesTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldValue, newValue) -> showItems(newValue));
    }

    private void buildItemsTable() {
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemsTable.setPlaceholder(new Label("Select a sale above to see its items."));

        itemsTable.getColumns().add(TableCols.<SaleItemView>muted("Code", SaleItemView::getProductCode));
        itemsTable.getColumns().add(TableCols.<SaleItemView>primary("Product", SaleItemView::getProductName));
        itemsTable.getColumns().add(TableCols.<SaleItemView>number("Qty", SaleItemView::getQuantity, Format::qty));
        itemsTable.getColumns().add(TableCols.<SaleItemView>text("Unit", SaleItemView::getUnit));
        itemsTable.getColumns().add(right(TableCols.<SaleItemView>text("Unit price",
            item -> Money.format(item.getUnitPriceCentavos()))));
        itemsTable.getColumns().add(right(TableCols.<SaleItemView>text("Line total", SaleItemView::getLineTotalDisplay)));
    }

    private static <T> TableColumn<T, ?> right(TableColumn<T, ?> column) {
        column.getStyleClass().add("col-right");
        return column;
    }

    private void loadFilterOptions() {
        try {
            List<FilterOption> outlets = new ArrayList<>();
            outlets.add(ALL_OUTLETS);
            outlets.addAll(service.outlets(actor));
            outletBox.setItems(FXCollections.observableArrayList(outlets));

            allClerks.clear();
            allClerks.addAll(service.cashiers(actor));

            outletBox.getSelectionModel().selectFirst();
            refreshClerks();
        } catch (SQLException | RuntimeException ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    private void refreshClerks() {
        FilterOption outlet = outletBox.getValue();
        List<FilterOption> options = new ArrayList<>();
        options.add(ALL_CLERKS);
        for (FilterOption clerk : allClerks) {
            if (outlet == null || outlet.id() == null || Objects.equals(clerk.locationId(), outlet.id())) {
                options.add(clerk);
            }
        }
        clerkBox.setItems(FXCollections.observableArrayList(options));
        clerkBox.getSelectionModel().selectFirst();
    }

    private void clearFilters() {
        outletBox.getSelectionModel().selectFirst();
        refreshClerks();
        fromPicker.setValue(null);
        toPicker.setValue(null);
        load();
    }

    private void load() {
        errorLabel.setText("");
        FilterOption outlet = outletBox.getValue();
        FilterOption clerk = clerkBox.getValue();
        Integer outletId = outlet == null ? null : outlet.id();
        Integer clerkId = clerk == null ? null : clerk.id();

        try {
            List<SaleSummary> sales = service.history(actor, outletId, clerkId,
                fromPicker.getValue(), toPicker.getValue());
            salesTable.setItems(FXCollections.observableArrayList(sales));
            updateSummary(sales);
        } catch (SQLException | RuntimeException ex) {
            errorLabel.setText(ex.getMessage());
        }

        showItems(null);
    }

    private void updateSummary(List<SaleSummary> sales) {
        long completedTotal = 0;
        int completed = 0;
        int voided = 0;
        for (SaleSummary sale : sales) {
            if (sale.isVoided()) {
                voided++;
            } else {
                completed++;
                completedTotal += sale.getTotalCentavos();
            }
        }
        summaryLabel.setText(completed + " completed sales totaling " + Money.format(completedTotal)
            + "  |  " + voided + " voided (not counted)");
    }

    private void showItems(SaleSummary sale) {
        if (sale == null) {
            detailLabel.setText("Sale details");
            itemsTable.setItems(FXCollections.observableArrayList());
            return;
        }

        String detail = "Sale #" + sale.getId() + " - " + sale.getLocationName()
            + " - Cashier: " + sale.getCashierName();
        if (sale.isVoided()) {
            detail += " - VOIDED: " + sale.getVoidReason();
        }
        detailLabel.setText(detail);

        try {
            itemsTable.setItems(FXCollections.observableArrayList(service.items(actor, sale.getId())));
        } catch (SQLException | RuntimeException ex) {
            errorLabel.setText(ex.getMessage());
        }
    }
}