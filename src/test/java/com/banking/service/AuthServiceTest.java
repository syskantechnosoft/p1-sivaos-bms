package com.banking.service;

import com.banking.dao.AuditDAO;
import com.banking.dao.CustomerDAO;
import com.banking.enums.CustomerStatus;
import com.banking.exceptions.AccountLockedException;
import com.banking.exceptions.AuthenticationException;
import com.banking.model.Customer;
import com.banking.security.AccountLockoutManager;
import com.banking.security.SessionManager;
import com.banking.service.impl.AuthServiceImpl;
import com.banking.util.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication Service Tests")
class AuthServiceTest {

    @Mock
    private CustomerDAO customerDAO;

    @Mock
    private AuditDAO auditDAO;

    @Mock
    private AccountLockoutManager lockoutManager;

    private AuthService authService;
    private Customer activeCustomer;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(customerDAO, auditDAO, lockoutManager);

        activeCustomer = new Customer();
        activeCustomer.setCustomerId(1L);
        activeCustomer.setCustomerCode("CUST001");
        activeCustomer.setEmail("arun@example.com");
        activeCustomer.setFirstName("Arun");
        activeCustomer.setLastName("Kumar");
        activeCustomer.setPasswordHash(PasswordUtil.hash("Pass@123"));
        activeCustomer.setStatus(CustomerStatus.ACTIVE);

        SessionManager.invalidateCurrentSession();
    }

    @AfterEach
    void tearDown() {
        SessionManager.invalidateCurrentSession();
    }

    @Test
    @DisplayName("Should successfully authenticate customer with valid credentials")
    void shouldLoginWithValidCredentials() {
        when(customerDAO.findByEmail("arun@example.com")).thenReturn(Optional.of(activeCustomer));

        Customer loggedIn = authService.loginCustomer("arun@example.com", "Pass@123", "127.0.0.1", "CLI");

        assertNotNull(loggedIn);
        assertEquals("CUST001", loggedIn.getCustomerCode());
        verify(lockoutManager).recordSuccessfulLogin(eq(activeCustomer), anyString(), anyString());
        assertTrue(SessionManager.hasActiveSession());
    }

    @Test
    @DisplayName("Should reject authentication for non-existent user")
    void shouldRejectNonExistentUser() {
        when(customerDAO.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () ->
                authService.loginCustomer("ghost@example.com", "Pass@123", "127.0.0.1", "CLI"));
    }

    @Test
    @DisplayName("Should immediately reject login if account is already BLOCKED")
    void shouldRejectBlockedAccount() {
        activeCustomer.setStatus(CustomerStatus.BLOCKED);
        when(customerDAO.findByEmail("arun@example.com")).thenReturn(Optional.of(activeCustomer));

        AccountLockedException ex = assertThrows(AccountLockedException.class, () ->
                authService.loginCustomer("arun@example.com", "Pass@123", "127.0.0.1", "CLI"));

        assertTrue(ex.getMessage().contains("LOCKED"));
    }

    @Test
    @DisplayName("Should handle wrong password and delegate to lockout manager")
    void shouldHandleWrongPassword() {
        when(customerDAO.findByEmail("arun@example.com")).thenReturn(Optional.of(activeCustomer));
        when(auditDAO.getConsecutiveFailedAttempts("arun@example.com")).thenReturn(1);

        assertThrows(AuthenticationException.class, () ->
                authService.loginCustomer("arun@example.com", "Wrong@999", "127.0.0.1", "CLI"));

        verify(lockoutManager).recordFailedAttempt(eq(activeCustomer), eq("arun@example.com"), anyString(), anyString(), anyString());
    }
}
