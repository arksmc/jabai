/*
 * this file is a read-only view of one item inside a past sale. it is used to show what was sold when a sale is selected in the history.
 */

package com.jabai.model;

import com.jabai.util.Money;

public class SaleItemView {
	private final String productCode;
	private final String productName;
	private final String unit;
	private final double quantity;
	private final long unitPriceCentavos;
	
	public SaleItemView(String productCode, String productName, String unit, double quantity, long unitPriceCentavos)
	{
		this.productCode = productCode;
		this.productName = productName;
		this.unit = unit;
		this.quantity = quantity;
		this.unitPriceCentavos = unitPriceCentavos;
	}
	
	public String getProductCode() { return this.productCode; }
	public String getProductName() { return this.productName; }
	public String getUnit() { return this.unit; }
	public double getQuantity() { return this.quantity; }
	public long getUnitPriceCentavos() { return this.unitPriceCentavos; }
	
	public long lineTotalCentavos()
	{
		double totalCentavos = this.quantity * this.unitPriceCentavos;
		
		return Math.round(totalCentavos);
	}
	
	public String getLineTotalDisplay()
	{
		return Money.format(this.lineTotalCentavos());
	}
}
