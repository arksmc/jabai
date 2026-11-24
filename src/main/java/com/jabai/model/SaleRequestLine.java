/*
 * this file represents one product and quantity requested for a sale before it is saved. it carries the clerk's input from the sales screen to the sale service.
 */

package com.jabai.model;

public record SaleRequestLine(int productId, double quantity) {}