package com.jabai.ui;

import com.jabai.db.ProductDao;
import com.jabai.model.Product;
import com.jabai.util.Format;
import com.jabai.util.Money;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;

/** Form for adding a product, or editing/removing an existing one. */
public class AddProductScreen extends VBox {
    private final ProductDao dao = new ProductDao();
    private final Product existing; // null when adding
    private final Runnable onDone;

    private final TextField name = new TextField();
    private final TextField code = new TextField();
    private final ComboBox<String> category = new ComboBox<>();
    private final ComboBox<String> unit = new ComboBox<>();
    private final ComboBox<String> status = new ComboBox<>();
    private final TextField costPrice = new TextField();
    private final TextField sellingPrice = new TextField();
    private final TextField reorderLevel = new TextField();

    public AddProductScreen(Runnable onDone) {
        this(null, onDone);
    }

    public AddProductScreen(Product existing, Runnable onDone) {
        this.existing = existing;
        this.onDone = onDone;
        boolean editing = existing != null;

        getStyleClass().add("form-root");
        setSpacing(20);
        setPadding(new Insets(24));
        setPrefWidth(520);

        category.getItems().addAll("Rice Grains", "Fruits", "Root Crops", "Vegetables", "Fish",
                "Dairy Products", "Planting Seeds", "Seafood", "Poultry", "Pork", "Beef");
        unit.getItems().addAll("kg", "L", "pc", "packet", "500g");
        status.getItems().addAll("Active", "Inactive");
        status.setValue("Active");
        code.setPromptText("e.g. 4800001000018");

        if (editing) {
            fill(existing);
        }

        Label heading = new Label(editing ? "Edit product" : "Add product");
        heading.getStyleClass().add("page-title");

        // --- fields: two columns ---
        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(16);
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(50);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(50);
        form.getColumnConstraints().addAll(left, right);

        form.add(field("Product name *", name, null), 0, 0, 2, 1);
        form.add(field("Barcode *", code, null), 0, 1);
        form.add(field("Category *", category, null), 1, 1);
        form.add(field("Unit of measure *", unit, null), 0, 2);
        form.add(field("Status", status, null), 1, 2);
        form.add(field("Buying price (₱) *", costPrice, "What the warehouse pays suppliers"), 0, 3);
        form.add(field("Selling price (₱) *", sellingPrice, "Shown to clerks at the till"), 1, 3);
        form.add(field("Reorder level", reorderLevel, null), 0, 4);

        // --- buttons ---
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> onDone.run());

        Button saveBtn = new Button(editing ? "Save changes" : "Save product");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            if (save()) onDone.run();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);
        if (editing) {
            Button removeBtn = new Button("Remove");
            removeBtn.getStyleClass().add("btn-danger-outline");
            removeBtn.setOnAction(e -> remove());
            buttons.getChildren().add(removeBtn);
        }
        buttons.getChildren().addAll(spacer, cancelBtn, saveBtn);

        getChildren().addAll(heading, form, buttons);
    }

    private static VBox field(String labelText, Control control, String hint) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        control.getStyleClass().add("form-field");
        control.setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox(6, label, control);
        if (hint != null) {
            Label h = new Label(hint);
            h.getStyleClass().add("form-hint");
            box.getChildren().add(h);
        }
        return box;
    }

    private void fill(Product p) {
        if (!category.getItems().contains(p.getCategory())) category.getItems().add(p.getCategory());
        if (!unit.getItems().contains(p.getUnit())) unit.getItems().add(p.getUnit());

        name.setText(p.getName());
        code.setText(p.getCode());
        category.setValue(p.getCategory());
        unit.setValue(p.getUnit());
        status.setValue(p.getStatus());
        costPrice.setText(BigDecimal.valueOf(p.getCostPriceCentavos(), 2).toPlainString());
        sellingPrice.setText(BigDecimal.valueOf(p.getPriceCentavos(), 2).toPlainString());
        reorderLevel.setText(Format.qty(p.getReorderLevel()));
    }

    private boolean save() {
        try {
            String n = name.getText().trim();
            String c = code.getText().trim();
            if (n.isEmpty() || c.isEmpty() || category.getValue() == null || unit.getValue() == null
                    || costPrice.getText().isBlank() || sellingPrice.getText().isBlank()) {
                error("Please fill in all required fields (*).");
                return false;
            }

            boolean taken = existing == null
                    ? dao.codeExists(c)
                    : dao.codeExistsForOther(c, existing.getId());
            if (taken) {
                error("Product code must be unique. \"" + c + "\" already exists.");
                return false;
            }

            long cost = Money.toCentavos(costPrice.getText());
            long price = Money.toCentavos(sellingPrice.getText());
            double reorder = reorderLevel.getText().isBlank() ? 0 : Double.parseDouble(reorderLevel.getText().trim());

            if (price < 0 || cost < 0 || reorder < 0) {
                error("Prices and reorder level cannot be negative.");
                return false;
            }

            Product p = new Product(existing == null ? 0 : existing.getId(), c, n,
                    category.getValue(), unit.getValue(), status.getValue(), price, cost, reorder);
            if (existing == null) {
                dao.add(p);
            } else {
                dao.update(p);
            }
            return true;
        } catch (NumberFormatException | ArithmeticException ex) {
            error("Prices and reorder level must be valid numbers.");
        } catch (Exception ex) {
            error(ex.getMessage());
        }
        return false;
    }

    private void remove() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove " + existing.getName() + "?");
        confirm.initOwner(getScene().getWindow());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            boolean deleted = dao.delete(existing.getId());
            if (!deleted) {
                Alert info = new Alert(Alert.AlertType.INFORMATION, existing.getName()
                        + " has sales, order or stock history, so it was marked Inactive instead of deleted.");
                info.initOwner(getScene().getWindow());
                info.showAndWait();
            }
            onDone.run();
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void error(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.initOwner(getScene().getWindow());
        alert.show();
    }
}