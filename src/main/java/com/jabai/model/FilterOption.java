package com.jabai.model;

public record FilterOption(Integer id, String label, Integer locationId) {

    @Override
    public String toString() {
        return label;
    }
}