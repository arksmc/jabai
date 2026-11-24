/*
 * this file represents one product and quantity that the manager sends from the warehouse to an outlet. the stock requests screen uses it when a request is fulfilled.
 */

package com.jabai.model;

public record SendLine(int productId, double quantity) {
}