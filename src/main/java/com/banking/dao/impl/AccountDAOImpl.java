package com.banking.dao.impl;

import com.banking.config.DBConnection;
import com.banking.dao.AccountDAO;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.exceptions.DatabaseException;
import com.banking.model.Account;
import com.banking.util.AppLogger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of AccountDAO.
 */
public class AccountDAOImpl implements AccountDAO {

    @Override
    public Optional<Account> findById(Long accountId) {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error finding account by ID: {}", e.getMessage());
            throw new DatabaseException("Failed to find account", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Account> findByIdForUpdate(Connection conn, Long accountId) {
        String sql = "SELECT * FROM accounts WHERE account_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error acquiring row lock for account {}: {}", accountId, e.getMessage());
            throw new DatabaseException("Failed to lock account for update", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE UPPER(account_number) = UPPER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error finding account by number: {}", e.getMessage());
            throw new DatabaseException("Failed to find account by number", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Account> findByCustomerId(Long customerId) {
        String sql = "SELECT * FROM accounts WHERE customer_id = ? ORDER BY account_id ASC";
        List<Account> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAccount(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error finding accounts by customer ID: {}", e.getMessage());
            throw new DatabaseException("Failed to list accounts for customer", e);
        }
        return list;
    }

    @Override
    public List<Account> findAll() {
        String sql = "SELECT * FROM accounts ORDER BY account_id ASC";
        List<Account> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAccount(rs));
            }
        } catch (SQLException e) {
            AppLogger.error("Error listing all accounts: {}", e.getMessage());
            throw new DatabaseException("Failed to retrieve accounts", e);
        }
        return list;
    }

    @Override
    public List<Account> findSavingsAccountsForInterest() {
        String sql = "SELECT * FROM accounts WHERE account_type = 'SAVINGS' AND status = 'ACTIVE' ORDER BY account_id ASC";
        List<Account> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAccount(rs));
            }
        } catch (SQLException e) {
            AppLogger.error("Error finding savings accounts for interest: {}", e.getMessage());
            throw new DatabaseException("Failed to retrieve savings accounts for interest", e);
        }
        return list;
    }

    @Override
    public Account save(Account account) {
        String sql = "INSERT INTO accounts (customer_id, account_number, account_type, balance, " +
                "interest_rate, minimum_balance, opening_date, maturity_date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, account.getCustomerId());
            ps.setString(2, account.getAccountNumber());
            ps.setString(3, account.getAccountType().name());
            ps.setBigDecimal(4, account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO);
            ps.setBigDecimal(5, account.getInterestRate() != null ? account.getInterestRate() : BigDecimal.ZERO);
            ps.setBigDecimal(6, account.getMinimumBalance() != null ? account.getMinimumBalance() : BigDecimal.ZERO);
            ps.setDate(7, Date.valueOf(account.getOpeningDate()));
            if (account.getMaturityDate() != null) {
                ps.setDate(8, Date.valueOf(account.getMaturityDate()));
            } else {
                ps.setNull(8, Types.DATE);
            }
            ps.setString(9, account.getStatus() != null ? account.getStatus().name() : AccountStatus.ACTIVE.name());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Creating account failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    account.setAccountId(generatedKeys.getLong(1));
                }
            }
            return account;
        } catch (SQLException e) {
            AppLogger.error("Error saving account: {}", e.getMessage());
            throw new DatabaseException("Failed to save account: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateBalance(Connection conn, Long accountId, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setLong(2, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.error("Error updating account balance: {}", e.getMessage());
            throw new DatabaseException("Failed to update balance", e);
        }
    }

    @Override
    public boolean updateStatus(Long accountId, AccountStatus status) {
        String sql = "UPDATE accounts SET status = ? WHERE account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.error("Error updating account status: {}", e.getMessage());
            throw new DatabaseException("Failed to update account status", e);
        }
    }

    @Override
    public String generateNextAccountNumber(AccountType type) {
        String prefix = switch (type) {
            case SAVINGS -> "SB";
            case CURRENT -> "CA";
            case LOAN -> "LN";
            case DEPOSIT -> "FD";
        };
        String sql = "SELECT COUNT(*) FROM accounts WHERE account_type = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                long count = 1;
                if (rs.next()) {
                    count = rs.getLong(1) + 1;
                }
                return String.format("%s%06d", prefix, 100000 + count);
            }
        } catch (SQLException e) {
            return prefix + (System.currentTimeMillis() % 1000000);
        }
    }

    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setAccountId(rs.getLong("account_id"));
        a.setCustomerId(rs.getLong("customer_id"));
        a.setAccountNumber(rs.getString("account_number"));
        String typeStr = rs.getString("account_type");
        if (typeStr != null) {
            a.setAccountType(AccountType.valueOf(typeStr));
        }
        a.setBalance(rs.getBigDecimal("balance"));
        a.setInterestRate(rs.getBigDecimal("interest_rate"));
        a.setMinimumBalance(rs.getBigDecimal("minimum_balance"));
        Date openDate = rs.getDate("opening_date");
        if (openDate != null) {
            a.setOpeningDate(openDate.toLocalDate());
        }
        Date matDate = rs.getDate("maturity_date");
        if (matDate != null) {
            a.setMaturityDate(matDate.toLocalDate());
        }
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            a.setStatus(AccountStatus.valueOf(statusStr));
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            a.setCreatedAt(createdAt.toLocalDateTime());
        }
        return a;
    }
}
