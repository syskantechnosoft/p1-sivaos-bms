package com.banking.exceptions;

/**
 * Thrown when low-level JDBC or database constraint operations fail.
 */
public class DatabaseException extends BankingException {

    public DatabaseException(String message) {
        super("DB_ERR", message);
    }

    public DatabaseException(String message, Throwable cause) {
        super("DB_ERR", message, cause);
    }
}
