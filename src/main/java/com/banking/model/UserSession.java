package com.banking.model;

/**
 * Represents an authenticated session for a user or administrator
 * with automatic inactivity timeout tracking (e.g. 2 minutes).
 */
public class UserSession {

    private final Customer customer;
    private final boolean admin;
    private final String sessionUser;
    private final long loginTimeMillis;
    private volatile long lastAccessTimeMillis;
    private final long timeoutMillis;
    private volatile boolean active;

    public UserSession(Customer customer, long timeoutMinutes) {
        this.customer = customer;
        this.admin = false;
        this.sessionUser = customer.getEmail();
        this.loginTimeMillis = System.currentTimeMillis();
        this.lastAccessTimeMillis = this.loginTimeMillis;
        this.timeoutMillis = timeoutMinutes * 60 * 1000L;
        this.active = true;
    }

    public static UserSession createAdminSession(String adminUsername, long timeoutMinutes) {
        return new UserSession(adminUsername, timeoutMinutes);
    }

    private UserSession(String adminUsername, long timeoutMinutes) {
        this.customer = null;
        this.admin = true;
        this.sessionUser = adminUsername;
        this.loginTimeMillis = System.currentTimeMillis();
        this.lastAccessTimeMillis = this.loginTimeMillis;
        this.timeoutMillis = timeoutMinutes * 60 * 1000L;
        this.active = true;
    }

    /**
     * Checks if the session has expired due to inactivity.
     *
     * @return true if inactive duration exceeds timeoutMillis or session is marked inactive.
     */
    public boolean isExpired() {
        if (!active) {
            return true;
        }
        return (System.currentTimeMillis() - lastAccessTimeMillis) > timeoutMillis;
    }

    /**
     * Updates the last access timestamp to the current system time.
     */
    public synchronized void touch() {
        if (active && !isExpired()) {
            this.lastAccessTimeMillis = System.currentTimeMillis();
        }
    }

    /**
     * Invalidates the active session.
     */
    public synchronized void invalidate() {
        this.active = false;
    }

    public long getRemainingSeconds() {
        if (!active) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - lastAccessTimeMillis;
        long remaining = (timeoutMillis - elapsed) / 1000L;
        return Math.max(0, remaining);
    }

    public Customer getCustomer() {
        return customer;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getSessionUser() {
        return sessionUser;
    }

    public long getLoginTimeMillis() {
        return loginTimeMillis;
    }

    public long getLastAccessTimeMillis() {
        return lastAccessTimeMillis;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public boolean isActive() {
        return active;
    }

    public void setLastAccessTimeMillis(long lastAccessTimeMillis) {
        this.lastAccessTimeMillis = lastAccessTimeMillis;
    }
}
