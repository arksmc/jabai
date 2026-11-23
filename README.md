# Bagsakan — Agriculture Data Management System

**Bagsakan** is a JavaFX desktop inventory, warehouse, and point-of-sale (POS) management system built for agricultural supply distribution—connecting a central **Main Warehouse** with retail **Outlets**.

> *"Fresh stock, every outlet."*

---

## Features

### Authentication & Role-Based Access
* **Dual-Role Login Flow:** Separate sign-in paths for **Managers** (global access across warehouse and outlets) and **Clerks** (outlet-specific access after selecting their assigned branch).
* **Security:** Passwords hashed and verified with **BCrypt** (cost factor 12), backed by role and location access checks in the service layer.

### Manager Portal
* **Product Catalog (`Products`):**
  * Manage agricultural and food items across 11 categories (*Rice Grains, Fruits, Root Crops, Vegetables, Fish, Dairy Products, Planting Seeds, Seafood, Poultry, Pork, Beef*).
  * Create and edit products with unique **EAN-13 barcodes** (`480` GS1 Philippines prefix), buying/cost price, selling price (stored in centavos for exact `₱` arithmetic), unit of measure (`kg`, `L`, `pc`, `packet`, `500g`), reorder thresholds, and `Active`/`Inactive` status.
  * Safe deletion: removes unused products with zero inventory, or automatically marks products with transaction/stock history as `Inactive`.
* **Multi-Location Inventory (`Inventory`):**
  * Inspect real-time stock levels across **Main Warehouse** and any **Outlet**.
  * Filter by barcode, product name, category, or **Low / Out of Stock** status (`In Stock`, `Low Stock`, `Out of Stock`).
* **Supplier Purchase Orders (`Stock Orders`):**
  * Create multi-item supplier orders with auto-filled buying prices, automatic duplicate-line merging, and order notes.
  * Master-detail view displaying formatted order codes (`YYMMDD0001`), order totals, and line items.
  * **Receive or Cancel Orders:** Marking an order as `Received` atomically increments **Main Warehouse** inventory and logs `PURCHASE` entries in `stock_movements`.

### Clerk Portal (Outlet Operations)
* **Point of Sale (`Sales`):**
  * Searchable outlet catalog with live available stock, double-click or quantity-based cart entry, whole-unit validation (`pc`, `packet`), and stock cap checks.
  * Real-time cart totals, cash tendered input with an **Exact** shortcut, change/shortfall calculation, and atomic checkout that deducts outlet inventory (`SALE`).
* **Outlet Wallet & Donations (`Wallet`):**
  * Tracks the outlet's live **Wallet Balance** (`Completed Sales + Completed Cash Donations`).
  * Record **Cash Donations** (credits the wallet) and **Item Donations** (credits outlet inventory under `DONATION`).
* **Transaction Ledger & Voids (`Transactions`):**
  * Unified chronological history of outlet sales (`S-<id>`) and donations (`D-<id>`) filterable by type.
  * **Audited Voiding:** Void completed sales or donations with a mandatory reason—automatically restoring sold stock (`VOID`) or deducting voided item donations (preventing voids if donated stock has already been sold).
* **Outlet Inventory (`Inventory`):**
  * Read-only view of the clerk's assigned outlet stock, reorder levels, and low-stock alerts.

---

## Tech Stack

* **Language:** Java 26 (Records, Sealed Classes, Pattern Matching)
* **GUI Framework:** JavaFX 26.0.2 (`javafx-controls`, `javafx-fxml`, custom CSS styling)
* **Database:** SQLite (`sqlite-jdbc` 3.50.3.0) with WAL mode, foreign keys, and automatic schema/seed initialization (`bagsakan.db`)
* **Security:** `at.favre.lib:bcrypt` 0.10.2
* **Build Tool:** Apache Maven + `javafx-maven-plugin`

---

## Getting Started

### Prerequisites
* **JDK 26**
* **Apache Maven 3.9+**

### Run the Application
```bash
git clone <your-repo-url>
cd jabai-main
mvn clean javafx:run
