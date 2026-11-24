package com.jabai.service;

import com.jabai.db.Database;
import com.jabai.db.InventoryDao;
import com.jabai.db.LocationDao;
import com.jabai.model.Inventory;
import com.jabai.model.Location;
import com.jabai.model.TransactionRow;
import com.jabai.model.User;

import java.sql.SQLException;
import java.util.List;

public class BagsakanSystem {

    private static final BagsakanSystem INSTANCE = new BagsakanSystem();

    private final InventoryDao inventoryDao = new InventoryDao();
    private final LocationDao locationDao = new LocationDao();
    private final TransactionService transactionService = new TransactionService();

    private BagsakanSystem() {
    }

    public static BagsakanSystem getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        Database.init();
    }

    public List<Location> locations() throws SQLException {
        return locationDao.findAll();
    }

    public List<Location> outlets() throws SQLException {
        return locationDao.findOutlets();
    }

    public Inventory inventoryAt(int locationId) throws SQLException {
        String name = "";
        for (Location l : locationDao.findAll()) {
            if (l.getId() == locationId) {
                name = l.getName();
                break;
            }
        }
        return new Inventory(locationId, name, inventoryDao.findStock(locationId));
    }

    public List<TransactionRow> recentTransactions(User actor, int locationId, int limit) throws SQLException {
        return transactionService.recent(actor, locationId, limit);
    }

    public List<TransactionRow> allTransactions(User actor, int limit) throws SQLException {
        return transactionService.all(actor, limit);
    }

    public void voidTransaction(User actor, TransactionRow row, String reason) throws SQLException {
        transactionService.voidTransaction(actor, row, reason);
    }
}