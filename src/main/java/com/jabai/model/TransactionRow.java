package com.jabai.model;

public record TransactionRow(String kind, long id, String createdAt, String recordedBy,
                             String details, long amountCentavos, String status, String voidReason) {

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