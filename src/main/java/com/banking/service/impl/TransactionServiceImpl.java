package com.banking.service.impl;

import com.banking.config.DBConnection;
import com.banking.dao.AccountDAO;
import com.banking.dao.TransactionDAO;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import com.banking.exceptions.AccountNotFoundException;
import com.banking.exceptions.DatabaseException;
import com.banking.exceptions.InsufficientFundsException;
import com.banking.exceptions.InvalidTransactionException;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.service.TransactionService;
import com.banking.util.AppLogger;
import com.banking.util.TransactionRefUtil;
import com.banking.validation.TransactionValidator;
import com.banking.validation.ValidationResult;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Implementation of TransactionService providing atomic ACID transactions
 * using explicit JDBC setAutoCommit(false), commit(), and rollback().
 */
public class TransactionServiceImpl implements TransactionService {

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final TransactionValidator transactionValidator;

    public TransactionServiceImpl(AccountDAO accountDAO, TransactionDAO transactionDAO) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.transactionValidator = new TransactionValidator();
    }

    public TransactionServiceImpl(AccountDAO accountDAO, TransactionDAO transactionDAO, TransactionValidator validator) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.transactionValidator = validator;
    }

    @Override
    public Transaction deposit(Long accountId, BigDecimal amount, String description) {
        ValidationResult val = transactionValidator.validate(accountId, null, TransactionType.DEPOSIT, amount);
        if (!val.isValid()) {
            throw new InvalidTransactionException(val.getFirstError());
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Account account = accountDAO.findByIdForUpdate(conn, accountId)
                    .orElseThrow(() -> new AccountNotFoundException("Account ID " + accountId + " not found."));

            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidTransactionException("Account " + account.getAccountNumber() + " is not active (Status: " + account.getStatus() + ")");
            }

            BigDecimal before = account.getBalance();
            BigDecimal after = before.add(amount);

            accountDAO.updateBalance(conn, accountId, after);

            String ref = TransactionRefUtil.generateReference("DEP");
            String desc = (description != null && !description.trim().isEmpty()) ? description : "Cash Deposit";

            Transaction txn = new Transaction(ref, accountId, null, TransactionType.DEPOSIT,
                    amount, before, after, desc, TransactionStatus.SUCCESS);

            Transaction saved = transactionDAO.save(conn, txn);

            conn.commit();

            AppLogger.logTransaction("DEPOSIT_SUCCESS: Ref={}, Account={}, Amount={}, BalanceBefore={}, BalanceAfter={}",
                    ref, account.getAccountNumber(), amount, before, after);

            return saved;
        } catch (Exception e) {
            rollbackQuietly(conn);
            AppLogger.error("Deposit failed for account {}: {}", accountId, e.getMessage());
            if (e instanceof RuntimeException re) throw re;
            throw new DatabaseException("Deposit transaction failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public Transaction withdraw(Long accountId, BigDecimal amount, String description) {
        ValidationResult val = transactionValidator.validate(accountId, null, TransactionType.WITHDRAW, amount);
        if (!val.isValid()) {
            throw new InvalidTransactionException(val.getFirstError());
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Account account = accountDAO.findByIdForUpdate(conn, accountId)
                    .orElseThrow(() -> new AccountNotFoundException("Account ID " + accountId + " not found."));

            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidTransactionException("Account " + account.getAccountNumber() + " is not active (Status: " + account.getStatus() + ")");
            }

            BigDecimal before = account.getBalance();
            if (before.compareTo(amount) < 0) {
                throw new InsufficientFundsException(
                        "Insufficient balance. Available: " + before + ", Requested: " + amount, before, amount);
            }

            BigDecimal after = before.subtract(amount);

            if (account.getAccountType() == AccountType.SAVINGS && after.compareTo(account.getMinimumBalance()) < 0) {
                throw new InsufficientFundsException(
                        "Withdrawal violates minimum balance policy (Required minimum: " + account.getMinimumBalance() + ")",
                        before, amount);
            }

            accountDAO.updateBalance(conn, accountId, after);

            String ref = TransactionRefUtil.generateReference("WTH");
            String desc = (description != null && !description.trim().isEmpty()) ? description : "Cash Withdrawal";

            Transaction txn = new Transaction(ref, accountId, null, TransactionType.WITHDRAW,
                    amount, before, after, desc, TransactionStatus.SUCCESS);

            Transaction saved = transactionDAO.save(conn, txn);

            conn.commit();

            AppLogger.logTransaction("WITHDRAW_SUCCESS: Ref={}, Account={}, Amount={}, BalanceBefore={}, BalanceAfter={}",
                    ref, account.getAccountNumber(), amount, before, after);

            return saved;
        } catch (Exception e) {
            rollbackQuietly(conn);
            AppLogger.error("Withdrawal failed for account {}: {}", accountId, e.getMessage());
            if (e instanceof RuntimeException re) throw re;
            throw new DatabaseException("Withdrawal transaction failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public Transaction transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String description) {
        ValidationResult val = transactionValidator.validate(sourceAccountId, destinationAccountId, TransactionType.FUND_TRANSFER, amount);
        if (!val.isValid()) {
            throw new InvalidTransactionException(val.getFirstError());
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Acquire row locks in consistent numeric order to prevent database deadlocks
            Long firstId = sourceAccountId < destinationAccountId ? sourceAccountId : destinationAccountId;
            Long secondId = sourceAccountId < destinationAccountId ? destinationAccountId : sourceAccountId;

            Account firstLocked = accountDAO.findByIdForUpdate(conn, firstId)
                    .orElseThrow(() -> new AccountNotFoundException("Account ID " + firstId + " not found."));
            Account secondLocked = accountDAO.findByIdForUpdate(conn, secondId)
                    .orElseThrow(() -> new AccountNotFoundException("Account ID " + secondId + " not found."));

            Account source = sourceAccountId.equals(firstId) ? firstLocked : secondLocked;
            Account destination = destinationAccountId.equals(firstId) ? firstLocked : secondLocked;

            if (source.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidTransactionException("Source account " + source.getAccountNumber() + " is not active.");
            }
            if (destination.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidTransactionException("Destination account " + destination.getAccountNumber() + " is not active.");
            }

            BigDecimal srcBefore = source.getBalance();
            if (srcBefore.compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient transfer balance in account " + source.getAccountNumber() +
                        ". Available: " + srcBefore + ", Transfer requested: " + amount, srcBefore, amount);
            }

            BigDecimal srcAfter = srcBefore.subtract(amount);
            if (source.getAccountType() == AccountType.SAVINGS && srcAfter.compareTo(source.getMinimumBalance()) < 0) {
                throw new InsufficientFundsException("Transfer violates minimum balance requirement for account " +
                        source.getAccountNumber() + " (Minimum: " + source.getMinimumBalance() + ")", srcBefore, amount);
            }

            BigDecimal destBefore = destination.getBalance();
            BigDecimal destAfter = destBefore.add(amount);

            // Atomically update both accounts
            accountDAO.updateBalance(conn, sourceAccountId, srcAfter);
            accountDAO.updateBalance(conn, destinationAccountId, destAfter);

            String ref = TransactionRefUtil.generateReference("TRF");
            String desc = (description != null && !description.trim().isEmpty())
                    ? description
                    : ("Transfer from " + source.getAccountNumber() + " to " + destination.getAccountNumber());

            Transaction txn = new Transaction(ref, sourceAccountId, destinationAccountId,
                    TransactionType.FUND_TRANSFER, amount, srcBefore, srcAfter, desc, TransactionStatus.SUCCESS);

            Transaction saved = transactionDAO.save(conn, txn);

            conn.commit();

            AppLogger.logTransaction("TRANSFER_SUCCESS: Ref={}, From={}, To={}, Amount={}, SrcBalanceAfter={}",
                    ref, source.getAccountNumber(), destination.getAccountNumber(), amount, srcAfter);

            return saved;
        } catch (Exception e) {
            rollbackQuietly(conn);
            AppLogger.error("Fund transfer failed from {} to {}: {}", sourceAccountId, destinationAccountId, e.getMessage());
            if (e instanceof RuntimeException re) throw re;
            throw new DatabaseException("Fund transfer failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public List<Transaction> getAccountStatement(Long accountId, int limit, int offset) {
        return transactionDAO.findByAccountId(accountId, limit, offset);
    }

    @Override
    public List<Transaction> getAllTransactions(int limit, int offset) {
        return transactionDAO.findAll(limit, offset);
    }

    @Override
    public int getTransactionCount(Long accountId) {
        return transactionDAO.countByAccountId(accountId);
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                AppLogger.error("Failed to rollback connection: {}", e.getMessage());
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                AppLogger.error("Failed to close connection: {}", e.getMessage());
            }
        }
    }
}
