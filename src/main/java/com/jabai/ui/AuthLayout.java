package com.jabai.ui;

import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.image.ImageView;

/** Split screen used before login: brand/hero on the left, the form on the right. */
public class AuthLayout extends HBox {

    private final StackPane formPanel = new StackPane();
    private final Button themeToggle = ThemeManager.createToggleButton("theme-toggle");

    protected AuthLayout() {
        getStyleClass().add("auth-root");

        VBox hero = buildHero();
        HBox.setHgrow(hero, Priority.ALWAYS);

        formPanel.getStyleClass().add("auth-form-panel");
        formPanel.setAlignment(Pos.CENTER);
        formPanel.setMinWidth(420);
        formPanel.setPrefWidth(480);
        formPanel.setMaxWidth(480);
        
        StackPane.setAlignment(themeToggle, Pos.TOP_RIGHT);
        StackPane.setMargin(themeToggle, new Insets(16, 0, 0, 0));

        getChildren().addAll(hero, formPanel);
    }

    /** Subclasses call this once with the content for the right-hand side. */
    protected final void setForm(Region content) {
        content.setMaxWidth(360);
        content.setMaxHeight(Region.USE_PREF_SIZE); // keep the form vertically centered
        formPanel.getChildren().setAll(content, themeToggle);
    }

    // ---------- Shared pieces for the form side ----------

    /** Title row. Pass null for onBack to hide the back arrow. */
    static HBox header(String title, Runnable onBack) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        if (onBack != null) {
            Button back = new Button("←");
            back.getStyleClass().add("auth-back");
            back.setOnAction(e -> onBack.run());
            row.getChildren().add(back);
        }
        Label t = new Label(title);
        t.getStyleClass().add("auth-title");
        row.getChildren().add(t);
        return row;
    }

    /** Filled pill button (green, or blue on clerk screens). */
    static Button primaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("auth-primary-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    /** Outlined blue pill button. */
    static Button outlineButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("auth-outline-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    // ---------- Left side ----------

    private VBox buildHero() {
    	Node logo = buildLogo();

        Label line1 = new Label("Fresh stock,");
        Label line2 = new Label("every outlet.");
        line1.getStyleClass().add("auth-headline");
        line2.getStyleClass().addAll("auth-headline", "auth-headline-accent");
        VBox headline = new VBox(line1, line2);

        StackPane art = buildArt();
        VBox.setVgrow(art, Priority.ALWAYS);

        VBox hero = new VBox(logo, art, headline);
        hero.getStyleClass().add("auth-hero");
        hero.setAlignment(Pos.TOP_LEFT);
        return hero;
    }

    /** A few overlapping mock cards that hint at what the app does. */
    private StackPane buildArt() {
        VBox warehouse = card("Main Warehouse", "Sinandomeng", "200 kg", "In Stock", "status-ok");
        VBox outlet = card("Outlet 1", "Galunggong", "10 kg", "Low Stock", "status-low");
        VBox order = card("Stock order 2611230001", "Benguet Farms Coop", "12 items", "Ordered", "status-info");

        warehouse.setTranslateX(-60);
        warehouse.setTranslateY(-100);
        outlet.setTranslateX(60);
        outlet.setTranslateY(0);
        order.setTranslateX(-30);
        order.setTranslateY(100);

        StackPane art = new StackPane(warehouse, outlet, order);
        art.setAlignment(Pos.CENTER);
        return art;
    }

    private VBox card(String where, String product, String qty, String statusText, String statusStyle) {
        Label w = new Label(where);
        w.getStyleClass().add("auth-card-meta");
        Label p = new Label(product);
        p.getStyleClass().add("auth-card-title");
        Label q = new Label(qty);
        q.getStyleClass().add("auth-card-meta");
        Label status = new Label(statusText);
        status.getStyleClass().add(statusStyle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, q, spacer, status);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(6, w, p, row);
        card.getStyleClass().add("auth-card");
        card.setPrefWidth(250);
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return card;
    }
    
    /** Loads the logo (dark version in dark mode); falls back to the letter badge if the file isn't there. */
    private Node buildLogo() {
        ImageView view = ThemeManager.createLogo(48);
        if (view == null) {
            Label fallback = new Label("B");
            fallback.getStyleClass().add("auth-logo");
            return fallback;
        }
        return view;
    }
}