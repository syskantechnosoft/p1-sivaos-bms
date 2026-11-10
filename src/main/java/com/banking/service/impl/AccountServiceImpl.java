package com.banking.service.impl;

import com.banking.dao.AccountDAO;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.exceptions.AccountNotFoundException;
import com.banking.exceptions.ValidationException;
import com.banking.model.Account;
import com.banking.service.AccountService;
import com.banking.util.AppLogger;
import com.banking.validation.AccountValidator;
import com.banking.validation.ValidationResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of AccountService.
 */
public class AccountServiceImpl implements AccountService {

    private final AccountDAO accountDAO;
    private final AccountValidator accountValidator;

    public AccountServiceImpl(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
        this.accountValidator = new AccountValidator();
    }

    public AccountServiceImpl(AccountDAO accountDAO, AccountValidator accountValidator) {
        this.accountDAO = accountDAO;
        this.accountValidator = accountValidator;
    }

    @Override
    public Account openAccount(Long customerId, AccountType type, BigDecimal initialDeposit,
                               BigDecimal interestRate, BigDecimal minimumBalance, LocalDate maturityDate) {
        if (type == null) {
            throw new ValidationException("Account type must be specified.");
        }

        BigDecimal balance = initialDeposit != null ? initialDeposit : BigDecimal.ZERO;

        BigDecimal rate = interestRate;
        if (rate == null) {
            rate = switch (type) {
                case SAVINGS -> BigDecimal.valueOf(4.00);
                case CURRENT -> BigDecimal.ZERO;
                case LOAN -> BigDecimal.valueOf(9.00);
                case DEPOSIT -> BigDecimal.valueOf(7.00);
            };
        }

        BigDecimal minBal = minimumBalance;
        if (minBal == null) {
            minBal = switch (type) {
                case SAVINGS -> BigDecimal.valueOf(1000.00);
                default -> BigDecimal.ZERO;
            };
        }

        String accNumber = accountDAO.generateNextAccountNumber(type);
        LocalDate openingDate = LocalDate.now();

        Account account = new Account(customerId, accNumber, type, balance, rate, minBal, openingDate, maturityDate);
        account.setStatus(AccountStatus.ACTIVE);

        ValidationResult val = accountValidator.validate(account);
        if (!val.isValid()) {
            throw new ValidationException(val.getErrors());
        }

        Account saved = accountDAO.save(account);
        AppLogger.logAccount("ACCOUNT_OPENED: AccountNumber={}, Type={}, CustomerId={}, Balance={}",
                saved.getAccountNumber(), saved.getAccountType(), saved.getCustomerId(), saved.getBalance());
        return saved;
    }

    @Override
    public Account getAccountById(Long accountId) {
        return accountDAO.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account ID " + accountId + " not found."));
    }

    @Override
    public Account getAccountByNumber(String accountNumber) {
        return accountDAO.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account number '" + accountNumber + "' not found."));
    }

    @Override
    public List<Account> getAccountsByCustomerId(Long customerId) {
        return accountDAO.findByCustomerId(customerId);
    }

    @Override
    public List<Account> getAllAccounts() {
        return accountDAO.findAll();
    }

    @Override
    public BigDecimal getAccountBalance(Long accountId) {
        return getAccountById(accountId).getBalance();
    }

    @Override
    public boolean freezeAccount(Long accountId) {
        Account a = getAccountById(accountId);
        boolean ok = accountDAO.updateStatus(accountId, AccountStatus.FROZEN);
        if (ok) {
            AppLogger.logAccount("ACCOUNT_FROZEN: AccountNumber={}", a.getAccountNumber());
        }
        return ok;
    }

    @Override
    public boolean unfreezeAccount(Long accountId) {
        Account a = getAccountById(accountId);
        boolean ok = accountDAO.updateStatus(accountId, AccountStatus.ACTIVE);
        if (ok) {
            AppLogger.logAccount("ACCOUNT_UNFROZEN: AccountNumber={}", a.getAccountNumber());
        }
        return ok;
    }

    @Override
    public boolean closeAccount(Long accountId) {
        Account a = getAccountById(accountId);
        if (a.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new ValidationException("Cannot close account with positive balance (" + a.getBalance() + "). Please withdraw or transfer funds first.");
        }
        boolean ok = accountDAO.updateStatus(accountId, AccountStatus.CLOSED);
        if (ok) {
            AppLogger.logAccount("ACCOUNT_CLOSED: AccountNumber={}", a.getAccountNumber());
        }
        return ok;
    }
}
