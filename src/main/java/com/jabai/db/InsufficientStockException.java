/*
 * this file is the error thrown when someone tries to take out more stock than a location has. services catch it so the screen can show a friendly message instead of crashing.
 */

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