package com.banking.exceptions;

/**
 * Thrown when user credentials fail authentication.
 */
public class AuthenticationException extends BankingException {

    public AuthenticationException(String message) {
        super("AUTH_INVALID_CREDENTIALS", message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super("AUTH_INVALID_CREDENTIALS", message, cause);
    }
}
