package com.jabai.model;

public record StockOrderSummary(long id, String supplier, String status, String orderedBy,
                                String createdAt, String receivedAt,
                                int itemCount, long totalCentavos) {
}