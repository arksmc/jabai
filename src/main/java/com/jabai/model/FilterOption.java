/*
 * this file is a small label and id pair used to fill filter dropdowns, such as the outlet or cashier filter. it lets the history screens share the same filtering code.
 */

package com.jabai.model;

public record FilterOption(Integer id, String label, Integer locationId) {

    @Override
    public String toString() {
        return label;
    }
}