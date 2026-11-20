package com.jabai.model;

import com.jabai.util.Money;

public class Product {
    private final int id;
    private final String code;
    private final String name;
    private final String category;
    private final String unit;
    private final String status;
    private final long priceCentavos;
    private final long costPriceCentavos;
    private final double reorderLevel;

    public Product(int id, String code, String name, String category, String unit,
                   String status, long priceCentavos, long costPriceCentavos, double reorderLevel) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.status = status;
        this.priceCentavos = priceCentavos;
        this.costPriceCentavos = costPriceCentavos;
        this.reorderLevel = reorderLevel;
    }

    public int getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getUnit() { return unit; }
    public String getStatus() { return status; }
    public long getPriceCentavos() { return priceCentavos; }
    public long getCostPriceCentavos() { return costPriceCentavos; }
    public double getReorderLevel() { return reorderLevel; }

    // for table display
    public String getPriceDisplay() { return Money.format(priceCentavos); }
    public String getCostPriceDisplay() { return Money.format(costPriceCentavos); }
}