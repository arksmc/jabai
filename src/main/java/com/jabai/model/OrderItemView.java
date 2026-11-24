/*
 * this file is a read-only view of one item in a saved stock order, used to show the order's details on screen. it exists so the screen does not need to deal with raw database rows.
 */

package com.jabai.model;

public record OrderItemView(String code, String name, String unit,
                            double quantity, long unitCostCentavos) {
    public long lineTotalCentavos() {
        return Math.round(quantity * unitCostCentavos);
    }
}