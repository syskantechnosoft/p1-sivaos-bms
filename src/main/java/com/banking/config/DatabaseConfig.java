package com.banking.config;

import com.banking.util.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads and exposes application and database configuration parameters.
 */
public final class DatabaseConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                PROPERTIES.load(in);
            } else {
                AppLogger.warn("application.properties not found on classpath, using defaults.");
            }
        } catch (IOException e) {
            AppLogger.error("Failed to load application.properties: {}", e.getMessage());
        }
    }

    private DatabaseConfig() {
    }

    public static String getUrl() {
        String env = System.getenv("DB_URL");
        if (env != null && !env.isEmpty()) return env;
        return PROPERTIES.getProperty("db.url",
                "jdbc:mysql://localhost:3306/banking_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    }

    public static String getUser() {
        String env = System.getenv("DB_USER");
        if (env != null && !env.isEmpty()) return env;
        return PROPERTIES.getProperty("db.user", "root");
    }

    public static String getPassword() {
        String env = System.getenv("DB_PASSWORD");
        if (env != null) return env;
        return PROPERTIES.getProperty("db.password", "root");
    }

    public static String getDriver() {
        return PROPERTIES.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
    }

    public static int getSessionTimeoutMinutes() {
        String val = PROPERTIES.getProperty("security.session.timeout.minutes", "2");
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    public static int getMaxFailedAttempts() {
        String val = PROPERTIES.getProperty("security.max.failed.attempts", "3");
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return 3;
        }
    }

    public static String getAdminUsername() {
        return PROPERTIES.getProperty("admin.username", "admin@bank.com");
    }

    public static String getAdminPassword() {
        return PROPERTIES.getProperty("admin.password", "Admin@123");
    }

    public static String getBankName() {
        return PROPERTIES.getProperty("bank.name", "Secure Retail Bank (SRB)");
    }
}
