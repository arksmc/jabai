
package com.jabai.model;

import com.jabai.util.Format;

public class RequestItemView {

	private final int productId;
	private final String productCode;
	private final String productName;
	private final String unit;
	private final double quantityRequested;
	private final double quantitySent;	

	public RequestItemView(int productId, String productCode, String productName, String unit,
						   double quantityRequested, double quantitySent)
	{
		this.productId = productId;
		this.productCode = productCode;
		this.productName = productName;
		this.unit = unit;
		this.quantityRequested = quantityRequested;
		this.quantitySent = quantitySent;
	}

	public int getProductId() { return this.productId; }
	public String getProductCode() { return this.productCode; }
	public String getProductName() { return this.productName; }
	public String getUnit() { return this.unit; }
	public double getQuantityRequested() { return this.quantityRequested; }
	public double getQuantitySent() { return this.quantitySent; }
	public double getRemainingQuantity() { return this.quantityRequested - this.quantitySent; }

    public String getRequestedDisplay() {
        return Format.qty(this.quantityRequested);
    }

    public String getSentDisplay() {
        return Format.qty(this.quantitySent);
    }

    public String getRemainingDisplay() {
        return Format.qty(this.getRemainingQuantity());
    }
}
