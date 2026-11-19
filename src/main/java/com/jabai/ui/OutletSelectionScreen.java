package com.jabai.ui;

import com.jabai.db.LocationDao;
import com.jabai.model.Location;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public class OutletSelectionScreen extends VBox {
    private final LocationDao dao = new LocationDao();

    public OutletSelectionScreen(Runnable onBack, Consumer<Location> onSelect) {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(40));
        setStyle("-fx-background-color: #f1f8f2;");

        Label title = new Label("Select your outlet");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1565c0;");

        VBox list = new VBox(10);
        list.setAlignment(Pos.CENTER);
        list.setPadding(new Insets(4));

        try {
            List<Location> outlets = dao.findOutlets();
            if (outlets.isEmpty()) {
                list.getChildren().add(new Label("No outlets available."));
            }
            for (Location outlet : outlets) {
                Button b = new Button(outlet.getName());
                b.setPrefSize(320, 48);
                b.setStyle("-fx-background-color: #1565c0; -fx-text-fill: white; "
                        + "-fx-font-size: 16px; -fx-background-radius: 8; -fx-cursor: hand;");
                b.setOnAction(e -> onSelect.accept(outlet));
                list.getChildren().add(b);
            }
        } catch (SQLException ex) {
            list.getChildren().add(new Label("Failed to load outlets: " + ex.getMessage()));
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setMaxSize(380, 320);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> onBack.run());

        getChildren().addAll(title, scroll, backBtn);
    }
}