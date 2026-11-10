package com.banking.service;

import com.banking.enums.AccountType;
import com.banking.model.Account;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for bank account lifecycle management.
 */
public interface AccountService {

    Account openAccount(Long customerId, AccountType type, BigDecimal initialDeposit,
                        BigDecimal interestRate, BigDecimal minimumBalance, LocalDate maturityDate);

    Account getAccountById(Long accountId);

    Account getAccountByNumber(String accountNumber);

    List<Account> getAccountsByCustomerId(Long customerId);

    List<Account> getAllAccounts();

    BigDecimal getAccountBalance(Long accountId);

    boolean freezeAccount(Long accountId);

    boolean unfreezeAccount(Long accountId);

    boolean closeAccount(Long accountId);
}
