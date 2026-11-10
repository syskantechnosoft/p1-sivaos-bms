package com.banking.config;

import com.banking.exceptions.DatabaseException;
import com.banking.util.AppLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Factory for creating and managing JDBC database connections.
 */
public final class DBConnection {

    static {
        try {
            Class.forName(DatabaseConfig.getDriver());
        } catch (ClassNotFoundException e) {
            AppLogger.error("JDBC Driver class not found: {}", DatabaseConfig.getDriver());
            throw new DatabaseException("Failed to load JDBC driver: " + DatabaseConfig.getDriver(), e);
        }
    }

    private DBConnection() {
    }

    /**
     * Obtains a new JDBC database connection.
     *
     * @return open java.sql.Connection
     * @throws DatabaseException if connection cannot be established
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    DatabaseConfig.getUrl(),
                    DatabaseConfig.getUser(),
                    DatabaseConfig.getPassword()
            );
        } catch (SQLException e) {
            AppLogger.error("Failed to acquire JDBC connection to {}: {}", DatabaseConfig.getUrl(), e.getMessage());
            throw new DatabaseException("Could not connect to database: " + e.getMessage(), e);
        }
    }

    /**
     * Tests connectivity to the database.
     *
     * @return true if connected successfully
     */
    public static boolean checkConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            AppLogger.warn("Database connectivity check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Executes a batch SQL script from an input stream using standard delimiter parsing.
     *
     * @param scriptStream SQL script stream
     * @throws DatabaseException on failure
     */
    public static void executeSqlScript(InputStream scriptStream) {
        if (scriptStream == null) {
            throw new IllegalArgumentException("SQL script input stream cannot be null");
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             BufferedReader reader = new BufferedReader(new InputStreamReader(scriptStream))) {

            StringBuilder sb = new StringBuilder();
            String line;
            String delimiter = ";";

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // Skip comments and empty lines
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("//")) {
                    continue;
                }

                if (trimmed.toUpperCase().startsWith("DELIMITER")) {
                    String[] parts = trimmed.split("\\s+");
                    if (parts.length > 1) {
                        delimiter = parts[1];
                    }
                    continue;
                }

                sb.append(line).append("\n");

                if (trimmed.endsWith(delimiter)) {
                    String command = sb.toString();
                    command = command.substring(0, command.length() - (delimiter.length() + 1)).trim();
                    if (!command.isEmpty()) {
                        try {
                            stmt.execute(command);
                        } catch (SQLException e) {
                            // Log and continue or bubble if critical
                            AppLogger.warn("SQL script statement warning/error: {} (SQL: {})",
                                    e.getMessage(), command.substring(0, Math.min(60, command.length())));
                        }
                    }
                    sb.setLength(0);
                }
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to execute SQL script: " + e.getMessage(), e);
        }
    }
}
