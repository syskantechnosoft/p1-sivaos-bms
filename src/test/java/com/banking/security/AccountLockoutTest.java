package com.banking.security;

import com.banking.dao.AuditDAO;
import com.banking.dao.CustomerDAO;
import com.banking.enums.CustomerStatus;
import com.banking.enums.LoginStatus;
import com.banking.exceptions.AccountLockedException;
import com.banking.model.Customer;
import com.banking.model.LoginAudit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Lockout after 3 Failed Login Attempts Tests")
class AccountLockoutTest {

    @Mock
    private CustomerDAO customerDAO;

    @Mock
    private AuditDAO auditDAO;

    private AccountLockoutManager lockoutManager;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        lockoutManager = new AccountLockoutManager(customerDAO, auditDAO);
        testCustomer = new Customer();
        testCustomer.setCustomerId(10L);
        testCustomer.setCustomerCode("CUST010");
        testCustomer.setEmail("victim@bank.com");
        testCustomer.setStatus(CustomerStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should record failed attempt without locking when below 3 attempts")
    void shouldNotLockBeforeThreeAttempts() {
        when(auditDAO.getConsecutiveFailedAttempts("victim@bank.com")).thenReturn(1);

        lockoutManager.recordFailedAttempt(testCustomer, "victim@bank.com", "127.0.0.1", "CLI", "Bad password");

        verify(auditDAO).saveLoginAudit(argThat(audit -> audit.getLoginStatus() == LoginStatus.FAILED));
        verify(customerDAO, never()).updateStatus(anyLong(), any());
        assertEquals(CustomerStatus.ACTIVE, testCustomer.getStatus());
    }

    @Test
    @DisplayName("Should lock customer account on 3rd consecutive failed login attempt")
    void shouldLockOnThirdFailedAttempt() {
        when(auditDAO.getConsecutiveFailedAttempts("victim@bank.com")).thenReturn(3);

        AccountLockedException ex = assertThrows(AccountLockedException.class, () ->
                lockoutManager.recordFailedAttempt(testCustomer, "victim@bank.com", "127.0.0.1", "CLI", "Bad password"));

        assertTrue(ex.getMessage().contains("LOCKED"));
        assertEquals(CustomerStatus.BLOCKED, testCustomer.getStatus());
        verify(customerDAO).updateStatus(10L, CustomerStatus.BLOCKED);
        verify(auditDAO).saveLoginAudit(argThat(audit -> audit.getLoginStatus() == LoginStatus.LOCKED));
    }

    @Test
    @DisplayName("Should unlock customer account when unlocked by administrator")
    void shouldUnlockCustomerAccount() {
        when(customerDAO.updateStatus(10L, CustomerStatus.ACTIVE)).thenReturn(true);

        boolean unlocked = lockoutManager.unlockAccount(10L);

        assertTrue(unlocked);
        verify(customerDAO).updateStatus(10L, CustomerStatus.ACTIVE);
    }
}
