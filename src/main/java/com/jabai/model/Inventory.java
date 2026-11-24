package com.jabai.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Inventory {

    private final int locationId;
    private final String locationName;
    private final List<StockRow> rows;

    public Inventory(int locationId, String locationName, List<StockRow> rows) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.rows = List.copyOf(rows);
    }

    public int getLocationId() {
        return locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public List<StockRow> getRows() {
        return rows;
    }

    public int size() {
        return rows.size();
    }

    public Optional<StockRow> find(int productId) {
        return rows.stream().filter(r -> r.productId() == productId).findFirst();
    }

    public double quantityOf(int productId) {
        return find(productId).map(StockRow::quantity).orElse(0.0);
    }

    public List<StockRow> lowStock() {
        return rows.stream().filter(r -> "Low Stock".equals(r.stockStatus())).toList();
    }

    public List<StockRow> outOfStock() {
        return rows.stream().filter(r -> "Out of Stock".equals(r.stockStatus())).toList();
    }

    public List<StockRow> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) return rows;
        return rows.stream()
                .filter(r -> r.code().toLowerCase().contains(q)
                        || r.name().toLowerCase().contains(q)
                        || r.category().toLowerCase().contains(q))
                .toList();
    }

    public Map<Integer, Double> quantities() {
        Map<Integer, Double> map = new HashMap<>();
        for (StockRow r : rows) {
            map.put(r.productId(), r.quantity());
        }
        return map;
    }
}