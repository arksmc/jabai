/*
 * this file records cash and item donations, where item donations add stock and voiding one removes it again. the wallet screen calls it and it uses the donation and inventory daos underneath.
 */

package com.jabai.service;

import com.jabai.db.Database;
import com.jabai.db.DonationDao;
import com.jabai.db.InsufficientStockException;
import com.jabai.db.InventoryDao;
import com.jabai.db.ProductDao;
import com.jabai.model.MovementReason;
import com.jabai.model.Product;
import com.jabai.model.User;
import com.jabai.db.SaleDao;
import com.jabai.model.DonationRow;
import com.jabai.model.WalletSummary;
import java.util.List;
import java.sql.Connection;
import java.sql.SQLException;

public class DonationService {
    private static final int MAX_NAME_LENGTH = 100;

    private final DonationDao donationDao = new DonationDao();
    private final InventoryDao inventoryDao = new InventoryDao();
    private final ProductDao productDao = new ProductDao();
    private final SaleDao saleDao = new SaleDao();

    public long recordCashDonation(User actor, int locationId, String donorName,
                                   long amountCentavos) throws SQLException {
        requireAccess(actor, locationId, "record donations for another outlet");
        String donor = cleanDonor(donorName);
        if (amountCentavos <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                long id = donationDao.insertCash(c, donor, amountCentavos, locationId, actor.getId());
                c.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public long recordItemDonation(User actor, int locationId, String donorName,
                                   int productId, double quantity) throws SQLException {
        requireAccess(actor, locationId, "record donations for another outlet");
        String donor = cleanDonor(donorName);
        if (!Double.isFinite(quantity) || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                Product p = productDao.findById(c, productId)
                        .orElseThrow(() -> new IllegalArgumentException("Product not found."));
                String unit = p.getUnit();
                boolean wholeOnly = "pc".equalsIgnoreCase(unit) || "packet".equalsIgnoreCase(unit);
                if (wholeOnly && quantity != Math.rint(quantity)) {
                    throw new IllegalArgumentException(p.getName() + " must be donated in whole numbers.");
                }
                long id = donationDao.insertItem(c, donor, productId, quantity, locationId, actor.getId());
                inventoryDao.increase(c, locationId, productId, quantity,
                        MovementReason.DONATION, id, actor.getId());
                c.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void voidDonation(User actor, long donationId, String reason) throws SQLException {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to void a donation.");
        }
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                DonationDao.Header h = donationDao.findHeader(c, donationId)
                        .orElseThrow(() -> new IllegalArgumentException("Donation #" + donationId + " not found."));
                requireAccess(actor, h.locationId(), "void donations from another outlet");
                if (!donationDao.markVoided(c, donationId, actor.getId(), reason.trim())) {
                    throw new IllegalStateException("Donation #" + donationId + " is already voided.");
                }
                if ("ITEM".equals(h.type()) && h.productId() != null) {
                    try {
                        inventoryDao.decrease(c, h.locationId(), h.productId(), h.quantity(),
                                MovementReason.VOID, donationId, actor.getId());
                    } catch (InsufficientStockException e) {
                        String name = productDao.findById(c, h.productId())
                                .map(Product::getName).orElse("this product");
                        throw new IllegalStateException("Can't void this donation: some of the donated "
                                + name + " has already been sold or moved out of this outlet.");
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
    
    public WalletSummary walletSummary(User actor, int locationId) throws SQLException {
        requireAccess(actor, locationId, "view another outlet's wallet");
        return new WalletSummary(saleDao.completedTotal(locationId),
                donationDao.completedCashTotal(locationId));
    }

    public List<DonationRow> recentDonations(User actor, int locationId, int limit) throws SQLException {
        requireAccess(actor, locationId, "view another outlet's donations");
        return donationDao.findRecent(locationId, limit);
    }

    private static void requireAccess(User actor, int locationId, String action) {
        if (!actor.canAccessLocation(locationId)) {
            throw new SecurityException("You can't " + action + ".");
        }
    }

    private static String cleanDonor(String donorName) {
        if (donorName == null || donorName.isBlank()) {
            throw new IllegalArgumentException("Donor name is required.");
        }
        String donor = donorName.trim();
        if (donor.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Donor name is too long (max " + MAX_NAME_LENGTH + " characters).");
        }
        return donor;
    }
}