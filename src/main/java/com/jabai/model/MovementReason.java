package com.jabai.model;

public enum MovementReason {
    PURCHASE,      // delivery from a supplier into the warehouse
    TRANSFER_OUT,  // warehouse sends stock to an outlet
    TRANSFER_IN,   // outlet receives stock from the warehouse
    SALE,          // clerk sale
    VOID,          // stock put back (or removed) because a sale/donation was voided
    DONATION,      // item donation received at an outlet
    ADJUSTMENT     // manual correction (for later)
}