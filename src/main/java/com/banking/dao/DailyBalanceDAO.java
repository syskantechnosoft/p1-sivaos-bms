package com.banking.dao;

import com.banking.model.AccountDailyBalance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Access Object contract for Account Daily Balance snapshots and MAB calculations.
 */
public interface DailyBalanceDAO {

    void captureDailyBalances(LocalDate date);

    void saveOrUpdate(AccountDailyBalance balance);

    BigDecimal calculateMonthlyAverageBalance(Long accountId, int year, int month);

    BigDecimal calculateQuarterlyInterest(Long accountId, int year, int quarter);

    List<AccountDailyBalance> getDailyBalances(Long accountId, LocalDate from, LocalDate to);
}
