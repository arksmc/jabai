/*
 * this file holds the summary of one stock order, such as the supplier, date and total. the stock orders screen shows a list of these.
 */

package com.jabai.model;

public record StockOrderSummary(long id, String supplier, String status, String orderedBy,
                                String createdAt, String receivedAt,
                                int itemCount, long totalCentavos) {
}