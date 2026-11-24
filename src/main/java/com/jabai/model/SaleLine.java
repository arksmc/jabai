/*
 * this file represents one product and quantity inside a sale that is being recorded. the sales screen builds a list of these and sends it to salesservice.
 */

package com.jabai.model;

public record SaleLine(int productId, double quantity, long unitPriceCentavos) {
    public long lineTotalCentavos() {
        return Math.round(quantity * unitPriceCentavos);
    }
}