package com.banking.dao;

import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.model.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for Account entities.
 */
public interface AccountDAO {

    Optional<Account> findById(Long accountId);

    Optional<Account> findByIdForUpdate(Connection conn, Long accountId);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(Long customerId);

    List<Account> findAll();

    List<Account> findSavingsAccountsForInterest();

    Account save(Account account);

    boolean updateBalance(Connection conn, Long accountId, BigDecimal newBalance);

    boolean updateStatus(Long accountId, AccountStatus status);

    String generateNextAccountNumber(AccountType type);
}
