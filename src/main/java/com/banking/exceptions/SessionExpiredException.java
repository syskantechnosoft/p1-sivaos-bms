package com.banking.exceptions;

/**
 * Thrown when an authenticated user session has timed out due to inactivity.
 */
public class SessionExpiredException extends BankingException {

    public SessionExpiredException(String message) {
        super("AUTH_SESSION_EXPIRED", message);
    }
}
