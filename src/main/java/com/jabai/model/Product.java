package com.jabai.model;

public class Product {
    private int id;
    private String name;
    private String category;
    private String description;
    private double price;
    private String unit;

    public Product(int id, String name, String category, String description, double price, String unit) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.unit = unit;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getUnit() { return unit; }
}