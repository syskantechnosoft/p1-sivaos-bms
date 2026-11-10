package com.banking.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Centralized logging utility that delegates to dedicated SLF4J / Logback loggers
 * to generate transactions.log, audit.log, account.log, and application.log.
 */
public final class AppLogger {

    private static final Logger APP_LOGGER = LoggerFactory.getLogger(AppLogger.class);
    private static final Logger TXN_LOGGER = LoggerFactory.getLogger("com.banking.log.transaction");
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("com.banking.log.audit");
    private static final Logger ACCT_LOGGER = LoggerFactory.getLogger("com.banking.log.account");

    static {
        // Ensure logs directory exists
        File logDir = new File("logs");
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (created) {
                APP_LOGGER.info("Created logs directory at: {}", logDir.getAbsolutePath());
            }
        }
    }

    private AppLogger() {
        // Private constructor for utility class
    }

    public static void logTransaction(String message, Object... params) {
        TXN_LOGGER.info(message, params);
    }

    public static void logAudit(String message, Object... params) {
        AUDIT_LOGGER.info(message, params);
    }

    public static void logAccount(String message, Object... params) {
        ACCT_LOGGER.info(message, params);
    }

    public static void info(String message, Object... params) {
        APP_LOGGER.info(message, params);
    }

    public static void warn(String message, Object... params) {
        APP_LOGGER.warn(message, params);
    }

    public static void error(String message, Throwable t) {
        APP_LOGGER.error(message, t);
    }

    public static void error(String message, Object... params) {
        APP_LOGGER.error(message, params);
    }
}
