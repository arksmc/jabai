/*
 * this file is one donation shown in a list, with the donor, type, amount or item and date. the wallet screen and transaction screens use it to display donations.
 */

package com.jabai.model;

public record DonationRow(long id, String donorName, String type, long amountCentavos,
                          String productName, String unit, double quantity,
                          String status, String createdAt) {
}