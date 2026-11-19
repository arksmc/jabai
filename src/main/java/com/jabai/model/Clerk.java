package com.jabai.model;

public final class Clerk extends User {
    private final int locationId;
    private final String locationName;

    public Clerk(int id, String username, String fullName, String status,
                 int locationId, String locationName) {
        super(id, username, fullName, status);
        this.locationId = locationId;
        this.locationName = locationName;
    }

    @Override
    public Role getRole() {
        return Role.CLERK;
    }

    @Override
    public Integer getLocationId() {
        return locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    @Override
    public boolean canAccessLocation(int locationId) {
        return this.locationId == locationId;
    }
}