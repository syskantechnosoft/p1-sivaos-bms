package com.banking.security;

import com.banking.config.DatabaseConfig;
import com.banking.dao.AuditDAO;
import com.banking.dao.CustomerDAO;
import com.banking.enums.CustomerStatus;
import com.banking.enums.LoginStatus;
import com.banking.exceptions.AccountLockedException;
import com.banking.model.Customer;
import com.banking.model.LoginAudit;
import com.banking.util.AppLogger;

/**
 * Manages failed login attempts and triggers automated account locking
 * when consecutive failures reach threshold (default: 3 attempts).
 */
public class AccountLockoutManager {

    private final CustomerDAO customerDAO;
    private final AuditDAO auditDAO;
    private final int maxAttempts;

    public AccountLockoutManager(CustomerDAO customerDAO, AuditDAO auditDAO) {
        this.customerDAO = customerDAO;
        this.auditDAO = auditDAO;
        this.maxAttempts = DatabaseConfig.getMaxFailedAttempts();
    }

    /**
     * Checks if a customer account is currently locked/blocked.
     */
    public boolean isLocked(Customer customer) {
        return customer != null && customer.getStatus() == CustomerStatus.BLOCKED;
    }

    /**
     * Handles a failed login attempt:
     * 1. Records FAILED audit record in login_audit table.
     * 2. Checks consecutive failed attempts.
     * 3. If >= 3 attempts, locks customer profile (BLOCKED status),
     *    records LOCKED audit entry, and throws AccountLockedException.
     */
    public void recordFailedAttempt(Customer customer, String username, String ipAddress, String deviceInfo, String reason) {
        Long customerId = customer != null ? customer.getCustomerId() : null;

        // Record failed attempt
        LoginAudit failedAudit = new LoginAudit(
                customerId,
                username,
                LoginStatus.FAILED,
                ipAddress,
                deviceInfo,
                reason
        );
        auditDAO.saveLoginAudit(failedAudit);
        AppLogger.logAudit("AUTH_FAILED: User '{}' failed login attempt. Reason: {}", username, reason);

        if (customer != null) {
            int failedCount = auditDAO.getConsecutiveFailedAttempts(username);
            AppLogger.warn("Consecutive failed attempts for user '{}': {}/{}", username, failedCount, maxAttempts);

            if (failedCount >= maxAttempts) {
                // Lock account
                customerDAO.updateStatus(customer.getCustomerId(), CustomerStatus.BLOCKED);
                customer.setStatus(CustomerStatus.BLOCKED);

                LoginAudit lockAudit = new LoginAudit(
                        customerId,
                        username,
                        LoginStatus.LOCKED,
                        ipAddress,
                        deviceInfo,
                        "Account locked automatically after " + failedCount + " consecutive failed attempts"
                );
                auditDAO.saveLoginAudit(lockAudit);

                AppLogger.logAudit("ACCOUNT_LOCKED: Customer {} ({}) has been LOCKED after {} consecutive failed login attempts.",
                        customer.getCustomerCode(), username, failedCount);

                throw new AccountLockedException("Your account has been LOCKED due to " +
                        failedCount + " consecutive failed login attempts. Please contact bank administration.");
            }
        }
    }

    /**
     * Records a successful login event in the audit trail.
     */
    public void recordSuccessfulLogin(Customer customer, String ipAddress, String deviceInfo) {
        LoginAudit successAudit = new LoginAudit(
                customer.getCustomerId(),
                customer.getEmail(),
                LoginStatus.SUCCESS,
                ipAddress,
                deviceInfo,
                null
        );
        auditDAO.saveLoginAudit(successAudit);
        AppLogger.logAudit("AUTH_SUCCESS: Customer {} ({}) authenticated successfully.",
                customer.getCustomerCode(), customer.getEmail());
    }

    /**
     * Unlocks a customer account (administrative operation).
     */
    public boolean unlockAccount(Long customerId) {
        boolean updated = customerDAO.updateStatus(customerId, CustomerStatus.ACTIVE);
        if (updated) {
            AppLogger.logAudit("ACCOUNT_UNLOCKED: Customer ID {} unlocked by administrator.", customerId);
        }
        return updated;
    }
}
