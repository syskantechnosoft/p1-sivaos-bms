package com.banking.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility to generate unique, traceable banking transaction references.
 */
public final class TransactionRefUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private TransactionRefUtil() {
    }

    /**
     * Generates a standard transaction reference: TXN-yyyyMMddHHmmss-XXXX
     */
    public static String generateReference(String prefix) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        int suffix = 1000 + RANDOM.nextInt(9000);
        String pfx = (prefix != null && !prefix.trim().isEmpty()) ? prefix.trim().toUpperCase() : "TXN";
        return pfx + "-" + timestamp + "-" + suffix;
    }

    /**
     * Generates standard quarterly interest reference: INT-<accountId>-<year>-Q<quarter>
     */
    public static String generateQuarterlyInterestReference(Long accountId, int year, int quarter) {
        return "INT-" + accountId + "-" + year + "-Q" + quarter;
    }
}
