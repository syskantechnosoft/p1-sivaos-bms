package com.banking.service;

import com.banking.dao.AccountDAO;
import com.banking.dao.TransactionDAO;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.enums.TransactionType;
import com.banking.exceptions.InsufficientFundsException;
import com.banking.exceptions.InvalidTransactionException;
import com.banking.model.Account;
import com.banking.service.impl.TransactionServiceImpl;
import com.banking.validation.TransactionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Service Unit Tests")
class TransactionServiceTest {

    @Mock
    private AccountDAO accountDAO;

    @Mock
    private TransactionDAO transactionDAO;

    @Mock
    private Connection mockConnection;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(accountDAO, transactionDAO, new TransactionValidator());
    }

    @Test
    @DisplayName("Should reject withdrawal when balance is insufficient")
    void shouldRejectWithdrawalOnInsufficientBalance() {
        Account savings = new Account();
        savings.setAccountId(1L);
        savings.setAccountNumber("SB100001");
        savings.setAccountType(AccountType.SAVINGS);
        savings.setBalance(new BigDecimal("500.00"));
        savings.setMinimumBalance(new BigDecimal("100.00"));
        savings.setStatus(AccountStatus.ACTIVE);

        when(accountDAO.findByIdForUpdate(any(), eq(1L))).thenReturn(Optional.of(savings));

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () ->
                transactionService.withdraw(1L, new BigDecimal("1000.00"), "ATM"));

        assertTrue(ex.getMessage().contains("Insufficient balance"));
    }

    @Test
    @DisplayName("Should reject withdrawal when violating minimum savings balance policy")
    void shouldRejectWithdrawalViolatingMinimumBalance() {
        Account savings = new Account();
        savings.setAccountId(1L);
        savings.setAccountNumber("SB100001");
        savings.setAccountType(AccountType.SAVINGS);
        savings.setBalance(new BigDecimal("1200.00"));
        savings.setMinimumBalance(new BigDecimal("1000.00"));
        savings.setStatus(AccountStatus.ACTIVE);

        when(accountDAO.findByIdForUpdate(any(), eq(1L))).thenReturn(Optional.of(savings));

        // Withdraw 500 would leave 700, which is below 1000
        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () ->
                transactionService.withdraw(1L, new BigDecimal("500.00"), "ATM"));

        assertTrue(ex.getMessage().contains("minimum balance policy"));
    }

    @Test
    @DisplayName("Should reject transfer when source and destination are the same account")
    void shouldRejectTransferToSameAccount() {
        InvalidTransactionException ex = assertThrows(InvalidTransactionException.class, () ->
                transactionService.transfer(1L, 1L, new BigDecimal("500.00"), "Self transfer"));

        assertTrue(ex.getMessage().contains("different"));
        verify(accountDAO, never()).updateBalance(any(), anyLong(), any());
    }
}
