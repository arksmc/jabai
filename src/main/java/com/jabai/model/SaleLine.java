package com.jabai.model;

public record SaleLine(int productId, double quantity, long unitPriceCentavos) {
    public long lineTotalCentavos() {
        return Math.round(quantity * unitPriceCentavos);
    }
}