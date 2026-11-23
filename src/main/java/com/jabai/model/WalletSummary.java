package com.jabai.model;

public record WalletSummary(long salesCentavos, long cashDonationsCentavos) {
    public long totalCentavos() {
        return salesCentavos + cashDonationsCentavos;
    }
}