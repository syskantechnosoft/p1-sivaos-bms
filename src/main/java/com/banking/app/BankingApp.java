package com.banking.app;

import com.banking.config.DBConnection;
import com.banking.dao.*;
import com.banking.dao.impl.*;
import com.banking.service.*;
import com.banking.service.impl.*;
import com.banking.util.AppLogger;
import com.banking.util.InputReader;

/**
 * Main application entry point for the Core Java 25 &amp; JDBC Secure Banking CLI Application.
 */
public class BankingApp {

    public static void main(String[] args) {
        AppLogger.info("Starting Banking Management System (BMS) CLI...");

        // Display Header Banner
        ConsoleUI.printBanner();

        // Check Database Connectivity
        if (!DBConnection.checkConnection()) {
            ConsoleUI.printError("Cannot establish connection to MySQL database.");
            ConsoleUI.printWarning("Please ensure MySQL server is running and database configuration in application.properties is correct.");
            ConsoleUI.printInfo("Run sql/schema.sql and sql/sample-data.sql in MySQL before launching.");
            System.exit(1);
        } else {
            ConsoleUI.printSuccess("Database connection established successfully.");
        }

        // Initialize DAO Layer
        CustomerDAO customerDAO = new CustomerDAOImpl();
        AccountDAO accountDAO = new AccountDAOImpl();
        TransactionDAO transactionDAO = new TransactionDAOImpl();
        AuditDAO auditDAO = new AuditDAOImpl();
        DailyBalanceDAO dailyBalanceDAO = new DailyBalanceDAOImpl();

        // Initialize Service Layer
        AuthService authService = new AuthServiceImpl(customerDAO, auditDAO);
        CustomerService customerService = new CustomerServiceImpl(customerDAO);
        AccountService accountService = new AccountServiceImpl(accountDAO);
        TransactionService transactionService = new TransactionServiceImpl(accountDAO, transactionDAO);
        InterestService interestService = new InterestServiceImpl(accountDAO, transactionDAO, dailyBalanceDAO);
        AuditService auditService = new AuditServiceImpl(auditDAO);

        InputReader inputReader = new InputReader();

        // Initialize and Run Menu Handler
        MenuHandler menuHandler = new MenuHandler(
                authService,
                customerService,
                accountService,
                transactionService,
                interestService,
                auditService,
                inputReader
        );

        menuHandler.run();

        AppLogger.info("Banking CLI Application terminated cleanly.");
    }
}
