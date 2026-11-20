package com.jabai.model;

public record OrderLine(int productId, double quantity, long unitCostCentavos) {
}