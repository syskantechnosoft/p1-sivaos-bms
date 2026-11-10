package com.banking.service.impl;

import com.banking.dao.AuditDAO;
import com.banking.model.LoginAudit;
import com.banking.model.TransactionAudit;
import com.banking.service.AuditService;

import java.util.List;

/**
 * Implementation of AuditService.
 */
public class AuditServiceImpl implements AuditService {

    private final AuditDAO auditDAO;

    public AuditServiceImpl(AuditDAO auditDAO) {
        this.auditDAO = auditDAO;
    }

    @Override
    public List<LoginAudit> getRecentLoginAudits(int limit) {
        return auditDAO.getRecentLoginAudits(limit);
    }

    @Override
    public List<TransactionAudit> getRecentTransactionAudits(int limit) {
        return auditDAO.getRecentTransactionAudits(limit);
    }
}
