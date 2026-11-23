package com.jabai.service;

import com.jabai.db.Database;
import com.jabai.db.InventoryDao;
import com.jabai.db.SaleDao;
import com.jabai.model.MovementReason;
import com.jabai.model.SaleLine;
import com.jabai.model.User;
import com.jabai.model.Product;
import com.jabai.model.SaleRequestLine;
import com.jabai.db.ProductDao;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SaleService {
    private final SaleDao saleDao = new SaleDao();
    private final InventoryDao inventoryDao = new InventoryDao();
    private final ProductDao productDao = new ProductDao();

    /** Records a sale and deducts stock in one transaction. Returns the new sale id. */
    /** Records a sale and deducts stock in one transaction. Prices come from the DB, not the UI. */
    public long recordSale(User actor, int locationId, List<SaleRequestLine> requested,
                           long cashTenderedCentavos) throws SQLException {
        if (!actor.canAccessLocation(locationId)) {
            throw new SecurityException("You can't record sales for another outlet.");
        }
        if (requested.isEmpty()) {
            throw new IllegalArgumentException("Sale has no items.");
        }

        // merge duplicate products and validate quantities
        Map<Integer, Double> merged = new LinkedHashMap<>();
        for (SaleRequestLine r : requested) {
            if (!Double.isFinite(r.quantity()) || r.quantity() <= 0) {
                throw new IllegalArgumentException("Quantities must be greater than zero.");
            }
            merged.merge(r.productId(), r.quantity(), Double::sum);
        }

        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                List<SaleLine> lines = new ArrayList<>();
                for (var e : merged.entrySet()) {
                    Product p = productDao.findById(c, e.getKey())
                            .orElseThrow(() -> new IllegalArgumentException("Product not found."));
                    if (!"Active".equalsIgnoreCase(p.getStatus())) {
                        throw new IllegalStateException(p.getName() + " is no longer for sale.");
                    }
                    lines.add(new SaleLine(p.getId(), e.getValue(), p.getPriceCentavos()));
                }

                long total = lines.stream().mapToLong(SaleLine::lineTotalCentavos).sum();
                if (cashTenderedCentavos < total) {
                    throw new IllegalArgumentException("Cash tendered is less than the total.");
                }

                long saleId = saleDao.insertSale(c, locationId, actor.getId(),
                        total, cashTenderedCentavos, cashTenderedCentavos - total);
                for (SaleLine line : lines) {
                    saleDao.insertItem(c, saleId, line);
                    inventoryDao.decrease(c, locationId, line.productId(), line.quantity(),
                            MovementReason.SALE, saleId, actor.getId());
                }
                c.commit();
                return saleId;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Voids a completed sale: marks it Voided, puts the stock back and logs a VOID movement,
     * all in one transaction. The actor must be allowed to access the sale's outlet.
     */
    public void voidSale(User actor, long saleId, String reason) throws SQLException {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to void a sale.");
        }
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                SaleDao.Header h = saleDao.findHeader(c, saleId)
                        .orElseThrow(() -> new IllegalArgumentException("Sale #" + saleId + " not found."));
                if (!actor.canAccessLocation(h.locationId())) {
                    throw new SecurityException("You can't void sales from another outlet.");
                }
                if (!saleDao.markVoided(c, saleId, actor.getId(), reason.trim())) {
                    throw new IllegalStateException("Sale #" + saleId + " is already voided.");
                }
                for (SaleDao.Item it : saleDao.findItems(c, saleId)) {
                    inventoryDao.increase(c, h.locationId(), it.productId(), it.quantity(),
                            MovementReason.VOID, saleId, actor.getId());
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
    
    
}