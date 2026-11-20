package com.jabai.ui;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

final class TableCols {
    private TableCols() {
    }

    static <S> TableColumn<S, String> text(String title, Function<S, String> getter) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> new SimpleStringProperty(getter.apply(cd.getValue())));
        return col;
    }

    /** Sorts by the real number, displays through the given formatter. */
    static <S> TableColumn<S, Number> number(String title, ToDoubleFunction<S> getter,
                                             DoubleFunction<String> format) {
        TableColumn<S, Number> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> new SimpleDoubleProperty(getter.applyAsDouble(cd.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : format.apply(value.doubleValue()));
            }
        });
        return col;
    }
}