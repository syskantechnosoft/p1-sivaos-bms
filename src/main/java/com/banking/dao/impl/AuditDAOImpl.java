package com.banking.dao.impl;

import com.banking.config.DBConnection;
import com.banking.dao.AuditDAO;
import com.banking.enums.AuditOperation;
import com.banking.enums.LoginStatus;
import com.banking.exceptions.DatabaseException;
import com.banking.model.LoginAudit;
import com.banking.model.TransactionAudit;
import com.banking.util.AppLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of AuditDAO.
 */
public class AuditDAOImpl implements AuditDAO {

    @Override
    public LoginAudit saveLoginAudit(LoginAudit audit) {
        String sql = "INSERT INTO login_audit (customer_id, username, login_status, ip_address, " +
                "device_info, failure_reason) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (audit.getCustomerId() != null) {
                ps.setLong(1, audit.getCustomerId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, audit.getUsername());
            ps.setString(3, audit.getLoginStatus().name());
            ps.setString(4, audit.getIpAddress());
            ps.setString(5, audit.getDeviceInfo());
            ps.setString(6, audit.getFailureReason());

            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    audit.setLoginAuditId(gk.getLong(1));
                }
            }
            return audit;
        } catch (SQLException e) {
            AppLogger.error("Error saving login audit: {}", e.getMessage());
            // Auditing error shouldn't crash main app, but we log
            return audit;
        }
    }

    @Override
    public int getConsecutiveFailedAttempts(String username) {
        String sql = "SELECT COUNT(*) FROM login_audit " +
                "WHERE LOWER(username) = LOWER(?) " +
                "  AND login_status = 'FAILED' " +
                "  AND login_time > COALESCE(" +
                "      (SELECT MAX(login_time) FROM login_audit WHERE LOWER(username) = LOWER(?) AND login_status IN ('SUCCESS', 'LOCKED')), " +
                "      '1970-01-01 00:00:00'" +
                "  )";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error querying consecutive failed login attempts: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public List<LoginAudit> getRecentLoginAudits(int limit) {
        String sql = "SELECT * FROM login_audit ORDER BY login_time DESC, login_audit_id DESC LIMIT ?";
        List<LoginAudit> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LoginAudit a = new LoginAudit();
                    a.setLoginAuditId(rs.getLong("login_audit_id"));
                    long cid = rs.getLong("customer_id");
                    if (!rs.wasNull()) {
                        a.setCustomerId(cid);
                    }
                    a.setUsername(rs.getString("username"));
                    String status = rs.getString("login_status");
                    if (status != null) {
                        a.setLoginStatus(LoginStatus.valueOf(status));
                    }
                    a.setIpAddress(rs.getString("ip_address"));
                    a.setDeviceInfo(rs.getString("device_info"));
                    a.setFailureReason(rs.getString("failure_reason"));
                    Timestamp ts = rs.getTimestamp("login_time");
                    if (ts != null) {
                        a.setLoginTime(ts.toLocalDateTime());
                    }
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error retrieving login audits: {}", e.getMessage());
            throw new DatabaseException("Failed to load login audits", e);
        }
        return list;
    }

    @Override
    public List<TransactionAudit> getRecentTransactionAudits(int limit) {
        String sql = "SELECT * FROM transaction_audit ORDER BY audit_time DESC, audit_id DESC LIMIT ?";
        List<TransactionAudit> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransactionAudit a = new TransactionAudit();
                    a.setAuditId(rs.getLong("audit_id"));
                    a.setTransactionId(rs.getLong("transaction_id"));
                    String op = rs.getString("operation");
                    if (op != null) {
                        a.setOperation(AuditOperation.valueOf(op));
                    }
                    a.setOldAmount(rs.getBigDecimal("old_amount"));
                    a.setNewAmount(rs.getBigDecimal("new_amount"));
                    a.setOldStatus(rs.getString("old_status"));
                    a.setNewStatus(rs.getString("new_status"));
                    a.setChangedBy(rs.getString("changed_by"));
                    Timestamp ts = rs.getTimestamp("audit_time");
                    if (ts != null) {
                        a.setAuditTime(ts.toLocalDateTime());
                    }
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error retrieving transaction audits: {}", e.getMessage());
            throw new DatabaseException("Failed to load transaction audits", e);
        }
        return list;
    }
}
