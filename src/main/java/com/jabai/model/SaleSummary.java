package com.jabai.model;

import com.jabai.util.Money;

public class SaleSummary {
	private final long id;
	private final String locationName;
	private final String cashierName;
	private final long totalCentavos;
	private final long cashTenderedCentavos;
	private final long changeGivenCentavos;
	private final String createdAt;
	
	public SaleSummary(long id, String locationName, String cashierName, long totalCentavos, long cashTenderedCentavos,
					   long changeGivenCentavos, String createdAt)
	{
		this.id = id;
		this.locationName = locationName;
		this.cashierName = cashierName;
		this.totalCentavos = totalCentavos;
		this.cashTenderedCentavos = cashTenderedCentavos;
		this.changeGivenCentavos = changeGivenCentavos;
		this.createdAt = createdAt;
	}
	
	public long getId() { return this.id; }
	public String getLocationName() { return this.locationName; }
	public String getCashierName() { return this.cashierName; }
	public long getTotalCentavos() { return this.totalCentavos; }
	public long getCashTenderedCentavos() { return this.cashTenderedCentavos; }
	public long getChangeGivenCentavos() { return this.changeGivenCentavos; }
	public String getCreatedAt() { return this.createdAt; }
	
	public String getTotalDisplay() { return Money.format(this.totalCentavos); }
	public String getCashTenderedDisplay() { return Money.format(this.cashTenderedCentavos); }
	public String getChangeGivenDisplay() { return Money.format(this.changeGivenCentavos); }
}
