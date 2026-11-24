/*
 * this file lists the reasons stock can change, such as sale, void, donation, purchase and transfer. every stock movement is saved with one of these so the history can be explained later.
 */

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