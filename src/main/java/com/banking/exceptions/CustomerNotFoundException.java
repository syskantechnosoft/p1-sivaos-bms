package com.banking.exceptions;

/**
 * Thrown when a customer profile cannot be found.
 */
public class CustomerNotFoundException extends BankingException {

    public CustomerNotFoundException(String message) {
        super("CUST_NOT_FOUND", message);
    }
}
