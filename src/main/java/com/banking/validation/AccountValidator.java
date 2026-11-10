package com.banking.validation;

import com.banking.enums.AccountType;
import com.banking.model.Account;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain validator for Account creation and lifecycle updates.
 */
public class AccountValidator {

    public ValidationResult validate(Account account) {
        List<String> errors = new ArrayList<>();

        if (account == null) {
            return ValidationResult.fail("Account cannot be null");
        }

        if (account.getCustomerId() == null || account.getCustomerId() <= 0) {
            errors.add("Valid customer ID is required.");
        }

        if (account.getAccountType() == null) {
            errors.add("Account type must be specified.");
        }

        if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Initial balance cannot be negative.");
        }

        if (account.getMinimumBalance() == null || account.getMinimumBalance().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Minimum balance cannot be negative.");
        }

        if (account.getInterestRate() != null &&
                (account.getInterestRate().compareTo(BigDecimal.ZERO) < 0 ||
                 account.getInterestRate().compareTo(BigDecimal.valueOf(100)) > 0)) {
            errors.add("Interest rate must be between 0.00% and 100.00%.");
        }

        if (account.getAccountType() == AccountType.DEPOSIT) {
            if (account.getMaturityDate() == null) {
                errors.add("Maturity date is required for DEPOSIT accounts.");
            } else if (account.getOpeningDate() != null && !account.getMaturityDate().isAfter(account.getOpeningDate())) {
                errors.add("Maturity date must be after the opening date.");
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }
}
