package com.jabai.service;

import com.jabai.db.TransactionDao;
import com.jabai.model.TransactionRow;
import com.jabai.model.User;

import java.sql.SQLException;
import java.util.List;

public class TransactionService {
    private final TransactionDao transactionDao = new TransactionDao();
    private final SaleService saleService = new SaleService();
    private final DonationService donationService = new DonationService();

    public List<TransactionRow> recent(User actor, int locationId, int limit) throws SQLException {
        if (!actor.canAccessLocation(locationId)) {
            throw new SecurityException("You can't view another outlet's transactions.");
        }
        return transactionDao.findRecent(locationId, limit);
    }

    public void voidTransaction(User actor, TransactionRow row, String reason) throws SQLException {
        if (row.isSale()) {
            saleService.voidSale(actor, row.id(), reason);
        } else {
            donationService.voidDonation(actor, row.id(), reason);
        }
    }
}