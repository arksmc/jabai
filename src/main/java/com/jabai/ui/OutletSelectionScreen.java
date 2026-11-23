package com.jabai.ui;

import com.jabai.db.LocationDao;
import com.jabai.model.Location;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public class OutletSelectionScreen extends AuthLayout {
    private final LocationDao dao = new LocationDao();

    public OutletSelectionScreen(Runnable onBack, Consumer<Location> onSelect) {
        VBox list = new VBox(10);
        list.setPadding(new Insets(2));

        try {
            List<Location> outlets = dao.findOutlets();
            if (outlets.isEmpty()) {
                list.getChildren().add(new Label("No outlets available."));
            }
            for (Location outlet : outlets) {
                Button b = outlineButton(outlet.getName());
                b.setOnAction(e -> onSelect.accept(outlet));
                list.getChildren().add(b);
            }
        } catch (SQLException ex) {
            Label err = new Label("Failed to load outlets: " + ex.getMessage());
            err.getStyleClass().add("auth-error");
            err.setWrapText(true);
            list.getChildren().add(err);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(320);
        scroll.getStyleClass().add("auth-scroll");

        Label hint = new Label("Choose the outlet you're working at.");
        hint.getStyleClass().add("auth-subtitle");

        setForm(new VBox(14, header("Select your outlet", onBack), hint, scroll));
    }
}