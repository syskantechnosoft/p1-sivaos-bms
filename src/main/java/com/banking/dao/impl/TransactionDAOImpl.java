package com.banking.dao.impl;

import com.banking.config.DBConnection;
import com.banking.dao.TransactionDAO;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import com.banking.exceptions.DatabaseException;
import com.banking.model.Transaction;
import com.banking.util.AppLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of TransactionDAO.
 */
public class TransactionDAOImpl implements TransactionDAO {

    @Override
    public Optional<Transaction> findById(Long transactionId) {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error finding transaction by ID: {}", e.getMessage());
            throw new DatabaseException("Failed to find transaction", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Transaction> findByReference(String reference) {
        String sql = "SELECT * FROM transactions WHERE transaction_ref = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reference.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error finding transaction by ref: {}", e.getMessage());
            throw new DatabaseException("Failed to find transaction by reference", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Transaction> findByAccountId(Long accountId, int limit, int offset) {
        String sql = "SELECT * FROM transactions WHERE account_id = ? OR destination_account_id = ? " +
                "ORDER BY transaction_date DESC, transaction_id DESC LIMIT ? OFFSET ?";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setLong(2, accountId);
            ps.setInt(3, limit > 0 ? limit : 20);
            ps.setInt(4, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error finding transactions for account {}: {}", accountId, e.getMessage());
            throw new DatabaseException("Failed to list account transactions", e);
        }
        return list;
    }

    @Override
    public List<Transaction> findAll(int limit, int offset) {
        String sql = "SELECT * FROM transactions ORDER BY transaction_date DESC, transaction_id DESC LIMIT ? OFFSET ?";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 20);
            ps.setInt(2, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error listing transactions: {}", e.getMessage());
            throw new DatabaseException("Failed to list transactions", e);
        }
        return list;
    }

    @Override
    public Transaction save(Connection conn, Transaction transaction) {
        String sql = "INSERT INTO transactions (transaction_ref, account_id, destination_account_id, " +
                "transaction_type, amount, balance_before, balance_after, description, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, transaction.getTransactionRef());
            ps.setLong(2, transaction.getAccountId());
            if (transaction.getDestinationAccountId() != null) {
                ps.setLong(3, transaction.getDestinationAccountId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setString(4, transaction.getTransactionType().name());
            ps.setBigDecimal(5, transaction.getAmount());
            ps.setBigDecimal(6, transaction.getBalanceBefore());
            ps.setBigDecimal(7, transaction.getBalanceAfter());
            ps.setString(8, transaction.getDescription());
            ps.setString(9, transaction.getStatus() != null ? transaction.getStatus().name() : TransactionStatus.SUCCESS.name());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Saving transaction failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    transaction.setTransactionId(generatedKeys.getLong(1));
                }
            }
            return transaction;
        } catch (SQLException e) {
            AppLogger.error("Error saving transaction: {}", e.getMessage());
            throw new DatabaseException("Failed to record transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public int countByAccountId(Long accountId) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE account_id = ? OR destination_account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setLong(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error counting transactions: {}", e.getMessage());
        }
        return 0;
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getLong("transaction_id"));
        t.setTransactionRef(rs.getString("transaction_ref"));
        t.setAccountId(rs.getLong("account_id"));
        long destId = rs.getLong("destination_account_id");
        if (!rs.wasNull()) {
            t.setDestinationAccountId(destId);
        }
        String typeStr = rs.getString("transaction_type");
        if (typeStr != null) {
            t.setTransactionType(TransactionType.valueOf(typeStr));
        }
        t.setAmount(rs.getBigDecimal("amount"));
        t.setBalanceBefore(rs.getBigDecimal("balance_before"));
        t.setBalanceAfter(rs.getBigDecimal("balance_after"));
        t.setDescription(rs.getString("description"));
        Timestamp date = rs.getTimestamp("transaction_date");
        if (date != null) {
            t.setTransactionDate(date.toLocalDateTime());
        }
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            t.setStatus(TransactionStatus.valueOf(statusStr));
        }
        return t;
    }
}
