package com.jabai.ui;

import com.jabai.model.TransactionRow;
import com.jabai.model.User;
import com.jabai.service.TransactionService;
import com.jabai.util.Money;

import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TransactionsScreen extends VBox {

    private static final int LIMIT = 500;
    private static final String ALL = "All";
    private static final String SALES = "Sales";
    private static final String DONATIONS = "Donations";

    private final User actor;
    private final int locationId;
    private final TransactionService service = new TransactionService();

    private final ObservableList<TransactionRow> master = FXCollections.observableArrayList();
    private final FilteredList<TransactionRow> filtered = new FilteredList<>(master, r -> true);
    private final TableView<TransactionRow> table = new TableView<>(filtered);
    private final Label error = new Label();
    private final Label caption = new Label();
    private final ComboBox<String> typeFilter = new ComboBox<>();

    public TransactionsScreen(User actor, int locationId) {
        super(16);
        this.actor = actor;
        this.locationId = locationId;
        setPadding(new Insets(10));

        Label title = new Label("Transactions");
        title.getStyleClass().add("page-title");

        caption.getStyleClass().add("muted-label");

        error.getStyleClass().add("auth-error");
        error.setWrapText(true);
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());

        typeFilter.getItems().addAll(ALL, SALES, DONATIONS);
        typeFilter.setValue(ALL);
        typeFilter.valueProperty().addListener((obs, o, n) -> applyFilter());

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("btn-secondary");
        refresh.setOnAction(e -> reload());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(10, typeFilter, spacer, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(title, caption, toolbar, error, table);
        reload();
    }

    private void buildTable() {
        table.setPlaceholder(new Label("No transactions yet."));
        table.getColumns().add(textCol("Date", 150, TransactionRow::createdAt, "cell-muted"));
        table.getColumns().add(textCol("Ref", 70, TransactionRow::reference, "cell-primary"));
        table.getColumns().add(chipCol("Type", 130, TransactionRow::typeLabel, r -> "status-info"));
        table.getColumns().add(textCol("Details", 300, TransactionRow::details, null));
        table.getColumns().add(textCol("Amount", 110, this::amountText, "cell-right"));
        table.getColumns().add(textCol("Recorded by", 140, TransactionRow::recordedBy, "cell-muted"));
        table.getColumns().add(chipCol("Status", 100, TransactionRow::status,
                r -> r.isCompleted() ? "status-ok" : "status-out"));
        table.getColumns().add(textCol("Void reason", 200,
                r -> r.voidReason() == null ? "" : r.voidReason(), "cell-muted"));
        table.getColumns().add(actionCol());
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

    private TableColumn<TransactionRow, Void> actionCol() {
        TableColumn<TransactionRow, Void> col = new TableColumn<>("");
        col.setPrefWidth(90);
        col.setSortable(false);
        col.setCellFactory(c -> new TableCell<>() {
            private final Button void_ = new Button("Void");

            {
                void_.getStyleClass().add("btn-secondary");
                void_.setOnAction(e -> {
                    TransactionRow r = getTableRow() == null ? null : getTableRow().getItem();
                    if (r != null) confirmVoid(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                TransactionRow r = empty || getTableRow() == null ? null : getTableRow().getItem();
                setGraphic(r != null && r.isCompleted() ? void_ : null);
            }
        });
        return col;
    }

    private void applyFilter() {
        String f = typeFilter.getValue();
        filtered.setPredicate(r -> SALES.equals(f) ? r.isSale()
                : DONATIONS.equals(f) ? !r.isSale() : true);
    }

    private void reload() {
        try {
            master.setAll(service.recent(actor, locationId, LIMIT));
            caption.setText("Most recent " + LIMIT + " sales and donations for this outlet, newest first.");
        } catch (SQLException | RuntimeException ex) {
            error.setText(ex.getMessage());
        }
    }

    private void confirmVoid(TransactionRow r) {
        error.setText("");
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Void transaction");
        dialog.setHeaderText("Void " + r.typeLabel().toLowerCase() + " " + r.reference() + "?");
        dialog.setContentText("Reason:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;
        String reason = result.get().trim();
        if (reason.isEmpty()) {
            error.setText("A reason is required to void a transaction.");
            return;
        }
        try {
            service.voidTransaction(actor, r, reason);
        } catch (SQLException | RuntimeException ex) {
            error.setText(ex.getMessage());
            reload();
            return;
        }
        reload();
        Alert ok = new Alert(Alert.AlertType.INFORMATION, r.reference() + " has been voided.");
        ok.setHeaderText(null);
        ok.showAndWait();
    }
}