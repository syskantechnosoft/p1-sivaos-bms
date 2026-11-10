package com.banking.validation;

import com.banking.enums.AccountType;
import com.banking.enums.TransactionType;
import com.banking.model.Account;
import com.banking.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Custom Validation Tests")
class ValidationTest {

    private final CustomerValidator customerValidator = new CustomerValidator();
    private final AccountValidator accountValidator = new AccountValidator();
    private final TransactionValidator transactionValidator = new TransactionValidator();

    @Test
    @DisplayName("Should pass valid customer profile and raw password")
    void shouldValidateValidCustomer() {
        Customer c = new Customer();
        c.setFirstName("Arun");
        c.setLastName("Kumar");
        c.setEmail("arun.kumar@bank.com");
        c.setPhone("9876543210");
        c.setDateOfBirth(LocalDate.of(1995, 5, 20));

        ValidationResult res = customerValidator.validate(c, "StrongPass@123");
        assertTrue(res.isValid());
        assertTrue(res.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should reject underage customer (age < 18)")
    void shouldRejectUnderageCustomer() {
        Customer c = new Customer();
        c.setFirstName("Young");
        c.setLastName("Child");
        c.setEmail("child@bank.com");
        c.setPhone("9876543210");
        // Born only 10 years ago
        c.setDateOfBirth(LocalDate.now().minusYears(10));

        ValidationResult res = customerValidator.validate(c, "StrongPass@123");
        assertFalse(res.isValid());
        assertTrue(res.getFirstError().contains("18 years old"));
    }

    @Test
    @DisplayName("Should reject invalid email format")
    void shouldRejectInvalidEmail() {
        Customer c = new Customer();
        c.setFirstName("John");
        c.setLastName("Doe");
        c.setEmail("not-an-email");
        c.setPhone("9876543210");
        c.setDateOfBirth(LocalDate.of(1990, 1, 1));

        ValidationResult res = customerValidator.validate(c, "StrongPass@123");
        assertFalse(res.isValid());
        assertTrue(res.getErrors().stream().anyMatch(e -> e.contains("email")));
    }

    @Test
    @DisplayName("Should validate account requirements")
    void shouldValidateAccountRules() {
        Account acc = new Account();
        acc.setCustomerId(1L);
        acc.setAccountType(AccountType.SAVINGS);
        acc.setBalance(new BigDecimal("1000.00"));
        acc.setMinimumBalance(new BigDecimal("500.00"));
        acc.setInterestRate(new BigDecimal("4.00"));

        assertTrue(accountValidator.validate(acc).isValid());

        // Deposit account without maturity date should fail
        Account dep = new Account();
        dep.setCustomerId(1L);
        dep.setAccountType(AccountType.DEPOSIT);
        dep.setBalance(new BigDecimal("10000.00"));
        dep.setMinimumBalance(BigDecimal.ZERO);
        dep.setOpeningDate(LocalDate.now());
        dep.setMaturityDate(null);

        ValidationResult depRes = accountValidator.validate(dep);
        assertFalse(depRes.isValid());
        assertTrue(depRes.getErrors().stream().anyMatch(e -> e.contains("Maturity date is required")));
    }

    @Test
    @DisplayName("Should validate transaction inputs")
    void shouldValidateTransactions() {
        // Amount <= 0 rejected
        ValidationResult r1 = transactionValidator.validate(1L, null, TransactionType.DEPOSIT, BigDecimal.ZERO);
        assertFalse(r1.isValid());

        // Transfer with same source and destination rejected
        ValidationResult r2 = transactionValidator.validate(1L, 1L, TransactionType.FUND_TRANSFER, new BigDecimal("100.00"));
        assertFalse(r2.isValid());
        assertTrue(r2.getFirstError().contains("different"));

        // Valid transfer
        ValidationResult r3 = transactionValidator.validate(1L, 2L, TransactionType.FUND_TRANSFER, new BigDecimal("500.00"));
        assertTrue(r3.isValid());
    }
}
