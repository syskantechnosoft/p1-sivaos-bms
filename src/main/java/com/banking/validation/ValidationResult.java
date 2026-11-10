package com.banking.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container holding the outcome of a validation check and any error messages.
 */
public class ValidationResult {

    private final boolean valid;
    private final List<String> errors;

    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? Collections.unmodifiableList(errors) : Collections.emptyList();
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult fail(String error) {
        return new ValidationResult(false, Collections.singletonList(error));
    }

    public static ValidationResult fail(List<String> errors) {
        return new ValidationResult(false, new ArrayList<>(errors));
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getFirstError() {
        return errors.isEmpty() ? "" : errors.get(0);
    }
}
