/*
 * this file is one line in the transaction history, which can be a sale, a cash donation or an item donation. both the clerk and manager transaction screens use it to display and void entries.
 */

package com.jabai.model;

public record TransactionRow(String kind, long id, String createdAt, String recordedBy,
                             String details, long amountCentavos, String status, String voidReason,
                             String locationName) {

    public TransactionRow(String kind, long id, String createdAt, String recordedBy,
                          String details, long amountCentavos, String status, String voidReason) {
        this(kind, id, createdAt, recordedBy, details, amountCentavos, status, voidReason, "");
    }

    public boolean isSale() {
        return "SALE".equals(kind);
    }

    public boolean isCompleted() {
        return "Completed".equals(status);
    }

    public String reference() {
        return (isSale() ? "S-" : "D-") + id;
    }

    public String typeLabel() {
        return switch (kind) {
            case "SALE" -> "Sale";
            case "CASH" -> "Cash donation";
            default -> "Item donation";
        };
    }
}