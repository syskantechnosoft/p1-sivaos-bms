package com.banking.service;

import com.banking.dao.AccountDAO;
import com.banking.dao.DailyBalanceDAO;
import com.banking.dao.TransactionDAO;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.service.impl.InterestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Quarterly Interest Service Tests")
class InterestServiceTest {

    @Mock
    private AccountDAO accountDAO;

    @Mock
    private TransactionDAO transactionDAO;

    @Mock
    private DailyBalanceDAO dailyBalanceDAO;

    private InterestService interestService;

    @BeforeEach
    void setUp() {
        interestService = new InterestServiceImpl(accountDAO, transactionDAO, dailyBalanceDAO);
    }

    @Test
    @DisplayName("Should calculate quarterly interest for savings account")
    void shouldCalculateQuarterlyInterest() {
        when(dailyBalanceDAO.calculateQuarterlyInterest(1L, 2026, 1))
                .thenReturn(new BigDecimal("1000.00"));

        BigDecimal interest = interestService.calculateInterestForQuarter(1L, 2026, 1);

        assertNotNull(interest);
        assertEquals(new BigDecimal("1000.00"), interest);
        verify(dailyBalanceDAO).calculateQuarterlyInterest(1L, 2026, 1);
    }

    @Test
    @DisplayName("Should skip interest credit if interest already credited for account and quarter")
    void shouldSkipAlreadyCreditedInterest() {
        Account savings = new Account();
        savings.setAccountId(1L);
        savings.setAccountNumber("SB100001");
        savings.setAccountType(AccountType.SAVINGS);
        savings.setStatus(AccountStatus.ACTIVE);
        savings.setBalance(new BigDecimal("100000.00"));

        when(accountDAO.findSavingsAccountsForInterest()).thenReturn(Collections.singletonList(savings));
        when(transactionDAO.findByReference("INT-1-2026-Q1")).thenReturn(Optional.of(new Transaction()));

        List<Transaction> credited = interestService.creditQuarterlyInterestForAllSavings(2026, 1);

        assertTrue(credited.isEmpty());
        verify(dailyBalanceDAO, never()).calculateQuarterlyInterest(anyLong(), anyInt(), anyInt());
    }
}
