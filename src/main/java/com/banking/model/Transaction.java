package com.banking.model;

import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction domain entity representing a financial movement.
 */
public class Transaction {

    private Long transactionId;
    private String transactionRef;
    private Long accountId;
    private Long destinationAccountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime transactionDate;
    private TransactionStatus status;

    public Transaction() {
        this.transactionDate = LocalDateTime.now();
        this.status = TransactionStatus.SUCCESS;
    }

    public Transaction(String transactionRef, Long accountId, Long destinationAccountId,
                       TransactionType transactionType, BigDecimal amount,
                       BigDecimal balanceBefore, BigDecimal balanceAfter,
                       String description, TransactionStatus status) {
        this.transactionRef = transactionRef;
        this.accountId = accountId;
        this.destinationAccountId = destinationAccountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.transactionDate = LocalDateTime.now();
        this.status = status != null ? status : TransactionStatus.SUCCESS;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "ref='" + transactionRef + '\'' +
                ", accountId=" + accountId +
                ", type=" + transactionType +
                ", amount=" + amount +
                ", date=" + transactionDate +
                ", status=" + status +
                '}';
    }
}
