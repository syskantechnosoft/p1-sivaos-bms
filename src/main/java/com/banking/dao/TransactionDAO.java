package com.banking.dao;

import com.banking.model.Transaction;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for Transaction entities.
 */
public interface TransactionDAO {

    Optional<Transaction> findById(Long transactionId);

    Optional<Transaction> findByReference(String reference);

    List<Transaction> findByAccountId(Long accountId, int limit, int offset);

    List<Transaction> findAll(int limit, int offset);

    Transaction save(Connection conn, Transaction transaction);

    int countByAccountId(Long accountId);
}
