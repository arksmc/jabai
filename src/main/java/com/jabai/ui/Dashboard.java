package com.jabai.ui;

import com.jabai.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Base class for every dashboard: sidebar, nav buttons, content switching, exit button.
 *
 * Usage in a subclass constructor:
 *   super(user, "Role label", "Log out", onExit);
 *   addSection("MANAGE");
 *   addPage("Products", ProductScreen::new);
 *   ...
 *   start();   // must be the LAST call
 *
 * Pages are built lazily by their Supplier each time they are opened, so screens always show fresh data.
 */
public abstract class Dashboard extends BorderPane {

    private final VBox sidebar = new VBox(4);
    private final Map<Button, Supplier<Node>> pages = new LinkedHashMap<>();
    private final User user;
    private final String roleLabel;
    private final String exitText;
    private final Runnable onExit;

    protected Dashboard(User user, String roleLabel, String exitText, Runnable onExit) {
        this.user = user;
        this.roleLabel = roleLabel;
        this.exitText = exitText;
        this.onExit = onExit;

        getStyleClass().add("app-root");
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(230);
        sidebar.getChildren().add(brandRow());
        setLeft(sidebar);
    }

    // ---------- building the sidebar ----------

    protected void addSection(String title) {
        Label l = new Label(title);
        l.getStyleClass().add("sidebar-section");
        sidebar.getChildren().add(l);
    }

    /** Adds a nav button that opens the page produced by the supplier. */
    protected Button addPage(String label, Supplier<Node> factory) {
        Button b = new Button(label);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> open(b));
        pages.put(b, factory);
        sidebar.getChildren().add(b);
        return b;
    }

    /** Optional strip above the content (e.g. the "viewing as" banner). */
    protected void setBanner(Node banner) {
        setTop(banner);
    }

    /** Finishes the sidebar (user info, exit button) and opens the first page. Call last. */
    protected void start() {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label who = new Label(user.getFullName());
        who.getStyleClass().add("sidebar-user");
        Label role = new Label(roleLabel);
        role.getStyleClass().add("sidebar-role");

        Button exit = new Button(exitText);
        exit.getStyleClass().add("sidebar-logout");
        exit.setMaxWidth(Double.MAX_VALUE);
        exit.setOnAction(e -> onExit.run());

        sidebar.getChildren().addAll(spacer, who, role, exit);

        if (!pages.isEmpty()) open(pages.keySet().iterator().next());
    }

    // ---------- navigation ----------

    protected void open(Button navButton) {
        for (Button b : pages.keySet()) b.getStyleClass().remove("nav-button-active");
        navButton.getStyleClass().add("nav-button-active");
        Node content = pages.get(navButton).get();
        BorderPane.setMargin(content, new Insets(24));
        setCenter(content);
    }

    protected User getUser() {
        return user;
    }

    private static Node brandRow() {
        Label brand = new Label("BAGSAKAN");
        brand.getStyleClass().add("sidebar-brand");
        var url = Dashboard.class.getResource("/images/logo.png");
        if (url == null) return brand;
        ImageView logo = new ImageView(new Image(url.toExternalForm(), 0, 34, true, true));
        HBox row = new HBox(8, logo, brand);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 12, 8));
        return row;
    }
}