package com.jabai.service;

import com.jabai.db.Database;
import com.jabai.db.InventoryDao;
import com.jabai.db.SaleDao;
import com.jabai.model.SaleLine;
import com.jabai.model.MovementReason;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SaleService {
    private final SaleDao saleDao = new SaleDao();
    private final InventoryDao inventoryDao = new InventoryDao();

    /** Records a sale and deducts stock in one transaction. Returns the new sale id. */
    public long recordSale(int locationId, int userId, List<SaleLine> lines,
                           long cashTenderedCentavos) throws SQLException {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Sale has no items.");
        }
        long total = lines.stream().mapToLong(SaleLine::lineTotalCentavos).sum();
        if (cashTenderedCentavos < total) {
            throw new IllegalArgumentException("Cash tendered is less than the total.");
        }

        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                long saleId = saleDao.insertSale(c, locationId, userId,
                        total, cashTenderedCentavos, cashTenderedCentavos - total);
                for (SaleLine line : lines) {
                    saleDao.insertItem(c, saleId, line);
                    inventoryDao.decrease(c, locationId, line.productId(), line.quantity(),
                            MovementReason.SALE, saleId, userId);
                }
                c.commit();
                return saleId;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
}