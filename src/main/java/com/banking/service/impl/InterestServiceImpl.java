package com.banking.service.impl;

import com.banking.config.DBConnection;
import com.banking.dao.AccountDAO;
import com.banking.dao.DailyBalanceDAO;
import com.banking.dao.TransactionDAO;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import com.banking.exceptions.DatabaseException;
import com.banking.exceptions.ValidationException;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.service.InterestService;
import com.banking.util.AppLogger;
import com.banking.util.TransactionRefUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of InterestService providing quarterly interest calculation
 * and automatic atomic credits for all active savings accounts.
 */
public class InterestServiceImpl implements InterestService {

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final DailyBalanceDAO dailyBalanceDAO;

    public InterestServiceImpl(AccountDAO accountDAO, TransactionDAO transactionDAO, DailyBalanceDAO dailyBalanceDAO) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.dailyBalanceDAO = dailyBalanceDAO;
    }

    @Override
    public BigDecimal calculateInterestForQuarter(Long accountId, int year, int quarter) {
        if (quarter < 1 || quarter > 4) {
            throw new ValidationException("Quarter must be between 1 and 4. Provided: " + quarter);
        }
        if (year < 2000 || year > 2100) {
            throw new ValidationException("Year must be between 2000 and 2100. Provided: " + year);
        }

        return dailyBalanceDAO.calculateQuarterlyInterest(accountId, year, quarter);
    }

    @Override
    public List<Transaction> creditQuarterlyInterestForAllSavings(int year, int quarter) {
        if (quarter < 1 || quarter > 4) {
            throw new ValidationException("Quarter must be between 1 and 4.");
        }

        List<Account> savingsAccounts = accountDAO.findSavingsAccountsForInterest();
        List<Transaction> creditedList = new ArrayList<>();

        AppLogger.info("Starting quarterly interest credit batch for Year {}, Quarter {} across {} savings accounts.",
                year, quarter, savingsAccounts.size());

        for (Account acc : savingsAccounts) {
            String interestRef = TransactionRefUtil.generateQuarterlyInterestReference(acc.getAccountId(), year, quarter);

            // Check if already credited for this account and quarter
            Optional<Transaction> existing = transactionDAO.findByReference(interestRef);
            if (existing.isPresent()) {
                AppLogger.warn("Quarterly interest already credited for account {} (Ref: {}). Skipping.",
                        acc.getAccountNumber(), interestRef);
                continue;
            }

            BigDecimal interestAmount = calculateInterestForQuarter(acc.getAccountId(), year, quarter);

            if (interestAmount.compareTo(BigDecimal.ZERO) <= 0) {
                AppLogger.info("Calculated interest for account {} is ₹{}. Skipping zero credit.",
                        acc.getAccountNumber(), interestAmount);
                continue;
            }

            Connection conn = null;
            try {
                conn = DBConnection.getConnection();
                conn.setAutoCommit(false);

                Account locked = accountDAO.findByIdForUpdate(conn, acc.getAccountId())
                        .orElse(acc);

                BigDecimal before = locked.getBalance();
                BigDecimal after = before.add(interestAmount);

                accountDAO.updateBalance(conn, locked.getAccountId(), after);

                String description = String.format("Quarterly interest credit Q%d %d (Rate: %s%%)",
                        quarter, year, locked.getInterestRate());

                Transaction interestTxn = new Transaction(
                        interestRef,
                        locked.getAccountId(),
                        null,
                        TransactionType.INTEREST_CREDIT,
                        interestAmount,
                        before,
                        after,
                        description,
                        TransactionStatus.SUCCESS
                );

                Transaction saved = transactionDAO.save(conn, interestTxn);
                conn.commit();

                creditedList.add(saved);

                AppLogger.logTransaction("INTEREST_CREDIT_SUCCESS: Ref={}, Account={}, Amount={}, BalanceAfter={}",
                        interestRef, locked.getAccountNumber(), interestAmount, after);
                AppLogger.logAccount("INTEREST_APPLIED: Account={}, Quarter=Q{} {}, Amount={}",
                        locked.getAccountNumber(), quarter, year, interestAmount);

            } catch (Exception e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        AppLogger.error("Failed to rollback interest transaction: {}", ex.getMessage());
                    }
                }
                AppLogger.error("Failed to credit interest for account {}: {}", acc.getAccountNumber(), e.getMessage());
                throw new DatabaseException("Failed to credit quarterly interest: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException ex) {
                        AppLogger.error("Failed to close connection: {}", ex.getMessage());
                    }
                }
            }
        }

        AppLogger.info("Quarterly interest credit batch completed. Credited {} accounts.", creditedList.size());
        return creditedList;
    }

    @Override
    public void captureDailyClosingBalances() {
        dailyBalanceDAO.captureDailyBalances(LocalDate.now());
    }
}
