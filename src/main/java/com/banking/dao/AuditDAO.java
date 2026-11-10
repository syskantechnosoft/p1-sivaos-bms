package com.banking.dao;

import com.banking.model.LoginAudit;
import com.banking.model.TransactionAudit;

import java.util.List;

/**
 * Data Access Object contract for Login and Transaction Auditing.
 */
public interface AuditDAO {

    LoginAudit saveLoginAudit(LoginAudit audit);

    int getConsecutiveFailedAttempts(String username);

    List<LoginAudit> getRecentLoginAudits(int limit);

    List<TransactionAudit> getRecentTransactionAudits(int limit);
}
