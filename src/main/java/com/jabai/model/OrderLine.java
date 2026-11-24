/*
 * this file represents one product, quantity and unit cost inside a stock order. neworderform creates these and stockservice saves them.
 */

package com.jabai.model;

public record OrderLine(int productId, double quantity, long unitCostCentavos) {
}