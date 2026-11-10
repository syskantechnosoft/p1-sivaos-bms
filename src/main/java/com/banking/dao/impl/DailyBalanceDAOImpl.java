package com.banking.dao.impl;

import com.banking.config.DBConnection;
import com.banking.dao.DailyBalanceDAO;
import com.banking.exceptions.DatabaseException;
import com.banking.model.AccountDailyBalance;
import com.banking.util.AppLogger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of DailyBalanceDAO.
 */
public class DailyBalanceDAOImpl implements DailyBalanceDAO {

    @Override
    public void captureDailyBalances(LocalDate date) {
        String sql = "INSERT INTO account_daily_balance (account_id, balance_date, closing_balance) " +
                "SELECT account_id, ?, balance FROM accounts WHERE status IN ('ACTIVE', 'FROZEN') " +
                "ON DUPLICATE KEY UPDATE closing_balance = VALUES(closing_balance)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            int rows = ps.executeUpdate();
            AppLogger.info("Captured daily closing balances for {}: {} accounts updated.", date, rows);
        } catch (SQLException e) {
            AppLogger.error("Failed to capture daily balances: {}", e.getMessage());
            throw new DatabaseException("Failed to capture daily balance snapshot", e);
        }
    }

    @Override
    public void saveOrUpdate(AccountDailyBalance balance) {
        String sql = "INSERT INTO account_daily_balance (account_id, balance_date, closing_balance) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE closing_balance = VALUES(closing_balance)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, balance.getAccountId());
            ps.setDate(2, Date.valueOf(balance.getBalanceDate()));
            ps.setBigDecimal(3, balance.getClosingBalance());
            ps.executeUpdate();
        } catch (SQLException e) {
            AppLogger.error("Error saving daily balance snapshot: {}", e.getMessage());
            throw new DatabaseException("Failed to save daily balance", e);
        }
    }

    @Override
    public BigDecimal calculateMonthlyAverageBalance(Long accountId, int year, int month) {
        // Try calling MySQL function fn_monthly_average_balance
        String funcSql = "SELECT fn_monthly_average_balance(?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(funcSql)) {
            ps.setLong(1, accountId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal result = rs.getBigDecimal(1);
                    if (result != null) {
                        return result;
                    }
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("MySQL fn_monthly_average_balance failed (falling back to Java MAB calculation): {}", e.getMessage());
        }

        // Java fallback calculation:
        YearMonth ym = YearMonth.of(year, month);
        int daysInMonth = ym.lengthOfMonth();
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        String sql = "SELECT COALESCE(SUM(closing_balance), 0), COUNT(*) FROM account_daily_balance " +
                "WHERE account_id = ? AND balance_date BETWEEN ? AND ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal sum = rs.getBigDecimal(1);
                    int count = rs.getInt(2);
                    if (count > 0) {
                        // Average over recorded days
                        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
                    }
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error calculating MAB in fallback: {}", e.getMessage());
        }

        // If no daily records exist, fallback to current balance of the account
        String balSql = "SELECT balance FROM accounts WHERE account_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(balSql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Failed to query fallback balance: {}", e.getMessage());
        }

        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateQuarterlyInterest(Long accountId, int year, int quarter) {
        // Try calling MySQL function fn_quarterly_interest
        String funcSql = "SELECT fn_quarterly_interest(?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(funcSql)) {
            ps.setLong(1, accountId);
            ps.setInt(2, year);
            ps.setInt(3, quarter);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal result = rs.getBigDecimal(1);
                    if (result != null) {
                        return result;
                    }
                }
            }
        } catch (SQLException e) {
            AppLogger.warn("MySQL fn_quarterly_interest failed (falling back to Java computation): {}", e.getMessage());
        }

        // Java fallback calculation:
        // Query interest rate from account
        BigDecimal rate = BigDecimal.ZERO;
        String rateSql = "SELECT interest_rate FROM accounts WHERE account_id = ? AND account_type = 'SAVINGS'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(rateSql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rate = rs.getBigDecimal("interest_rate");
                } else {
                    return BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Failed to query savings interest rate: {}", e.getMessage());
            return BigDecimal.ZERO;
        }

        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        int startMonth = (quarter - 1) * 3 + 1;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (int m = startMonth; m < startMonth + 3; m++) {
            BigDecimal mab = calculateMonthlyAverageBalance(accountId, year, m);
            // monthly interest = MAB * (rate / 1200)
            BigDecimal monthlyInterest = mab.multiply(rate)
                    .divide(BigDecimal.valueOf(1200), 4, RoundingMode.HALF_UP);
            totalInterest = totalInterest.add(monthlyInterest);
        }

        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<AccountDailyBalance> getDailyBalances(Long accountId, LocalDate from, LocalDate to) {
        String sql = "SELECT * FROM account_daily_balance WHERE account_id = ? AND balance_date BETWEEN ? AND ? " +
                "ORDER BY balance_date ASC";
        List<AccountDailyBalance> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AccountDailyBalance b = new AccountDailyBalance();
                    b.setSnapshotId(rs.getLong("snapshot_id"));
                    b.setAccountId(rs.getLong("account_id"));
                    Date d = rs.getDate("balance_date");
                    if (d != null) b.setBalanceDate(d.toLocalDate());
                    b.setClosingBalance(rs.getBigDecimal("closing_balance"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) b.setCreatedAt(ts.toLocalDateTime());
                    list.add(b);
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error retrieving daily balances: {}", e.getMessage());
            throw new DatabaseException("Failed to get daily balances", e);
        }
        return list;
    }
}
