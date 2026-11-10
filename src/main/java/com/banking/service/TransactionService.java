package com.banking.service;

import com.banking.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service contract for financial transactions (Deposits, Withdrawals, Transfers).
 */
public interface TransactionService {

    Transaction deposit(Long accountId, BigDecimal amount, String description);

    Transaction withdraw(Long accountId, BigDecimal amount, String description);

    Transaction transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String description);

    List<Transaction> getAccountStatement(Long accountId, int limit, int offset);

    List<Transaction> getAllTransactions(int limit, int offset);

    int getTransactionCount(Long accountId);
}
