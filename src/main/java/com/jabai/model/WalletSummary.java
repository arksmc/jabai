/*
 * this file holds the totals shown in an outlet's wallet, such as sales and cash donations. the wallet screen uses it so the balance is calculated in one place.
 */

package com.jabai.model;

public record WalletSummary(long salesCentavos, long cashDonationsCentavos) {
    public long totalCentavos() {
        return salesCentavos + cashDonationsCentavos;
    }
}