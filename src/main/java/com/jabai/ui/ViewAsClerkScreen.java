package com.jabai.ui;

import com.jabai.db.LocationDao;
import com.jabai.model.Location;
import com.jabai.model.Manager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class ViewAsClerkScreen extends VBox {

    private final Manager manager;
    private final Runnable onLogout;
    private final LocationDao locationDao = new LocationDao();

    public ViewAsClerkScreen(Manager manager, Runnable onLogout) {
        super(16);
        this.manager = manager;
        this.onLogout = onLogout;
        setPadding(new Insets(10));

        Label title = new Label("View as Clerk");
        title.getStyleClass().add("page-title");

        Label caption = new Label("Pick an outlet to open its clerk dashboard. Actions are recorded under your manager account.");
        caption.getStyleClass().add("muted-label");
        caption.setWrapText(true);

        Label error = new Label();
        error.getStyleClass().add("auth-error");
        error.setWrapText(true);
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());

        FlowPane outletsPane = new FlowPane(12, 12);

        try {
            List<Location> outlets = locationDao.findOutlets();
            if (outlets.isEmpty()) {
                outletsPane.getChildren().add(new Label("No outlets available."));
            }
            for (Location outlet : outlets) {
                Button open = new Button(outlet.getName());
                open.getStyleClass().add("btn-primary");
                open.setOnAction(e -> openClerkDashboard(outlet));
                outletsPane.getChildren().add(open);
            }
        } catch (SQLException ex) {
            error.setText("Failed to load outlets: " + ex.getMessage());
        }

        getChildren().addAll(title, caption, error, outletsPane);
    }

    private void openClerkDashboard(Location outlet) {
        Scene scene = getScene();
        if (scene == null) return;
        scene.setRoot(new ClerkDashboard(manager, outlet.getId(), outlet.getName(),
                () -> scene.setRoot(new ManagerDashboard(manager, onLogout))));
    }
}