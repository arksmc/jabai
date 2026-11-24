/*
 * this file represents a place that holds stock, either the main warehouse or an outlet. inventory, sales and requests all point to a location.
 */

package com.jabai.model;

public class Location {
    private final int id;
    private final String name;
    private final String type;

    public Location(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return name;
    }
}