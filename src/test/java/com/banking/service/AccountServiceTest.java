package com.banking.service;

import com.banking.dao.AccountDAO;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.exceptions.ValidationException;
import com.banking.model.Account;
import com.banking.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Service Tests")
class AccountServiceTest {

    @Mock
    private AccountDAO accountDAO;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountDAO);
    }

    @Test
    @DisplayName("Should open savings account with default 4% interest rate and min balance")
    void shouldOpenSavingsAccountWithDefaults() {
        when(accountDAO.generateNextAccountNumber(AccountType.SAVINGS)).thenReturn("SB100005");
        when(accountDAO.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account created = accountService.openAccount(
                1L,
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                null,
                null,
                null
        );

        assertNotNull(created);
        assertEquals("SB100005", created.getAccountNumber());
        assertEquals(AccountType.SAVINGS, created.getAccountType());
        assertEquals(0, new BigDecimal("4.00").compareTo(created.getInterestRate()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(created.getMinimumBalance()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(created.getBalance()));
        verify(accountDAO).save(any(Account.class));
    }

    @Test
    @DisplayName("Should prevent closing account if balance is greater than zero")
    void shouldPreventClosingAccountWithPositiveBalance() {
        Account acc = new Account();
        acc.setAccountId(1L);
        acc.setAccountNumber("SB100001");
        acc.setBalance(new BigDecimal("250.00"));
        acc.setStatus(AccountStatus.ACTIVE);

        when(accountDAO.findById(1L)).thenReturn(Optional.of(acc));

        ValidationException ex = assertThrows(ValidationException.class, () ->
                accountService.closeAccount(1L));

        assertTrue(ex.getMessage().contains("Cannot close account with positive balance"));
        verify(accountDAO, never()).updateStatus(anyLong(), any());
    }

    @Test
    @DisplayName("Should freeze and unfreeze account successfully")
    void shouldFreezeAndUnfreezeAccount() {
        Account acc = new Account();
        acc.setAccountId(1L);
        acc.setAccountNumber("SB100001");
        acc.setStatus(AccountStatus.ACTIVE);

        when(accountDAO.findById(1L)).thenReturn(Optional.of(acc));
        when(accountDAO.updateStatus(1L, AccountStatus.FROZEN)).thenReturn(true);
        when(accountDAO.updateStatus(1L, AccountStatus.ACTIVE)).thenReturn(true);

        assertTrue(accountService.freezeAccount(1L));
        assertTrue(accountService.unfreezeAccount(1L));

        verify(accountDAO).updateStatus(1L, AccountStatus.FROZEN);
        verify(accountDAO).updateStatus(1L, AccountStatus.ACTIVE);
    }
}
