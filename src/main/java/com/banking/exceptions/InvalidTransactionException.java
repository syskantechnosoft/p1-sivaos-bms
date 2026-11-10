package com.banking.exceptions;

/**
 * Thrown when a financial transaction violates domain rules.
 */
public class InvalidTransactionException extends BankingException {

    public InvalidTransactionException(String message) {
        super("TXN_INVALID", message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super("TXN_INVALID", message, cause);
    }
}
