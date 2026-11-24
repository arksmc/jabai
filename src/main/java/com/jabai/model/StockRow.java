/*
 * this file is one row of an inventory list, showing a product and how much of it is in stock at a location. it also works out whether the item is in stock, low stock or out of stock.
 */

package com.jabai.model;

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