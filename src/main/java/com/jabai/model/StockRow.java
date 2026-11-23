package com.jabai.model;

/** One product's stock at one location (replaces InventoryScreen.InventoryItem). */
public record StockRow(int productId, String code, String name, String category, String unit,
                       String status, long priceCentavos, double quantity, double reorderLevel) {

    public String stockStatus() {
        if (quantity <= 0) return "Out of Stock";
        if (quantity <= reorderLevel) return "Low Stock";
        return "In Stock";
    }

    public boolean isActive() {
        return "Active".equalsIgnoreCase(status);
    }
}