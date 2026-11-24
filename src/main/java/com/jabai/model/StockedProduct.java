/*
 * this file pairs a product with the quantity currently in stock at a location. it is used where a screen needs both the product details and how much is available, such as when picking items to sell.
 */

package com.jabai.model;

import com.jabai.util.Format;
import com.jabai.util.Money;

public class StockedProduct {
	private final Product product;
	private final double quantity;

	public StockedProduct(Product product, double quantity) {
	    this.product = product;
	    this.quantity = quantity;
	}
	
	public Product getProduct()
	{
		return this.product;
	}
	
	public double getQuantity()
	{
		return this.quantity;
	}
	
	public String getStockDisplay()
	{
		return Format.qty(this.quantity) + " " + product.getUnit();
	}
	
	public String getPriceDisplay()
	{
		return Money.format(product.getPriceCentavos());
	}
	
	public boolean isOutOfStock()
	{
		return this.quantity <= 0;
	}
	
	public boolean isLowStock()
	{
		return !this.isOutOfStock() && this.quantity <= product.getReorderLevel();
	}
}
