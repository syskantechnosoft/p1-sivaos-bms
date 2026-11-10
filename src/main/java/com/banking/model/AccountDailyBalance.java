package com.banking.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain entity recording the daily closing snapshot balance for interest calculations.
 */
public class AccountDailyBalance {

    private Long snapshotId;
    private Long accountId;
    private LocalDate balanceDate;
    private BigDecimal closingBalance;
    private LocalDateTime createdAt;

    public AccountDailyBalance() {
        this.createdAt = LocalDateTime.now();
    }

    public AccountDailyBalance(Long accountId, LocalDate balanceDate, BigDecimal closingBalance) {
        this.accountId = accountId;
        this.balanceDate = balanceDate;
        this.closingBalance = closingBalance;
        this.createdAt = LocalDateTime.now();
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public LocalDate getBalanceDate() {
        return balanceDate;
    }

    public void setBalanceDate(LocalDate balanceDate) {
        this.balanceDate = balanceDate;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AccountDailyBalance{" +
                "accountId=" + accountId +
                ", date=" + balanceDate +
                ", balance=" + closingBalance +
                '}';
    }
}
