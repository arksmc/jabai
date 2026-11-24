# Bagsakan

Bagsakan is a desktop app for managing a wholesale produce and goods business. It was built for an agricultural data management project. The business has one Main Warehouse and several outlets. The app keeps track of products, stock, sales, and donations.

It is built with JavaFX and SQLite, so everything runs on one computer and your data is saved automatically.

## Why this topic

In the Philippines, produce, fish, meat, dairy and rice are often distributed through a central warehouse (a "bagsakan") that supplies smaller outlets. A business like this needs to know:

- how much stock is at each location
- what is running low or out of stock
- how much cash has come in from sales
- what has been donated, and by whom

Bagsakan covers these needs with four parts: Products, Inventory, Sales, and Cash/Item Donations.

## How to run

1. Open the project in your IDE (it is a Maven project).
2. Run the `Launcher` class.
3. The database is created automatically the first time, with sample products and stock.

## Logins

| Role    | Username | Password   | Location    |
|---------|----------|------------|-------------|
| Manager | manager  | manager123 | All outlets |
| Clerk   | clerk1   | clerk123   | Outlet 1    |
| Clerk   | clerk2   | clerk123   | Outlet 2    |

## The two kinds of users

**Clerk** works at one outlet. A clerk can:
- record sales
- check the outlet's inventory
- record cash donations and item donations
- see the outlet's wallet balance
- void (cancel) a sale or donation that was entered by mistake

**Manager** oversees everything. A manager can:
- add and manage products
- see inventory at the warehouse and at every outlet
- restock outlets from the warehouse
- watch every clerk's sale history
- see a combined transaction history for all outlets
- open any clerk's dashboard ("View as Clerk") to help or fix something

## A typical walkthrough

### 1. Start and log in
The Welcome screen appears first. Choose an outlet (or the manager option), then enter your username and password. You are taken to the dashboard that matches your role.

### 2. As a clerk
The menu on the left has these pages:

- **Sales:** Search or pick products, set the quantity, and complete the sale. Stock at your outlet goes down automatically, and the money is added to your wallet.
- **Transactions:** A list of every sale and donation at your outlet. Select a wrong entry and click **Void**, then type a reason. The stock and the wallet are corrected automatically.
- **Inventory:** Shows what your outlet has in stock. Items are marked In Stock, Low Stock, or Out of Stock.
- **Wallet:** Shows the outlet's cash balance and lets you record a donation. A cash donation adds money to the wallet. An item donation adds the donated product to your stock. You enter the donor's name each time.
- **Request Stock:** Ask the warehouse for more of a product.

### 3. As a manager
The menu is grouped into sections:

- **Manage:** Add products and view inventory for any location.
- **Warehouse:** Send stock from the warehouse to outlets (Stock Orders) and review requests that clerks sent (Stock Requests).
- **Monitor:** See each clerk's Sale History and the full Transaction History across all outlets (sales and donations, with donor, type, amount or item, and date). The Transaction History is for viewing only.
- **Clerk View:** Open any outlet's clerk dashboard to work as that clerk. A banner shows that you are "viewing as" a clerk, and the Back to Manager button returns you to your own dashboard. Anything done this way appears right away in the manager's screens.

### 4. Log out
Use the logout button at the bottom of the left menu.

## Simple rules the app follows

- Stock can never go below zero. A sale of more than what is available is refused.
- Voided entries are not deleted. They stay in the history, marked as voided, with the reason.
- Voiding a sale puts the stock back. Voiding an item donation takes the stock out again. If those items were already sold, the app tells you it can't be voided.
- The wallet balance is the total of completed sales plus completed cash donations. Voided entries are not counted.
- Money is stored in centavos internally, so there are no rounding errors.

## How it is built (short version)

- **Screens (ui):** What the user sees. The main window switches between screens, and all dashboards share the same layout.
- **Services (service):** The rules, such as "can this person do this?" and "save sale and update stock together."
- **Database access (db):** The only code that talks to SQLite.
- **Models (model):** The things in the business: User (Manager or Clerk), Product, Inventory, and so on.
- **BagsakanSystem:** The main entry point that the screens use to reach the rest of the system.

Data is saved in a local SQLite database file, so it is still there the next time the app is opened.
