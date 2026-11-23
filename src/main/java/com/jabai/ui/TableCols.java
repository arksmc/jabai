package com.jabai.ui;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Label;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

final class TableCols {
    static <S> TableColumn<S, String> chip(String title, Function<S, String> getter) {
        TableColumn<S, String> col = text(title, getter);
        col.setCellFactory(c -> new TableCell<>() {
            private final Label chip = new Label();

            {
                chip.getStyleClass().add("chip");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                chip.setText(item);
                setGraphic(chip);
            }
        });
        return col;
    }
    
    private static final String[] STATUS_CLASSES = {
            "status-ok", "status-low", "status-out", "status-info", "status-muted", "status-quiet"
    };

    private TableCols() {
    }

    static <S> TableColumn<S, String> text(String title, Function<S, String> getter) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> new SimpleStringProperty(getter.apply(cd.getValue())));
        return col;
    }

    static <S> TableColumn<S, String> primary(String title, Function<S, String> getter) {
        return styled(title, getter, "cell-primary");
    }

    static <S> TableColumn<S, String> muted(String title, Function<S, String> getter) {
        return styled(title, getter, "cell-muted");
    }

    private static <S> TableColumn<S, String> styled(String title, Function<S, String> getter,
                                                     String cssClass) {
        TableColumn<S, String> col = text(title, getter);
        col.setCellFactory(c -> new TableCell<>() {
            {
                getStyleClass().add(cssClass);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }
        });
        return col;
    }

    static <S> TableColumn<S, Number> number(String title, ToDoubleFunction<S> getter,
                                             DoubleFunction<String> format) {
        TableColumn<S, Number> col = new TableColumn<>(title);
        col.getStyleClass().add("col-right");
        col.setCellValueFactory(cd -> new SimpleDoubleProperty(getter.applyAsDouble(cd.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            {
                getStyleClass().add("cell-right");
            }

            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : format.apply(value.doubleValue()));
            }
        });
        return col;
    }

    static <S> TableColumn<S, String> status(String title, Function<S, String> getter,
                                             Function<String, String> styleFor) {
        TableColumn<S, String> col = text(title, getter);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll(STATUS_CLASSES);
                if (empty || status == null) {
                    setText(null);
                    return;
                }
                setText(status);
                getStyleClass().add(styleFor.apply(status));
            }
        });
        return col;
    }
}