package com.banking.model;

import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Account domain entity representing bank accounts.
 */
public class Account {

    private Long accountId;
    private Long customerId;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private BigDecimal interestRate;
    private BigDecimal minimumBalance;
    private LocalDate openingDate;
    private LocalDate maturityDate;
    private AccountStatus status;
    private LocalDateTime createdAt;

    public Account() {
        this.balance = BigDecimal.ZERO;
        this.interestRate = BigDecimal.ZERO;
        this.minimumBalance = BigDecimal.ZERO;
        this.openingDate = LocalDate.now();
        this.status = AccountStatus.ACTIVE;
    }

    public Account(Long customerId, String accountNumber, AccountType accountType,
                   BigDecimal balance, BigDecimal interestRate, BigDecimal minimumBalance,
                   LocalDate openingDate, LocalDate maturityDate) {
        this();
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.interestRate = interestRate != null ? interestRate : BigDecimal.ZERO;
        this.minimumBalance = minimumBalance != null ? minimumBalance : BigDecimal.ZERO;
        this.openingDate = openingDate != null ? openingDate : LocalDate.now();
        this.maturityDate = maturityDate;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }

    public void setMinimumBalance(BigDecimal minimumBalance) {
        this.minimumBalance = minimumBalance;
    }

    public LocalDate getOpeningDate() {
        return openingDate;
    }

    public void setOpeningDate(LocalDate openingDate) {
        this.openingDate = openingDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    public boolean isSavings() {
        return this.accountType == AccountType.SAVINGS;
    }

    public boolean isDeposit() {
        return this.accountType == AccountType.DEPOSIT;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + accountId +
                ", number='" + accountNumber + '\'' +
                ", type=" + accountType +
                ", balance=" + balance +
                ", status=" + status +
                '}';
    }
}
