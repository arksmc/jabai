/*
 * this file lists and voids transactions for an outlet so sales and donations can be handled in one place. the clerk transactions screen and the system facade call it and it uses the transaction dao underneath.
 */

package com.jabai.service;

import com.jabai.db.TransactionDao;
import com.jabai.model.Role;
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

    public List<TransactionRow> all(User actor, int limit) throws SQLException {
        if (actor.getRole() != Role.MANAGER) {
            throw new SecurityException("Only managers can view transactions across all outlets.");
        }
        return transactionDao.findAll(null, limit);
    }

    public void voidTransaction(User actor, TransactionRow row, String reason) throws SQLException {
        if (row.isSale()) {
            saleService.voidSale(actor, row.id(), reason);
        } else {
            donationService.voidDonation(actor, row.id(), reason);
        }
    }
}