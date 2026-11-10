package com.banking.service;

import com.banking.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service contract for quarterly interest calculations and automatic crediting to savings accounts.
 */
public interface InterestService {

    BigDecimal calculateInterestForQuarter(Long accountId, int year, int quarter);

    List<Transaction> creditQuarterlyInterestForAllSavings(int year, int quarter);

    void captureDailyClosingBalances();
}
