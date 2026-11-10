package com.banking.exceptions;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when user input or request payload fails validation checks.
 */
public class ValidationException extends BankingException {

    private final List<String> errors;

    public ValidationException(String message) {
        super("VAL_FAILED", message);
        this.errors = Collections.singletonList(message);
    }

    public ValidationException(List<String> errors) {
        super("VAL_FAILED", String.join("; ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
