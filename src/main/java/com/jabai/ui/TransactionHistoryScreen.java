package com.jabai.ui;

import com.jabai.model.TransactionRow;
import com.jabai.model.User;
import com.jabai.service.TransactionService;
import com.jabai.util.Money;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TransactionHistoryScreen extends VBox {

    private static final int LIMIT = 1000;
    private static final String ALL_OUTLETS = "All outlets";
    private static final String ALL_TYPES = "All types";
    private static final String SALES = "Sales";
    private static final String CASH = "Cash donations";
    private static final String ITEM = "Item donations";

    private final User actor;
    private final TransactionService service = new TransactionService();

    private final ObservableList<TransactionRow> master = FXCollections.observableArrayList();
    private final FilteredList<TransactionRow> filtered = new FilteredList<>(master, r -> true);
    private final TableView<TransactionRow> table = new TableView<>(filtered);
    private final Label error = new Label();
    private final Label caption = new Label();
    private final Label summary = new Label();
    private final ComboBox<String> outletFilter = new ComboBox<>();
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private boolean loading;

    public TransactionHistoryScreen(User actor) {
        super(16);
        this.actor = actor;
        setPadding(new Insets(10));

        Label title = new Label("Transaction History");
        title.getStyleClass().add("page-title");

        caption.getStyleClass().add("muted-label");
        caption.setText("Sales and donations across all outlets, newest first (most recent " + LIMIT + ").");

        summary.getStyleClass().add("muted-label");

        error.getStyleClass().add("auth-error");
        error.setWrapText(true);
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());

        outletFilter.getItems().add(ALL_OUTLETS);
        outletFilter.setValue(ALL_OUTLETS);
        outletFilter.valueProperty().addListener((obs, o, n) -> {
            if (!loading) applyFilter();
        });

        typeFilter.getItems().addAll(ALL_TYPES, SALES, CASH, ITEM);
        typeFilter.setValue(ALL_TYPES);
        typeFilter.valueProperty().addListener((obs, o, n) -> applyFilter());

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("btn-secondary");
        refresh.setOnAction(e -> reload());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(10, outletFilter, typeFilter, spacer, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(title, caption, toolbar, summary, error, table);
        reload();
    }

    private void buildTable() {
        table.setPlaceholder(new Label("No transactions match these filters."));
        table.getColumns().add(textCol("Date", 150, TransactionRow::createdAt, "cell-muted"));
        table.getColumns().add(textCol("Ref", 70, TransactionRow::reference, "cell-primary"));
        table.getColumns().add(textCol("Outlet", 120, TransactionRow::locationName, null));
        table.getColumns().add(chipCol("Type", 130, TransactionRow::typeLabel, r -> "status-info"));
        table.getColumns().add(textCol("Details", 300, TransactionRow::details, null));
        table.getColumns().add(textCol("Amount", 110, this::amountText, "cell-right"));
        table.getColumns().add(textCol("Recorded by", 140, TransactionRow::recordedBy, "cell-muted"));
        table.getColumns().add(chipCol("Status", 100, TransactionRow::status,
                r -> r.isCompleted() ? "status-ok" : "status-out"));
        table.getColumns().add(textCol("Void reason", 200,
                r -> r.voidReason() == null ? "" : r.voidReason(), "cell-muted"));
    }

    private String amountText(TransactionRow r) {
        return r.amountCentavos() > 0 ? Money.format(r.amountCentavos()) : "\u2014";
    }

    private TableColumn<TransactionRow, String> textCol(String title, double width,
                                                        Function<TransactionRow, String> value,
                                                        String styleClass) {
        TableColumn<TransactionRow, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setSortable(false);
        col.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            {
                if (styleClass != null) getStyleClass().add(styleClass);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }
        });
        return col;
    }

    private TableColumn<TransactionRow, String> chipCol(String title, double width,
                                                        Function<TransactionRow, String> value,
                                                        Function<TransactionRow, String> styleFor) {
        TableColumn<TransactionRow, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setSortable(false);
        col.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                TransactionRow r = getTableRow() == null ? null : getTableRow().getItem();
                if (empty || item == null || r == null) {
                    setGraphic(null);
                    return;
                }
                Label chip = new Label(item);
                chip.getStyleClass().addAll("chip", styleFor.apply(r));
                setGraphic(chip);
            }
        });
        return col;
    }

    private void applyFilter() {
        String outlet = outletFilter.getValue();
        String type = typeFilter.getValue();
        filtered.setPredicate(r -> matchesOutlet(r, outlet) && matchesType(r, type));
        updateSummary();
    }

    private boolean matchesOutlet(TransactionRow r, String outlet) {
        return outlet == null || ALL_OUTLETS.equals(outlet) || outlet.equals(r.locationName());
    }

    private boolean matchesType(TransactionRow r, String type) {
        if (type == null || ALL_TYPES.equals(type)) return true;
        return switch (type) {
            case SALES -> "SALE".equals(r.kind());
            case CASH -> "CASH".equals(r.kind());
            case ITEM -> "ITEM".equals(r.kind());
            default -> true;
        };
    }

    private void updateSummary() {
        long completedCentavos = 0;
        int completed = 0;
        for (TransactionRow r : filtered) {
            if (r.isCompleted()) {
                completed++;
                completedCentavos += r.amountCentavos();
            }
        }
        summary.setText("Showing " + filtered.size() + " of " + master.size() + " transactions. "
                + completed + " completed, worth " + Money.format(completedCentavos) + " in cash.");
    }

    private void reload() {
        error.setText("");
        List<TransactionRow> rows;
        try {
            rows = service.all(actor, LIMIT);
        } catch (SQLException | RuntimeException ex) {
            error.setText(ex.getMessage());
            return;
        }
        String previous = outletFilter.getValue();
        List<String> names = rows.stream()
                .map(TransactionRow::locationName)
                .distinct()
                .sorted()
                .toList();
        loading = true;
        outletFilter.getItems().setAll(ALL_OUTLETS);
        outletFilter.getItems().addAll(names);
        outletFilter.setValue(names.contains(previous) ? previous : ALL_OUTLETS);
        loading = false;
        master.setAll(rows);
        applyFilter();
    }
}