package com.jabai.service;

import com.jabai.db.SaleDao;
import com.jabai.model.FilterOption;
import com.jabai.model.Role;
import com.jabai.model.SaleItemView;
import com.jabai.model.SaleSummary;
import com.jabai.model.User;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SaleHistoryService {
    private final SaleDao saleDao = new SaleDao();

    private void requireManager(User actor) {
        if (actor == null || actor.getRole() != Role.MANAGER) {
            throw new SecurityException("Only managers can view the sale history.");
        }
    }

    public List<SaleSummary> history(User actor, Integer locationId, Integer clerkId,
                                     LocalDate from, LocalDate to) throws SQLException {
        requireManager(actor);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("The start date must not be after the end date.");
        }
        String fromText = from == null ? null : from.toString();
        String toText = to == null ? null : to.toString();
        return saleDao.findFiltered(locationId, clerkId, fromText, toText);
    }

    public List<SaleItemView> items(User actor, long saleId) throws SQLException {
        requireManager(actor);
        return saleDao.findItems(saleId);
    }

    public List<FilterOption> outlets(User actor) throws SQLException {
        requireManager(actor);
        return saleDao.findOutletOptions();
    }

    public List<FilterOption> cashiers(User actor) throws SQLException {
        requireManager(actor);
        return saleDao.findCashierOptions();
    }
}