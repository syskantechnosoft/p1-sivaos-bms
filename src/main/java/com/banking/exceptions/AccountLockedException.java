package com.banking.exceptions;

/**
 * Thrown when an account or customer profile is locked/blocked due to
 * repeated authentication failures or administrative hold.
 */
public class AccountLockedException extends BankingException {

    public AccountLockedException(String message) {
        super("AUTH_ACCOUNT_LOCKED", message);
    }

    public AccountLockedException(String message, Throwable cause) {
        super("AUTH_ACCOUNT_LOCKED", message, cause);
    }
}
