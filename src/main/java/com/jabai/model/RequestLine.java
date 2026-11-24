/*
 * this file represents one product and quantity that a clerk asks the warehouse for. the request stock screen collects these into a stock request.
 */

package com.jabai.model;

public record RequestLine(int productId, double quantity) {
}