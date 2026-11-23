package com.jabai.ui;

import com.jabai.model.Clerk;
import com.jabai.model.Manager;
import com.jabai.model.User;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Dashboard for one outlet. Works for a Clerk (their own outlet) or for a Manager
 * who opened a clerk's outlet ("view as clerk"). The acting user is what gets recorded
 * on sales/donations/voids; the services check user.canAccessLocation(locationId).
 */
public class ClerkDashboard extends Dashboard {

    private final User actor;
    private final int locationId;
    private final String locationName;

    /** Normal clerk login: same call shape as before, so Main doesn't change. */
    public ClerkDashboard(Clerk clerk, Runnable onLogout) {
        this(clerk, clerk.getLocationId(), clerk.getLocationName(), onLogout);
    }

    /** General form: any user + the outlet to operate on. */
    public ClerkDashboard(User actor, int locationId, String locationName, Runnable onExit) {
        super(actor,
              actor instanceof Manager ? "Manager (outlet view)" : "Clerk  |  " + locationName,
              actor instanceof Manager ? "Back to Manager" : "Log out",
              onExit);
        this.actor = actor;
        this.locationId = locationId;
        this.locationName = locationName;

        if (actor instanceof Manager) {
            Label l = new Label("Viewing " + locationName + " as Manager. Actions are recorded under your name.");
            l.getStyleClass().add("view-as-banner-text");
            HBox banner = new HBox(l);
            banner.getStyleClass().add("view-as-banner");
            setBanner(banner);
        }

        addSection("SELL");
        addPage("Sales", () -> new SalesScreen(actor, locationId));
        addPage("Transactions", () -> placeholder("Transaction History (with Void)"));
        addSection("OUTLET");
        addPage("Inventory", () -> new InventoryScreen(locationId));
        addPage("Wallet", () -> new WalletScreen(actor, locationId));
        addPage("Request Stock", () -> placeholder("Request Stock"));

        start();
    }

    public User getActor() { return actor; }
    public int getLocationId() { return locationId; }
    public String getLocationName() { return locationName; }

    private Node placeholder(String title) {
        Label t = new Label(title);
        t.getStyleClass().add("page-title");
        Label s = new Label("Coming next. Outlet: " + locationName);
        VBox box = new VBox(8, t, s);
        box.setPadding(new Insets(4));
        return box;
    }
}