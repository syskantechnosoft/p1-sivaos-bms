package com.banking.validation;

import com.banking.model.Customer;
import com.banking.util.PasswordUtil;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain validator for Customer entities.
 */
public class CustomerValidator {

    public ValidationResult validate(Customer customer, String rawPassword) {
        List<String> errors = new ArrayList<>();

        if (customer == null) {
            return ValidationResult.fail("Customer cannot be null");
        }

        if (!ValidationUtil.isValidName(customer.getFirstName())) {
            errors.add("First name must be 2-50 alphabetic characters.");
        }

        if (!ValidationUtil.isValidName(customer.getLastName())) {
            errors.add("Last name must be 2-50 alphabetic characters.");
        }

        if (!ValidationUtil.isValidEmail(customer.getEmail())) {
            errors.add("Invalid email address format.");
        }

        if (!ValidationUtil.isValidPhone(customer.getPhone())) {
            errors.add("Invalid phone number format (must be 10-15 digits).");
        }

        if (rawPassword != null && !PasswordUtil.isValidComplexity(rawPassword)) {
            errors.add("Password does not meet complexity requirements: " + PasswordUtil.getPasswordRequirementsDescription());
        }

        if (customer.getDateOfBirth() != null) {
            if (!customer.getDateOfBirth().isBefore(LocalDate.now())) {
                errors.add("Date of birth must be strictly in the past.");
            } else {
                int age = Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears();
                if (age < 18) {
                    errors.add("Customer must be at least 18 years old to open an account.");
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }
}
