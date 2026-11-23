package com.jabai.ui;

import com.jabai.db.ProductDao;
import com.jabai.model.Product;
import com.jabai.util.Money;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Function;

public class ProductScreen extends VBox {
    private static final String ALL = "All categories";
    private static final int TONES = 4;

    private record Row(Product product, String category, int shown, int total, int tone) {
        boolean isGroup() {
            return product == null;
        }
    }

    private final ProductDao dao = new ProductDao();
    private final ObservableList<Product> data = FXCollections.observableArrayList();
    private final ObservableSet<Integer> checked = FXCollections.observableSet();
    private final List<Integer> visibleIds = new ArrayList<>();
    private final Set<String> collapsed = new HashSet<>();
    private final TreeTableView<Row> table = new TreeTableView<>();

    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryBox = new ComboBox<>();
    private final CheckBox groupToggle = new CheckBox("Group by category");
    private final CheckBox selectAll = new CheckBox();
    private final Label countLabel = new Label();
    private final Button delBtn = new Button();

    public ProductScreen() {
        setSpacing(16);
        setPadding(new Insets(10));

        Label heading = new Label("Products");
        heading.getStyleClass().add("page-title");
        countLabel.getStyleClass().add("muted-label");
        VBox titleBox = new VBox(2, heading, countLabel);

        Button addBtn = new Button("Add product");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setMinHeight(36);
        addBtn.setOnAction(e -> openAddDialog());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, titleBox, headerSpacer, addBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Barcode, name or category");
        searchField.setPrefWidth(300);
        searchField.setMinHeight(36);
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, o, n) -> rebuild());

        categoryBox.getItems().add(ALL);
        categoryBox.setValue(ALL);
        categoryBox.setPrefWidth(200);
        categoryBox.setMinHeight(36);
        categoryBox.setOnAction(e -> rebuild());

        groupToggle.setSelected(true);
        groupToggle.setMinHeight(36);
        groupToggle.setOnAction(e -> rebuild());

        delBtn.getStyleClass().add("btn-danger-outline");
        delBtn.setMinHeight(36);
        delBtn.setOnAction(e -> removeChecked());

        Region controlsSpacer = new Region();
        HBox.setHgrow(controlsSpacer, Priority.ALWAYS);
        HBox controls = new HBox(14,
                field("Search", searchField),
                field("Category", categoryBox),
                groupToggle,
                controlsSpacer,
                delBtn);
        controls.setAlignment(Pos.BOTTOM_LEFT);

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        checked.addListener((SetChangeListener<Integer>) change -> onCheckedChanged());
        selectAll.setOnAction(e -> {
            if (selectAll.isSelected()) {
                checked.addAll(visibleIds);
            } else {
                checked.removeAll(visibleIds);
            }
        });

        getChildren().addAll(header, controls, table);
        refresh();
    }

    private static VBox field(String label, Node control) {
        Label l = new Label(label);
        l.getStyleClass().add("auth-field-label");
        return new VBox(4, l, control);
    }

    private TreeTableColumn<Row, Row> column(String title) {
        TreeTableColumn<Row, Row> col = new TreeTableColumn<>(title);
        col.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getValue()));
        return col;
    }

    private static Comparator<Row> byProduct(Comparator<Product> cmp) {
        return (a, b) -> {
            if (a.isGroup() || b.isGroup()) return 0;
            return cmp.compare(a.product(), b.product());
        };
    }

    private static TreeTableCell<Row, Row> textCell(String cssClass, Function<Product, String> text) {
        return new TreeTableCell<>() {
            {
                getStyleClass().add(cssClass);
            }

            @Override
            protected void updateItem(Row row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null || row.isGroup()) {
                    setText(null);
                    return;
                }
                setText(text.apply(row.product()));
            }
        };
    }

    private static Node groupHeader(Row row) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("swatch", "tone-" + row.tone());
        Label name = new Label(row.category());
        name.getStyleClass().add("group-name");
        String count = row.shown() == row.total()
                ? row.total() + (row.total() == 1 ? " product" : " products")
                : row.shown() + " of " + row.total() + " products";
        Label n = new Label(count);
        n.getStyleClass().add("group-count");
        HBox box = new HBox(10, swatch, name, n);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void buildTable() {
        table.getStyleClass().add("grouped-table");
        table.setShowRoot(false);
        table.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No products match your filters."));

        TreeTableColumn<Row, Row> checkCol = column("");
        checkCol.setGraphic(selectAll);
        checkCol.setSortable(false);
        checkCol.setResizable(false);
        checkCol.setMinWidth(48);
        checkCol.setPrefWidth(48);
        checkCol.setMaxWidth(48);
        checkCol.setCellFactory(c -> new TreeTableCell<>() {
            private final CheckBox box = new CheckBox();

            {
                getStyleClass().add("gt-center");
                box.setOnAction(e -> {
                    Row row = getItem();
                    if (row == null || row.isGroup()) return;
                    int id = row.product().getId();
                    if (box.isSelected()) {
                        checked.add(id);
                    } else {
                        checked.remove(id);
                    }
                });
            }

            @Override
            protected void updateItem(Row row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null || row.isGroup()) {
                    setGraphic(null);
                    return;
                }
                box.setSelected(checked.contains(row.product().getId()));
                setGraphic(box);
            }
        });

        TreeTableColumn<Row, Row> nameCol = column("Name");
        nameCol.getStyleClass().add("col-tree");
        nameCol.setPrefWidth(420);
        nameCol.setComparator(byProduct(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER)));
        nameCol.setCellFactory(c -> new TreeTableCell<>() {
            {
                getStyleClass().add("gt-name");
                setContentDisplay(ContentDisplay.RIGHT);
                setGraphicTextGap(10);
            }

            @Override
            protected void updateItem(Row row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (row.isGroup()) {
                    setText(null);
                    setGraphic(groupHeader(row));
                    return;
                }
                setText(row.product().getName());
                if ("Active".equalsIgnoreCase(row.product().getStatus())) {
                    setGraphic(null);
                } else {
                    Label tag = new Label("Inactive");
                    tag.getStyleClass().add("inactive-tag");
                    setGraphic(tag);
                }
            }
        });

        TreeTableColumn<Row, Row> barcodeCol = column("Barcode");
        barcodeCol.setPrefWidth(200);
        barcodeCol.setComparator(byProduct(Comparator.comparing(Product::getCode)));
        barcodeCol.setCellFactory(c -> textCell("gt-muted", Product::getCode));

        TreeTableColumn<Row, Row> priceCol = column("Buying price");
        priceCol.getStyleClass().add("col-right");
        priceCol.setPrefWidth(160);
        priceCol.setComparator(byProduct(Comparator.comparingLong(Product::getCostPriceCentavos)));
        priceCol.setCellFactory(c -> textCell("gt-right",
                p -> p.getCostPriceCentavos() > 0 ? Money.format(p.getCostPriceCentavos()) : "—"));

        TreeTableColumn<Row, Row> unitCol = column("Unit");
        unitCol.setPrefWidth(120);
        unitCol.setComparator(byProduct(Comparator.comparing(Product::getUnit)));
        unitCol.setCellFactory(c -> textCell("gt-muted", p -> "per " + p.getUnit()));

        table.getColumns().addAll(checkCol, nameCol, barcodeCol, priceCol, unitCol);
        table.setTreeColumn(nameCol);

        table.setRowFactory(tv -> new TreeTableRow<>() {
            @Override
            protected void updateItem(Row row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().removeAll("group-row", "row-checked", "row-inactive",
                        "tone-0", "tone-1", "tone-2", "tone-3");
                if (empty || row == null) return;
                if (row.isGroup()) {
                    getStyleClass().addAll("group-row", "tone-" + row.tone());
                    return;
                }
                if (checked.contains(row.product().getId())) {
                    getStyleClass().add("row-checked");
                }
                if (!"Active".equalsIgnoreCase(row.product().getStatus())) {
                    getStyleClass().add("row-inactive");
                }
            }
        });
    }

    private static TreeItem<Row> leaf(Product p) {
        return new TreeItem<>(new Row(p, p.getCategory(), 0, 0, 0));
    }

    private void rebuild() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String cat = categoryBox.getValue();

        List<Product> shown = data.stream()
                .filter(p -> cat == null || cat.equals(ALL) || p.getCategory().equals(cat))
                .filter(p -> q.isEmpty()
                        || p.getCode().toLowerCase().contains(q)
                        || p.getName().toLowerCase().contains(q)
                        || p.getCategory().toLowerCase().contains(q))
                .sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        visibleIds.clear();
        for (Product p : shown) {
            visibleIds.add(p.getId());
        }
        checked.retainAll(visibleIds);

        TreeItem<Row> root = new TreeItem<>();
        if (groupToggle.isSelected()) {
            List<String> allCats = data.stream()
                    .map(Product::getCategory)
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            Map<String, List<Product>> byCat = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Product p : shown) {
                byCat.computeIfAbsent(p.getCategory(), k -> new ArrayList<>()).add(p);
            }
            for (Map.Entry<String, List<Product>> e : byCat.entrySet()) {
                String key = e.getKey();
                int total = (int) data.stream().filter(p -> p.getCategory().equals(key)).count();
                int tone = allCats.indexOf(key) % TONES;
                TreeItem<Row> group = new TreeItem<>(new Row(null, key, e.getValue().size(), total, tone));
                group.setExpanded(!q.isEmpty() || !collapsed.contains(key));
                group.expandedProperty().addListener((obs, was, now) -> {
                    if (!q.isEmpty()) return;
                    if (now) {
                        collapsed.remove(key);
                    } else {
                        collapsed.add(key);
                    }
                });
                for (Product p : e.getValue()) {
                    group.getChildren().add(leaf(p));
                }
                root.getChildren().add(group);
            }
        } else {
            for (Product p : shown) {
                root.getChildren().add(leaf(p));
            }
        }

        root.setExpanded(true);
        table.setRoot(root);
        table.sort();

        countLabel.setText(shown.size() == data.size()
                ? data.size() + " products"
                : shown.size() + " of " + data.size() + " products");
        onCheckedChanged();
    }

    private void onCheckedChanged() {
        boolean any = !checked.isEmpty();
        delBtn.setText("Remove selected (" + checked.size() + ")");
        delBtn.setVisible(any);
        delBtn.setManaged(any);
        selectAll.setSelected(!visibleIds.isEmpty() && checked.containsAll(visibleIds));
        table.refresh();
    }

    private void openAddDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Add product");

        AddProductScreen form = new AddProductScreen(() -> {
            dialog.close();
            refresh();
        });

        Scene scene = new Scene(form);
        scene.getStylesheets().addAll(getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void removeChecked() {
        List<Product> targets = data.stream().filter(p -> checked.contains(p.getId())).toList();
        if (targets.isEmpty()) return;

        String message = targets.size() == 1
                ? "Remove " + targets.get(0).getName() + "?"
                : "Remove " + targets.size() + " products?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        List<String> kept = new ArrayList<>();
        try {
            for (Product p : targets) {
                if (!dao.delete(p.getId())) {
                    kept.add(p.getName());
                }
            }
        } catch (Exception ex) {
            error(ex.getMessage());
        }

        if (!kept.isEmpty()) {
            String info = kept.size() == 1
                    ? kept.get(0) + " has sales, order or stock history, so it was marked Inactive instead of deleted."
                    : kept.size() + " products have sales, order or stock history, so they were marked Inactive instead of deleted: "
                            + String.join(", ", kept);
            new Alert(Alert.AlertType.INFORMATION, info).show();
        }

        checked.clear();
        refresh();
    }

    private void refresh() {
        try {
            data.setAll(dao.findAll());

            String selected = categoryBox.getValue();
            List<String> cats = data.stream().map(Product::getCategory).distinct().sorted().toList();
            categoryBox.getItems().setAll(ALL);
            categoryBox.getItems().addAll(cats);
            categoryBox.setValue(cats.contains(selected) ? selected : ALL);

            rebuild();
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
}