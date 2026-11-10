package com.banking.exceptions;

/**
 * Base custom unchecked exception for Banking CLI Application.
 */
public class BankingException extends RuntimeException {

    private final String errorCode;

    public BankingException(String message) {
        super(message);
        this.errorCode = "BANK_ERR_GENERIC";
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BANK_ERR_GENERIC";
    }

    public BankingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BankingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
