package com.banking.service.impl;

import com.banking.config.DatabaseConfig;
import com.banking.dao.AuditDAO;
import com.banking.dao.CustomerDAO;
import com.banking.enums.CustomerStatus;
import com.banking.enums.LoginStatus;
import com.banking.exceptions.AccountLockedException;
import com.banking.exceptions.AuthenticationException;
import com.banking.model.Customer;
import com.banking.model.LoginAudit;
import com.banking.model.UserSession;
import com.banking.security.AccountLockoutManager;
import com.banking.security.SessionManager;
import com.banking.service.AuthService;
import com.banking.util.AppLogger;
import com.banking.util.PasswordUtil;

import java.util.Optional;

/**
 * Implementation of AuthService supporting BCrypt verification,
 * consecutive failed attempt lockout, and inactivity session tracking.
 */
public class AuthServiceImpl implements AuthService {

    private final CustomerDAO customerDAO;
    private final AuditDAO auditDAO;
    private final AccountLockoutManager lockoutManager;

    public AuthServiceImpl(CustomerDAO customerDAO, AuditDAO auditDAO) {
        this.customerDAO = customerDAO;
        this.auditDAO = auditDAO;
        this.lockoutManager = new AccountLockoutManager(customerDAO, auditDAO);
    }

    public AuthServiceImpl(CustomerDAO customerDAO, AuditDAO auditDAO, AccountLockoutManager lockoutManager) {
        this.customerDAO = customerDAO;
        this.auditDAO = auditDAO;
        this.lockoutManager = lockoutManager;
    }

    @Override
    public Customer loginCustomer(String identifier, String password, String ipAddress, String deviceInfo) {
        if (identifier == null || identifier.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Email/Customer Code and Password are required.");
        }

        String trimmed = identifier.trim();
        Optional<Customer> optCustomer = trimmed.contains("@")
                ? customerDAO.findByEmail(trimmed)
                : customerDAO.findByCustomerCode(trimmed);

        if (optCustomer.isEmpty()) {
            // Record unknown user failure in audit
            auditDAO.saveLoginAudit(new LoginAudit(
                    null,
                    trimmed,
                    LoginStatus.FAILED,
                    ipAddress,
                    deviceInfo,
                    "User not found"
            ));
            AppLogger.logAudit("AUTH_FAILED: Login attempt with non-existent identifier '{}'", trimmed);
            throw new AuthenticationException("Invalid username or password.");
        }

        Customer customer = optCustomer.get();

        // Check if customer is already locked/blocked
        if (customer.getStatus() == CustomerStatus.BLOCKED) {
            auditDAO.saveLoginAudit(new LoginAudit(
                    customer.getCustomerId(),
                    customer.getEmail(),
                    LoginStatus.LOCKED,
                    ipAddress,
                    deviceInfo,
                    "Attempted login on locked account"
            ));
            AppLogger.logAudit("AUTH_REJECTED: Account {} is currently locked/blocked.", customer.getCustomerCode());
            throw new AccountLockedException("Your account is currently LOCKED. Please contact bank administration to unlock.");
        }

        // Verify password hash
        boolean passwordMatches = PasswordUtil.verify(password, customer.getPasswordHash());

        if (!passwordMatches) {
            // Records failure and triggers automated lock if 3 attempts reached
            lockoutManager.recordFailedAttempt(customer, customer.getEmail(), ipAddress, deviceInfo, "Invalid password");
            int failedSoFar = auditDAO.getConsecutiveFailedAttempts(customer.getEmail());
            int remaining = Math.max(0, DatabaseConfig.getMaxFailedAttempts() - failedSoFar);
            throw new AuthenticationException("Invalid password. Warning: " + remaining + " attempts remaining before account lockout.");
        }

        // Password is correct: record success & establish session
        lockoutManager.recordSuccessfulLogin(customer, ipAddress, deviceInfo);
        SessionManager.createCustomerSession(customer);
        return customer;
    }

    @Override
    public boolean loginAdmin(String username, String password, String ipAddress, String deviceInfo) {
        String expectedUser = DatabaseConfig.getAdminUsername();
        String expectedPass = DatabaseConfig.getAdminPassword();

        if (username != null && username.equalsIgnoreCase(expectedUser) &&
                password != null && password.equals(expectedPass)) {
            auditDAO.saveLoginAudit(new LoginAudit(
                    null,
                    username,
                    LoginStatus.SUCCESS,
                    ipAddress,
                    deviceInfo,
                    "Administrator login"
            ));
            SessionManager.createAdminSession(username);
            return true;
        }

        auditDAO.saveLoginAudit(new LoginAudit(
                null,
                username,
                LoginStatus.FAILED,
                ipAddress,
                deviceInfo,
                "Invalid administrator credentials"
        ));
        AppLogger.logAudit("AUTH_FAILED: Failed administrator login attempt for user '{}'", username);
        throw new AuthenticationException("Invalid administrator credentials.");
    }

    @Override
    public void logout() {
        UserSession session = SessionManager.getCurrentSession();
        if (session != null) {
            Long customerId = session.getCustomer() != null ? session.getCustomer().getCustomerId() : null;
            auditDAO.saveLoginAudit(new LoginAudit(
                    customerId,
                    session.getSessionUser(),
                    LoginStatus.LOGOUT,
                    "127.0.0.1",
                    "CLI Console",
                    "Explicit user logout"
            ));
            SessionManager.invalidateCurrentSession();
        }
    }
}
