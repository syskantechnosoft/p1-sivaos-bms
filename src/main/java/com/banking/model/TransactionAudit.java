package com.banking.model;

import com.banking.enums.AuditOperation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit domain entity recording data modifications on transactions.
 */
public class TransactionAudit {

    private Long auditId;
    private Long transactionId;
    private AuditOperation operation;
    private BigDecimal oldAmount;
    private BigDecimal newAmount;
    private String oldStatus;
    private String newStatus;
    private String changedBy;
    private LocalDateTime auditTime;

    public TransactionAudit() {
        this.auditTime = LocalDateTime.now();
    }

    public TransactionAudit(Long transactionId, AuditOperation operation,
                            BigDecimal oldAmount, BigDecimal newAmount,
                            String oldStatus, String newStatus, String changedBy) {
        this.transactionId = transactionId;
        this.operation = operation;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.auditTime = LocalDateTime.now();
    }

    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public AuditOperation getOperation() {
        return operation;
    }

    public void setOperation(AuditOperation operation) {
        this.operation = operation;
    }

    public BigDecimal getOldAmount() {
        return oldAmount;
    }

    public void setOldAmount(BigDecimal oldAmount) {
        this.oldAmount = oldAmount;
    }

    public BigDecimal getNewAmount() {
        return newAmount;
    }

    public void setNewAmount(BigDecimal newAmount) {
        this.newAmount = newAmount;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(LocalDateTime auditTime) {
        this.auditTime = auditTime;
    }

    @Override
    public String toString() {
        return "TransactionAudit{" +
                "auditId=" + auditId +
                ", transactionId=" + transactionId +
                ", operation=" + operation +
                ", changedBy='" + changedBy + '\'' +
                ", auditTime=" + auditTime +
                '}';
    }
}
