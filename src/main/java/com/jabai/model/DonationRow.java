package com.jabai.model;

public record DonationRow(long id, String donorName, String type, long amountCentavos,
                          String productName, String unit, double quantity,
                          String status, String createdAt) {
}