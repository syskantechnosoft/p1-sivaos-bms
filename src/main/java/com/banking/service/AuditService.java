package com.banking.service;

import com.banking.model.LoginAudit;
import com.banking.model.TransactionAudit;

import java.util.List;

/**
 * Service contract for querying login and transaction audit records.
 */
public interface AuditService {

    List<LoginAudit> getRecentLoginAudits(int limit);

    List<TransactionAudit> getRecentTransactionAudits(int limit);
}
