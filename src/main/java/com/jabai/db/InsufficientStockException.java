package com.jabai.db;

public class InsufficientStockException extends RuntimeException {
    private final int productId;

    public InsufficientStockException(int productId) {
        super("Not enough stock for product id " + productId);
        this.productId = productId;
    }

    public int getProductId() {
        return productId;
    }
}