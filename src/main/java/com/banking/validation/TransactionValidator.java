package com.banking.validation;

import com.banking.enums.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain validator for financial transactions (Deposit, Withdrawal, Transfer).
 */
public class TransactionValidator {

    public ValidationResult validate(Long sourceAccountId, Long destinationAccountId,
                                    TransactionType type, BigDecimal amount) {
        List<String> errors = new ArrayList<>();

        if (sourceAccountId == null || sourceAccountId <= 0) {
            errors.add("Valid source account ID is required.");
        }

        if (type == null) {
            errors.add("Transaction type must be specified.");
        }

        if (!ValidationUtil.isValidPositiveAmount(amount)) {
            errors.add("Transaction amount must be strictly greater than zero and have at most 2 decimal places.");
        }

        if (type == TransactionType.FUND_TRANSFER) {
            if (destinationAccountId == null || destinationAccountId <= 0) {
                errors.add("Destination account ID is required for FUND_TRANSFER.");
            } else if (sourceAccountId != null && sourceAccountId.equals(destinationAccountId)) {
                errors.add("Source and destination accounts must be different for a fund transfer.");
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }
}
