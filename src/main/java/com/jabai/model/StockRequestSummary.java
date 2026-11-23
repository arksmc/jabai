package com.jabai.model;

public class StockRequestSummary {
	private final long id;
	private final String locationName;
	private final String requestedBy;
	private final String status;
	private final String note;
	private final String responseNote;
	private final String createdAt;
	private final String resolvedAt;
	private final String resolvedBy;
	private final int itemCount;
	
	public StockRequestSummary(long id, String locationName, String requestedBy, String status, String note,
							   String responseNote, String createdAt, String resolvedAt, String resolvedBy,
							   int itemCount)
	{
			this.id = id;
			this.locationName = locationName;
			this.requestedBy = requestedBy;
			this.status = status;
			this.note = note;
			this.responseNote = responseNote;
			this.createdAt = createdAt;
			this.resolvedAt = resolvedAt;
			this.resolvedBy = resolvedBy;
			this.itemCount = itemCount;
	}
	
	public long getId() { return this.id; }
	public String getLocationName() { return this.locationName; }
	public String getRequestedBy() { return this.requestedBy; }
	public String getStatus() { return this.status; }
	public String getNote() { return this.note; }
	public String getResponseNote() { return this.responseNote; }
	public String getCreatedAt() { return this.createdAt; }
	public String getResolvedAt() { return this.resolvedAt; }
	public String getResolvedBy() { return this.resolvedBy; }
	public int getItemCount() { return this.itemCount; }
}
