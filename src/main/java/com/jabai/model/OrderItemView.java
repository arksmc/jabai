package com.jabai.model;

public record OrderItemView(String code, String name, String unit,
                            double quantity, long unitCostCentavos) {
    public long lineTotalCentavos() {
        return Math.round(quantity * unitCostCentavos);
    }
}