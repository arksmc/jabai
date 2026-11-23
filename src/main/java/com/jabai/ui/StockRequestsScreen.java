package com.jabai.ui;

import com.jabai.model.Manager;
import com.jabai.model.RequestItemView;
import com.jabai.model.SendLine;
import com.jabai.model.StockRequestSummary;
import com.jabai.service.StockService;
import com.jabai.util.Format;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StockRequestsScreen extends VBox {

    private static final String ALL = "All";
    private static final String PENDING = "Pending";

    private static class SendRow {
        final RequestItemView item;
        final double available;
        String send;

        SendRow(RequestItemView item, double available) {
            this.item = item;
            this.available = available;
            this.send = BigDecimal.valueOf(item.getQuantityRequested()).stripTrailingZeros().toPlainString();
        }

        String stockLabel() {
            return available >= item.getQuantityRequested() ? "Enough" : "Short";
        }
    }

    private final Manager manager;
    private final StockService service = new StockService();

    private final ObservableList<StockRequestSummary> requests = FXCollections.observableArrayList();
    private final FilteredList<StockRequestSummary> filtered = new FilteredList<>(requests, r -> true);
    private final TableView<StockRequestSummary> requestTable = new TableView<>(filtered);

    private final ObservableList<SendRow> rows = FXCollections.observableArrayList();
    private final TableView<SendRow> itemTable = new TableView<>(rows);

    private final ComboBox<String> statusBox = new ComboBox<>();
    private final Label countLabel = new Label();
    private final Label detailHeading = new Label();
    private final Label clerkNote = new Label();
    private final Label errorLabel = new Label();
    private final TextField replyField = new TextField();
    private final Button fulfillBtn = new Button("Fulfill request");
    private final Button rejectBtn = new Button("Reject");

    private boolean pendingSelected;

    public StockRequestsScreen(Manager manager) {
        this.manager = manager;
        setSpacing(12);
        setPadding(new Insets(10));

        Label heading = new Label("Stock Requests");
        heading.getStyleClass().add("page-title");

        statusBox.getItems().addAll(PENDING, ALL, "Fulfilled", "Rejected");
        statusBox.setValue(PENDING);
        statusBox.setOnAction(e -> applyFilter());
        countLabel.getStyleClass().add("muted-label");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refresh());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controls = new HBox(10, statusBox, countLabel, spacer, refreshBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        buildRequestTable();
        buildItemTable();

        detailHeading.getStyleClass().add("section-title");
        clerkNote.getStyleClass().add("muted-label");
        clerkNote.setWrapText(true);

        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setWrapText(true);
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        replyField.setPromptText("Reply to the outlet (optional)");
        HBox.setHgrow(replyField, Priority.ALWAYS);

        fulfillBtn.getStyleClass().add("btn-primary");
        fulfillBtn.setOnAction(e -> fulfill());
        rejectBtn.getStyleClass().add("btn-danger");
        rejectBtn.setOnAction(e -> reject());

        HBox actions = new HBox(10, replyField, rejectBtn, fulfillBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox.setVgrow(itemTable, Priority.ALWAYS);
        VBox bottom = new VBox(8, detailHeading, clerkNote, itemTable, errorLabel, actions);
        bottom.setPadding(new Insets(8, 0, 0, 0));

        SplitPane split = new SplitPane(requestTable, bottom);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.42);
        VBox.setVgrow(split, Priority.ALWAYS);

        getChildren().addAll(heading, controls, split);

        onSelect(null);
        refresh();
    }

    private void buildRequestTable() {
        requestTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        requestTable.setPlaceholder(new Label("No requests to show."));
        requestTable.getColumns().add(TableCols.<StockRequestSummary>primary("Request", r -> "#" + r.getId()));
        requestTable.getColumns().add(TableCols.<StockRequestSummary>text("Outlet", StockRequestSummary::getLocationName));
        requestTable.getColumns().add(TableCols.<StockRequestSummary>text("Requested by", StockRequestSummary::getRequestedBy));
        requestTable.getColumns().add(TableCols.<StockRequestSummary>muted("Sent", r -> shortDate(r.getCreatedAt())));
        requestTable.getColumns().add(TableCols.<StockRequestSummary>text("Items", r -> String.valueOf(r.getItemCount())));
        requestTable.getColumns().add(TableCols.<StockRequestSummary>status("Status", StockRequestSummary::getStatus,
                s -> switch (s) {
                    case "Fulfilled" -> "status-ok";
                    case "Rejected" -> "status-out";
                    case "Pending" -> "status-low";
                    default -> "status-quiet";
                }));
        requestTable.getColumns().add(TableCols.<StockRequestSummary>muted("Handled by",
                r -> r.getResolvedBy() == null ? "\u2014" : r.getResolvedBy()));
        requestTable.getSelectionModel().selectedItemProperty()
                .addListener((o, a, b) -> onSelect(b));
    }

    private void buildItemTable() {
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        itemTable.setPlaceholder(new Label("Select a request to see its items."));
        itemTable.getColumns().add(TableCols.<SendRow>muted("Barcode", r -> r.item.getProductCode()));
        itemTable.getColumns().add(TableCols.<SendRow>primary("Product", r -> r.item.getProductName()));
        itemTable.getColumns().add(TableCols.<SendRow>muted("Unit", r -> r.item.getUnit()));
        itemTable.getColumns().add(TableCols.<SendRow>text("Requested", r -> r.item.getRequestedDisplay()));
        itemTable.getColumns().add(TableCols.<SendRow>text("In warehouse", r -> Format.qty(r.available)));
        itemTable.getColumns().add(TableCols.<SendRow>status("Stock", SendRow::stockLabel,
                s -> "Short".equals(s) ? "status-out" : "status-ok"));
        itemTable.getColumns().add(sendColumn());
    }

    private TableColumn<SendRow, SendRow> sendColumn() {
        TableColumn<SendRow, SendRow> col = new TableColumn<>("Send");
        col.setSortable(false);
        col.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            private final TextField field = new TextField();
            private boolean updating;

            {
                field.setPrefWidth(80);
                field.textProperty().addListener((o, a, b) -> {
                    if (updating) return;
                    SendRow r = getItem();
                    if (r != null) r.send = b;
                });
            }

            @Override
            protected void updateItem(SendRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (pendingSelected) {
                    setText(null);
                    updating = true;
                    field.setText(row.send);
                    updating = false;
                    setGraphic(field);
                } else {
                    setGraphic(null);
                    setText(row.item.getSentDisplay());
                }
            }
        });
        return col;
    }

    private static String shortDate(String s) {
        return s != null && s.length() >= 16 ? s.substring(0, 16) : s;
    }

    private void refresh() {
        try {
            requests.setAll(service.allRequests(manager));
            applyFilter();
        } catch (SQLException | RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void applyFilter() {
        String status = statusBox.getValue();
        filtered.setPredicate(r -> status == null || ALL.equals(status) || r.getStatus().equals(status));
        countLabel.setText(filtered.size() + " of " + requests.size() + " requests");
    }

    private void onSelect(StockRequestSummary sel) {
        errorLabel.setText("");
        replyField.clear();
        if (sel == null) {
            pendingSelected = false;
            rows.clear();
            detailHeading.setText("Select a request to answer it.");
            clerkNote.setText("");
            clerkNote.setVisible(false);
            clerkNote.setManaged(false);
            fulfillBtn.setDisable(true);
            rejectBtn.setDisable(true);
            replyField.setDisable(true);
            return;
        }

        pendingSelected = PENDING.equals(sel.getStatus());
        try {
            Map<Integer, Double> stock = service.warehouseStock(manager);
            List<SendRow> built = new ArrayList<>();
            for (RequestItemView item : service.requestItems(manager, sel.getId())) {
                built.add(new SendRow(item, stock.getOrDefault(item.getProductId(), 0.0)));
            }
            rows.setAll(built);
        } catch (SQLException | RuntimeException ex) {
            rows.clear();
            showError(ex.getMessage());
        }

        detailHeading.setText("Request #" + sel.getId() + "  \u00b7  " + sel.getLocationName()
                + "  \u00b7  " + sel.getRequestedBy());

        String text = "";
        if (sel.getNote() != null && !sel.getNote().isBlank()) {
            text = "Outlet note: " + sel.getNote();
        }
        if (!pendingSelected && sel.getResponseNote() != null && !sel.getResponseNote().isBlank()) {
            text += (text.isEmpty() ? "" : "\n") + "Your reply: " + sel.getResponseNote();
        }
        clerkNote.setText(text);
        clerkNote.setVisible(!text.isEmpty());
        clerkNote.setManaged(!text.isEmpty());

        fulfillBtn.setDisable(!pendingSelected);
        rejectBtn.setDisable(!pendingSelected);
        replyField.setDisable(!pendingSelected);
        itemTable.refresh();
    }

    private double parseSend(String text, String productName) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return 0;
        double value;
        try {
            value = Double.parseDouble(t);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid quantity for " + productName + ".");
        }
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("Enter a valid quantity for " + productName + ".");
        }
        return value;
    }

    private void fulfill() {
        errorLabel.setText("");
        StockRequestSummary req = requestTable.getSelectionModel().getSelectedItem();
        if (req == null || !PENDING.equals(req.getStatus())) return;

        List<SendLine> lines = new ArrayList<>();
        try {
            for (SendRow r : rows) {
                double qty = parseSend(r.send, r.item.getProductName());
                if (qty > 0) lines.add(new SendLine(r.item.getProductId(), qty));
            }
        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
            return;
        }
        if (lines.isEmpty()) {
            errorLabel.setText("Enter a quantity to send for at least one item, or reject the request.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Send " + lines.size() + " of " + rows.size() + " items to " + req.getLocationName() + "?\n\n"
              + "Stock moves from the Main Warehouse to the outlet right away.");
        confirm.setHeaderText(null);
        confirm.initOwner(getScene().getWindow());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            service.fulfillRequest(manager, req.getId(), lines, replyField.getText());
        } catch (SQLException | RuntimeException ex) {
            errorLabel.setText("Could not fulfill request: " + ex.getMessage());
            return;
        }
        Alert ok = new Alert(Alert.AlertType.INFORMATION, "Request #" + req.getId() + " fulfilled.");
        ok.setHeaderText(null);
        ok.initOwner(getScene().getWindow());
        ok.showAndWait();
        refresh();
    }

    private void reject() {
        errorLabel.setText("");
        StockRequestSummary req = requestTable.getSelectionModel().getSelectedItem();
        if (req == null || !PENDING.equals(req.getStatus())) return;

        TextInputDialog dialog = new TextInputDialog(replyField.getText());
        dialog.setTitle("Reject request");
        dialog.setHeaderText("Reject request #" + req.getId() + " from " + req.getLocationName() + "?");
        dialog.setContentText("Reason:");
        dialog.initOwner(getScene().getWindow());
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String reason = result.get().trim();
        if (reason.isEmpty()) {
            errorLabel.setText("A reason is required to reject a request.");
            return;
        }
        try {
            service.rejectRequest(manager, req.getId(), reason);
        } catch (SQLException | RuntimeException ex) {
            errorLabel.setText("Could not reject request: " + ex.getMessage());
            return;
        }
        refresh();
    }

    private void showError(String msg) {
        errorLabel.setText(msg == null ? "Something went wrong." : msg);
    }
}