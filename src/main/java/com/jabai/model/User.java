/*
 * this file is the base class for everyone who logs in, holding the shared details like username, full name and status. it is sealed so that only manager and clerk can extend it.
 */

package com.jabai.model;

public abstract sealed class User permits Manager, Clerk {
    private final int id;
    private final String username;
    private final String fullName;
    private final String status;

    protected User(int id, String username, String fullName, String status) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return "Active".equalsIgnoreCase(status);
    }

    public abstract Role getRole();

    public abstract Integer getLocationId();

    public abstract boolean canAccessLocation(int locationId);
}