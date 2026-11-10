package com.banking.security;

import com.banking.config.DatabaseConfig;
import com.banking.exceptions.SessionExpiredException;
import com.banking.model.Customer;
import com.banking.model.UserSession;
import com.banking.util.AppLogger;

/**
 * Manages user authentication sessions with strict inactivity timeout enforcement
 * (default 2 minutes / 120 seconds).
 */
public class SessionManager {

    private static UserSession currentSession;
    private static long configuredTimeoutMinutes = DatabaseConfig.getSessionTimeoutMinutes();

    public static synchronized UserSession createCustomerSession(Customer customer) {
        currentSession = new UserSession(customer, configuredTimeoutMinutes);
        AppLogger.logAudit("SESSION_STARTED: Customer {} ({}) logged in. Timeout set to {} minutes.",
                customer.getCustomerCode(), customer.getEmail(), configuredTimeoutMinutes);
        return currentSession;
    }

    public static synchronized UserSession createAdminSession(String adminUsername) {
        currentSession = UserSession.createAdminSession(adminUsername, configuredTimeoutMinutes);
        AppLogger.logAudit("SESSION_STARTED: Administrator {} logged in. Timeout set to {} minutes.",
                adminUsername, configuredTimeoutMinutes);
        return currentSession;
    }

    public static synchronized UserSession getCurrentSession() {
        return currentSession;
    }

    public static synchronized boolean hasActiveSession() {
        return currentSession != null && !currentSession.isExpired();
    }

    /**
     * Verifies that the current session is active.
     * If the inactivity duration exceeds 2 minutes, the session is invalidated,
     * an audit log is written, and SessionExpiredException is thrown.
     *
     * @return active UserSession
     * @throws SessionExpiredException if no session exists or inactivity timeout was exceeded
     */
    public static synchronized UserSession validateAndTouch() {
        if (currentSession == null) {
            throw new SessionExpiredException("No active session found. Please login.");
        }

        if (currentSession.isExpired()) {
            String user = currentSession.getSessionUser();
            currentSession.invalidate();
            currentSession = null;
            AppLogger.logAudit("SESSION_EXPIRED: User {} session timed out due to 2 minutes of inactivity.", user);
            throw new SessionExpiredException("Your session has timed out after 2 minutes of inactivity. Please login again.");
        }

        currentSession.touch();
        return currentSession;
    }

    public static synchronized void invalidateCurrentSession() {
        if (currentSession != null) {
            AppLogger.logAudit("SESSION_ENDED: User {} logged out explicitly.", currentSession.getSessionUser());
            currentSession.invalidate();
            currentSession = null;
        }
    }

    public static synchronized void setConfiguredTimeoutMinutes(long minutes) {
        configuredTimeoutMinutes = minutes;
    }

    public static synchronized long getConfiguredTimeoutMinutes() {
        return configuredTimeoutMinutes;
    }
}
