package com.jabai.ui;

import com.jabai.model.Manager;

public class ManagerDashboard extends Dashboard {

    public ManagerDashboard(Manager manager, Runnable onLogout) {
        super(manager, "Manager", "Log out", onLogout);

        addSection("MANAGE");
        addPage("Products", ProductScreen::new);
        addPage("Inventory", InventoryScreen::new);
        addSection("WAREHOUSE");
        addPage("Stock Orders", () -> new StockOrdersScreen(manager));
        addPage("Stock Requests", () -> new StockRequestsScreen(manager));
        addSection("MONITOR");
        addPage("Sale History", () -> new SaleHistoryScreen(manager));

        start();
    }
}