package com.jabai.model;

public enum MovementReason {
    PURCHASE,      // delivery from a supplier into the warehouse
    TRANSFER_OUT,  // warehouse sends stock to an outlet
    TRANSFER_IN,   // outlet receives stock from the warehouse
    SALE,          // clerk sale
    ADJUSTMENT     // manual correction (for later)
}