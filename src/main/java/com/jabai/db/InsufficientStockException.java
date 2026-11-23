package com.jabai.db;

public class InsufficientStockException extends RuntimeException {
    private final int productId;

    public InsufficientStockException(int productId, String message) {
        super(message);
        this.productId = productId;
    }

    public InsufficientStockException(int productId) {
        this(productId, "Not enough stock for product id " + productId);
    }

    public int getProductId() {
        return productId;
    }
}