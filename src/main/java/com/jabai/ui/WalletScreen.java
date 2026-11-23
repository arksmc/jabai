package com.jabai.ui;

import com.jabai.db.InventoryDao;
import com.jabai.model.DonationRow;
import com.jabai.model.StockRow;
import com.jabai.model.User;
import com.jabai.model.WalletSummary;
import com.jabai.service.DonationService;
import com.jabai.util.Format;
import com.jabai.util.Money;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class WalletScreen extends HBox {

    private static final Set<String> WHOLE_UNITS = Set.of("pc", "packet");
    private static final int RECENT_LIMIT = 200;

    private final User actor;
    private final int locationId;
    private final DonationService service = new DonationService();
    private final InventoryDao inventoryDao = new InventoryDao();

    private final Label balanceLabel = new Label(Money.format(0));
    private final Label breakdownLabel = new Label();
    private final Label pageError = errorLabel();

    private final TextField cashDonorField = new TextField();
    private final TextField cashAmountField = new TextField();
    private final Label cashError = errorLabel();

    private final TextField itemDonorField = new TextField();
    private final ComboBox<StockRow> productBox = new ComboBox<>();
    private final TextField itemQtyField = new TextField();
    private final Label itemError = errorLabel();

    private final ObservableList<DonationRow> donations = FXCollections.observableArrayList();
    private final TableView<DonationRow> table = new TableView<>();

    public WalletScreen(User actor, int locationId) {
        this.actor = actor;
        this.locationId = locationId;
        setSpacing(16);
        setPadding(new Insets(10));

        VBox right = buildHistory();
        HBox.setHgrow(right, Priority.ALWAYS);
        getChildren().addAll(buildLeft(), right);

        loadProducts();
        refresh();
    }

    private static Label errorLabel() {
        Label l = new Label();
        l.getStyleClass().add("auth-error");
        l.setWrapText(true);
        l.visibleProperty().bind(l.textProperty().isNotEmpty());
        l.managedProperty().bind(l.visibleProperty());
        return l;
    }

    private ScrollPane buildLeft() {
        Label title = new Label("Wallet");
        title.getStyleClass().add("page-title");

        Label balanceCaption = new Label("WALLET BALANCE");
        balanceCaption.getStyleClass().add("muted-label");
        balanceLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        breakdownLabel.getStyleClass().add("muted-label");
        breakdownLabel.setWrapText(true);

        Label cashCaption = new Label("RECORD CASH DONATION");
        cashCaption.getStyleClass().add("muted-label");
        cashDonorField.setPromptText("Donor name");
        cashAmountField.setPromptText("Amount");
        cashAmountField.setOnAction(e -> recordCash());
        Button cashBtn = new Button("Record cash donation");
        cashBtn.getStyleClass().add("btn-primary");
        cashBtn.setMaxWidth(Double.MAX_VALUE);
        cashBtn.setOnAction(e -> recordCash());

        Label itemCaption = new Label("RECORD ITEM DONATION");
        itemCaption.getStyleClass().add("muted-label");
        itemDonorField.setPromptText("Donor name");
        productBox.setPromptText("Select product");
        productBox.setMaxWidth(Double.MAX_VALUE);
        productBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(StockRow r) {
                return r == null ? "" : r.name() + " (" + r.unit() + ")";
            }

            @Override
            public StockRow fromString(String s) {
                return null;
            }
        });
        itemQtyField.setPromptText("Quantity");
        itemQtyField.setOnAction(e -> recordItem());
        Button itemBtn = new Button("Record item donation");
        itemBtn.getStyleClass().add("btn-primary");
        itemBtn.setMaxWidth(Double.MAX_VALUE);
        itemBtn.setOnAction(e -> recordItem());

        VBox content = new VBox(10,
                title,
                balanceCaption, balanceLabel, breakdownLabel, pageError,
                cashCaption, cashDonorField, cashAmountField, cashError, cashBtn,
                itemCaption, itemDonorField, productBox, itemQtyField, itemError, itemBtn);
        content.setPadding(new Insets(0, 8, 0, 0));

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPrefWidth(380);
        sp.setMinWidth(380);
        sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    private VBox buildHistory() {
        Label caption = new Label("RECENT DONATIONS");
        caption.getStyleClass().add("muted-label");

        table.setItems(donations);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No donations yet."));
        table.getColumns().add(TableCols.<DonationRow>muted("Date", DonationRow::createdAt));
        table.getColumns().add(TableCols.<DonationRow>primary("Donor", DonationRow::donorName));
        table.getColumns().add(TableCols.<DonationRow>chip("Type",
                r -> "CASH".equals(r.type()) ? "Cash" : "Item"));
        table.getColumns().add(TableCols.<DonationRow>text("Details", WalletScreen::details));
        table.getColumns().add(TableCols.<DonationRow>status("Status", DonationRow::status,
                s -> "Completed".equals(s) ? "status-ok" : "status-muted"));
        VBox.setVgrow(table, Priority.ALWAYS);

        return new VBox(12, caption, table);
    }

    private static String details(DonationRow r) {
        if ("CASH".equals(r.type())) return Money.format(r.amountCentavos());
        return Format.qty(r.quantity()) + " " + r.unit() + " " + r.productName();
    }

    private void loadProducts() {
        try {
            List<StockRow> rows = inventoryDao.findStock(locationId).stream()
                    .filter(StockRow::isActive).toList();
            productBox.setItems(FXCollections.observableArrayList(rows));
        } catch (SQLException e) {
            itemError.setText("Failed to load products: " + e.getMessage());
        }
    }

    private void refresh() {
        pageError.setText("");
        try {
            WalletSummary w = service.walletSummary(actor, locationId);
            balanceLabel.setText(Money.format(w.totalCentavos()));
            breakdownLabel.setText("Sales " + Money.format(w.salesCentavos())
                    + "  |  Cash donations " + Money.format(w.cashDonationsCentavos()));
            donations.setAll(service.recentDonations(actor, locationId, RECENT_LIMIT));
        } catch (SQLException | RuntimeException e) {
            pageError.setText("Failed to load wallet: " + e.getMessage());
        }
    }

    private void recordCash() {
        cashError.setText("");
        String amountText = cashAmountField.getText() == null ? "" : cashAmountField.getText().trim();
        if (amountText.isEmpty()) {
            cashError.setText("Enter the donation amount.");
            return;
        }
        long amount;
        try {
            amount = Money.toCentavos(amountText);
        } catch (NumberFormatException | ArithmeticException ex) {
            cashError.setText("Enter a valid amount.");
            return;
        }
        if (amount <= 0) {
            cashError.setText("Amount must be greater than zero.");
            return;
        }
        String donor = cashDonorField.getText() == null ? "" : cashDonorField.getText().trim();
        try {
            long id = service.recordCashDonation(actor, locationId, donor, amount);
            new Alert(Alert.AlertType.INFORMATION,
                    "Cash donation #" + id + " recorded.\n\nDonor: " + donor
                            + "\nAmount: " + Money.format(amount)).showAndWait();
            cashDonorField.clear();
            cashAmountField.clear();
            refresh();
        } catch (SQLException | RuntimeException ex) {
            cashError.setText("Could not record donation: " + ex.getMessage());
        }
    }

    private void recordItem() {
        itemError.setText("");
        StockRow row = productBox.getValue();
        if (row == null) {
            itemError.setText("Select a product.");
            return;
        }
        String qtyText = itemQtyField.getText() == null ? "" : itemQtyField.getText().trim();
        double qty;
        try {
            qty = Double.parseDouble(qtyText);
        } catch (NumberFormatException ex) {
            itemError.setText("Enter a valid quantity.");
            return;
        }
        if (!Double.isFinite(qty) || qty <= 0) {
            itemError.setText("Quantity must be greater than zero.");
            return;
        }
        if (WHOLE_UNITS.contains(row.unit().toLowerCase()) && qty != Math.rint(qty)) {
            itemError.setText(row.name() + " is counted by the " + row.unit() + " (whole numbers only).");
            return;
        }
        String donor = itemDonorField.getText() == null ? "" : itemDonorField.getText().trim();
        try {
            long id = service.recordItemDonation(actor, locationId, donor, row.productId(), qty);
            new Alert(Alert.AlertType.INFORMATION,
                    "Item donation #" + id + " recorded.\n\nDonor: " + donor
                            + "\nItem: " + Format.qty(qty) + " " + row.unit() + " " + row.name()
                            + "\n\nThe outlet's stock has been updated.").showAndWait();
            itemDonorField.clear();
            itemQtyField.clear();
            productBox.setValue(null);
            refresh();
        } catch (SQLException | RuntimeException ex) {
            itemError.setText("Could not record donation: " + ex.getMessage());
        }
    }
}