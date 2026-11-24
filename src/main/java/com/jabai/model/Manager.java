/*
 * this file represents the manager, who has no fixed location and can access every outlet. the dashboards and services check this class to decide what a manager is allowed to do.
 */

package com.jabai.model;

public final class Manager extends User {

    public Manager(int id, String username, String fullName, String status) {
        super(id, username, fullName, status);
    }

    @Override
    public Role getRole() {
        return Role.MANAGER;
    }

    @Override
    public Integer getLocationId() {
        return null;
    }

    @Override
    public boolean canAccessLocation(int locationId) {
        return true;
    }
}