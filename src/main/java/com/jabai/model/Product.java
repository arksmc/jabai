package com.jabai.model;

public class Product {
    private int id;
    private String code;
    private String name;
    private String category;
    private String unit;
    private String status;
    private double price;        // selling price
    private double costPrice;
    private double reorderLevel;

    public Product(int id, String code, String name, String category, String unit,
                   String status, double price, double costPrice, double reorderLevel) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.status = status;
        this.price = price;
        this.costPrice = costPrice;
        this.reorderLevel = reorderLevel;
    }

    public int getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getUnit() { return unit; }
    public String getStatus() { return status; }
    public double getPrice() { return price; }
    public double getCostPrice() { return costPrice; }
    public double getReorderLevel() { return reorderLevel; }
}