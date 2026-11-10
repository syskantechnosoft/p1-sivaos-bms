package com.banking.exceptions;

/**
 * Thrown when an account cannot be located by ID or account number.
 */
public class AccountNotFoundException extends BankingException {

    public AccountNotFoundException(String message) {
        super("ACCT_NOT_FOUND", message);
    }
}
