package com.banking.exceptions;

import java.math.BigDecimal;

/**
 * Thrown when a withdrawal or fund transfer exceeds available balance
 * or violates minimum balance policy.
 */
public class InsufficientFundsException extends BankingException {

    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(String message) {
        super("TXN_INSUFFICIENT_FUNDS", message);
        this.currentBalance = null;
        this.requestedAmount = null;
    }

    public InsufficientFundsException(String message, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super("TXN_INSUFFICIENT_FUNDS", message);
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
