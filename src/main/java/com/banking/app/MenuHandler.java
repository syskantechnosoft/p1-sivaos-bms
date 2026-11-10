package com.banking.app;

import com.banking.config.DBConnection;
import com.banking.config.DatabaseConfig;
import com.banking.enums.AccountType;
import com.banking.enums.CustomerStatus;
import com.banking.exceptions.BankingException;
import com.banking.exceptions.SessionExpiredException;
import com.banking.model.*;
import com.banking.security.SessionManager;
import com.banking.service.*;
import com.banking.service.impl.*;
import com.banking.util.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

/**
 * CLI navigation and interactive workflow coordinator.
 */
public class MenuHandler {

    private final AuthService authService;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final InterestService interestService;
    private final AuditService auditService;
    private final InputReader inputReader;

    public MenuHandler(AuthService authService,
                       CustomerService customerService,
                       AccountService accountService,
                       TransactionService transactionService,
                       InterestService interestService,
                       AuditService auditService,
                       InputReader inputReader) {
        this.authService = authService;
        this.customerService = customerService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.interestService = interestService;
        this.auditService = auditService;
        this.inputReader = inputReader;
    }

    public void run() {
        boolean running = true;
        while (running) {
            try {
                ConsoleUI.printHeader("MAIN MENU");
                System.out.println(" 1. Customer Login");
                System.out.println(" 2. Open / Register New Customer Profile");
                System.out.println(" 3. Bank Administration & Staff Portal");
                System.out.println(" 4. Database & Security Status");
                System.out.println(" 5. Exit Application");
                ConsoleUI.printDivider();

                int choice = inputReader.readInt("Enter your choice (1-5)", 1, 5);
                switch (choice) {
                    case 1 -> handleCustomerLogin();
                    case 2 -> handleCustomerRegistration();
                    case 3 -> handleAdminLogin();
                    case 4 -> showDatabaseStatus();
                    case 5 -> {
                        ConsoleUI.printInfo("Thank you for using " + DatabaseConfig.getBankName() + ". Goodbye!");
                        running = false;
                    }
                }
            } catch (SessionExpiredException e) {
                ConsoleUI.printWarning(e.getMessage());
            } catch (Exception e) {
                ConsoleUI.printError("Unexpected error: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------
    // Authentication Handlers
    // -------------------------------------------------------------

    private void handleCustomerLogin() {
        ConsoleUI.printHeader("CUSTOMER AUTHENTICATION");
        System.out.println("Note: Account will be locked after 3 consecutive failed login attempts.");
        System.out.println("Note: Authenticated sessions automatically time out after 2 minutes of inactivity.");
        ConsoleUI.printDivider();

        String identifier = inputReader.readRequiredString("Enter Email or Customer Code (e.g. CUST001 or arun@example.com)");
        String password = inputReader.readPassword("Enter Password");

        try {
            Customer customer = authService.loginCustomer(identifier, password, "127.0.0.1", "CLI Terminal");
            ConsoleUI.printSuccess("Welcome back, " + customer.getFullName() + "! (Customer Code: " + customer.getCustomerCode() + ")");
            handleCustomerSession(customer);
        } catch (BankingException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private void handleCustomerRegistration() {
        ConsoleUI.printHeader("NEW CUSTOMER ONBOARDING");
        System.out.println("Please provide your profile details below (or enter 'cancel' to return):");
        ConsoleUI.printDivider();

        try {
            String firstName = inputReader.readRequiredString("First Name");
            String lastName = inputReader.readRequiredString("Last Name");
            String email = inputReader.readRequiredString("Email Address");
            String phone = inputReader.readRequiredString("Phone Number (10 digits)");
            LocalDate dob = inputReader.readDate("Date of Birth", true);
            String address = inputReader.readRequiredString("Residential Address");

            System.out.println();
            ConsoleUI.printInfo("Password Requirements: " + PasswordUtil.getPasswordRequirementsDescription());
            String password = inputReader.readPassword("Create Master Password");

            Customer newCustomer = new Customer();
            newCustomer.setFirstName(firstName);
            newCustomer.setLastName(lastName);
            newCustomer.setEmail(email);
            newCustomer.setPhone(phone);
            newCustomer.setDateOfBirth(dob);
            newCustomer.setAddress(address);

            Customer created = customerService.registerCustomer(newCustomer, password);
            ConsoleUI.printSuccess("Customer account registered successfully!");
            ConsoleUI.printInfo("Your assigned Customer Code is: " + created.getCustomerCode());

            // Offer to open an initial account
            System.out.println();
            String openNow = inputReader.readString("Would you like to open a Savings account now? (Y/N)");
            if (openNow.equalsIgnoreCase("Y") || openNow.equalsIgnoreCase("YES")) {
                BigDecimal deposit = inputReader.readBigDecimal("Initial Deposit Amount (Min ₹1,000 for Savings)");
                Account acc = accountService.openAccount(created.getCustomerId(), AccountType.SAVINGS, deposit, null, null, null);
                ConsoleUI.printSuccess("Savings Account opened! Account Number: " + acc.getAccountNumber() + ", Balance: ₹" + acc.getBalance());
            }
        } catch (BankingException e) {
            ConsoleUI.printError("Registration failed: " + e.getMessage());
        } catch (RuntimeException e) {
            if ("Operation cancelled by user.".equals(e.getMessage())) {
                ConsoleUI.printInfo("Registration cancelled.");
            } else {
                ConsoleUI.printError("Error: " + e.getMessage());
            }
        }
    }

    private void handleAdminLogin() {
        ConsoleUI.printHeader("BANK STAFF / ADMINISTRATOR LOGIN");
        ConsoleUI.printDivider();

        String username = inputReader.readRequiredString("Admin Username (e.g. admin@bank.com)");
        String password = inputReader.readPassword("Admin Password (e.g. Admin@123)");

        try {
            authService.loginAdmin(username, password, "127.0.0.1", "Admin Console");
            ConsoleUI.printSuccess("Administrator authenticated successfully.");
            handleAdminSession();
        } catch (BankingException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Customer Portal (Session-Protected)
    // -------------------------------------------------------------

    private void handleCustomerSession(Customer customer) {
        boolean active = true;
        while (active) {
            try {
                // Verify 2-minute inactivity timeout
                SessionManager.validateAndTouch();

                ConsoleUI.printHeader("CUSTOMER PORTAL - " + customer.getFullName());
                System.out.println(" 1. View My Profile & Account Summary");
                System.out.println(" 2. Check Account Balance");
                System.out.println(" 3. Deposit Money");
                System.out.println(" 4. Withdraw Cash");
                System.out.println(" 5. Fund Transfer (Atomic)");
                System.out.println(" 6. View Transaction Statement / History");
                System.out.println(" 7. Open Additional Account (Savings, Current, Loan, Deposit)");
                System.out.println(" 8. Change Password");
                System.out.println(" 9. Logout");
                ConsoleUI.printDivider();

                int choice = inputReader.readInt("Select operation (1-9)", 1, 9);
                switch (choice) {
                    case 1 -> showCustomerProfile(customer);
                    case 2 -> showAccountBalance(customer);
                    case 3 -> handleDeposit(customer);
                    case 4 -> handleWithdraw(customer);
                    case 5 -> handleFundTransfer(customer);
                    case 6 -> handleTransactionStatement(customer);
                    case 7 -> handleOpenAdditionalAccount(customer);
                    case 8 -> handleChangePassword(customer);
                    case 9 -> {
                        authService.logout();
                        ConsoleUI.printSuccess("Logged out successfully.");
                        active = false;
                    }
                }
            } catch (SessionExpiredException e) {
                ConsoleUI.printWarning("SESSION TIMED OUT: " + e.getMessage());
                active = false;
            } catch (Exception e) {
                ConsoleUI.printError("Operation error: " + e.getMessage());
            }
        }
    }

    private void showCustomerProfile(Customer customer) {
        ConsoleUI.printHeader("MY PROFILE & ACCOUNTS");
        System.out.println(" Customer Code : " + customer.getCustomerCode());
        System.out.println(" Full Name     : " + customer.getFullName());
        System.out.println(" Email Address : " + customer.getEmail());
        System.out.println(" Phone Number  : " + customer.getPhone());
        System.out.println(" Date of Birth : " + DateUtil.formatDate(customer.getDateOfBirth()));
        System.out.println(" Address       : " + customer.getAddress());
        System.out.println(" Status        : " + customer.getStatus());
        System.out.println();

        List<Account> accounts = accountService.getAccountsByCustomerId(customer.getCustomerId());
        TablePrinter printer = new TablePrinter("Account ID", "Account Number", "Type", "Balance (₹)", "Interest %", "Min Balance (₹)", "Status");
        for (Account a : accounts) {
            printer.addRow(
                    String.valueOf(a.getAccountId()),
                    a.getAccountNumber(),
                    a.getAccountType().name(),
                    String.format("%.2f", a.getBalance()),
                    String.format("%.2f", a.getInterestRate()),
                    String.format("%.2f", a.getMinimumBalance()),
                    a.getStatus().name()
            );
        }
        printer.print();
    }

    private void showAccountBalance(Customer customer) {
        Account account = selectCustomerAccount(customer, "Select account to check balance");
        if (account != null) {
            BigDecimal current = accountService.getAccountBalance(account.getAccountId());
            ConsoleUI.printInfo("Current balance for " + account.getAccountNumber() + " (" + account.getAccountType() + "): ₹" + current);
        }
    }

    private void handleDeposit(Customer customer) {
        Account account = selectCustomerAccount(customer, "Select account to deposit into");
        if (account == null) return;

        BigDecimal amount = inputReader.readBigDecimal("Enter deposit amount (₹)");
        String desc = inputReader.readString("Enter description (or press Enter for default)");

        Transaction txn = transactionService.deposit(account.getAccountId(), amount, desc);
        ConsoleUI.printSuccess("Deposit successful! Ref: " + txn.getTransactionRef() +
                ", New Balance: ₹" + txn.getBalanceAfter());
    }

    private void handleWithdraw(Customer customer) {
        Account account = selectCustomerAccount(customer, "Select account to withdraw from");
        if (account == null) return;

        BigDecimal amount = inputReader.readBigDecimal("Enter withdrawal amount (₹)");
        String desc = inputReader.readString("Enter description (or press Enter for default)");

        Transaction txn = transactionService.withdraw(account.getAccountId(), amount, desc);
        ConsoleUI.printSuccess("Withdrawal successful! Ref: " + txn.getTransactionRef() +
                ", New Balance: ₹" + txn.getBalanceAfter());
    }

    private void handleFundTransfer(Customer customer) {
        ConsoleUI.printHeader("ATOMIC FUND TRANSFER");
        Account source = selectCustomerAccount(customer, "Select source account to transfer FROM");
        if (source == null) return;

        String destAccNum = inputReader.readRequiredString("Enter Destination Account Number (e.g. SB100002 or CA100001)");
        Account dest = accountService.getAccountByNumber(destAccNum);

        if (dest.getAccountId().equals(source.getAccountId())) {
            ConsoleUI.printError("Source and destination accounts cannot be identical.");
            return;
        }

        BigDecimal amount = inputReader.readBigDecimal("Enter Transfer Amount (₹)");
        String desc = inputReader.readString("Enter transfer remarks");

        Transaction txn = transactionService.transfer(source.getAccountId(), dest.getAccountId(), amount, desc);
        ConsoleUI.printSuccess("Fund transfer completed atomically! Ref: " + txn.getTransactionRef());
        ConsoleUI.printInfo("Transferred ₹" + amount + " to " + dest.getAccountNumber() + ". Updated balance: ₹" + txn.getBalanceAfter());
    }

    private void handleTransactionStatement(Customer customer) {
        Account account = selectCustomerAccount(customer, "Select account to view statement");
        if (account == null) return;

        List<Transaction> txns = transactionService.getAccountStatement(account.getAccountId(), 20, 0);
        ConsoleUI.printHeader("TRANSACTION STATEMENT: " + account.getAccountNumber());
        TablePrinter printer = new TablePrinter("Ref ID", "Type", "Amount (₹)", "Balance Before", "Balance After", "Date & Time", "Description", "Status");
        for (Transaction t : txns) {
            printer.addRow(
                    t.getTransactionRef(),
                    t.getTransactionType().name(),
                    String.format("%.2f", t.getAmount()),
                    String.format("%.2f", t.getBalanceBefore()),
                    String.format("%.2f", t.getBalanceAfter()),
                    DateUtil.formatDateTime(t.getTransactionDate()),
                    t.getDescription() != null ? t.getDescription() : "",
                    t.getStatus().name()
            );
        }
        printer.print();
    }

    private void handleOpenAdditionalAccount(Customer customer) {
        ConsoleUI.printHeader("OPEN ADDITIONAL ACCOUNT");
        System.out.println("Available Account Types:");
        System.out.println(" 1. SAVINGS (Annual 4.00% interest, eligible for quarterly credit, min bal ₹1,000)");
        System.out.println(" 2. CURRENT (Business account, 0% interest, min bal ₹0)");
        System.out.println(" 3. LOAN (9.00% interest rate)");
        System.out.println(" 4. DEPOSIT (Fixed deposit, 7.00% interest rate, requires maturity date)");
        ConsoleUI.printDivider();

        int typeChoice = inputReader.readInt("Select account type (1-4)", 1, 4);
        AccountType type = switch (typeChoice) {
            case 1 -> AccountType.SAVINGS;
            case 2 -> AccountType.CURRENT;
            case 3 -> AccountType.LOAN;
            case 4 -> AccountType.DEPOSIT;
            default -> AccountType.SAVINGS;
        };

        BigDecimal initialDeposit = inputReader.readBigDecimal("Initial opening deposit amount (₹)");
        LocalDate maturity = null;
        if (type == AccountType.DEPOSIT) {
            maturity = inputReader.readDate("Maturity Date (YYYY-MM-DD)", false);
        }

        Account acc = accountService.openAccount(customer.getCustomerId(), type, initialDeposit, null, null, maturity);
        ConsoleUI.printSuccess("New " + type + " account opened! Account Number: " + acc.getAccountNumber());
    }

    private void handleChangePassword(Customer customer) {
        ConsoleUI.printHeader("CHANGE PASSWORD");
        String oldPass = inputReader.readPassword("Enter Current Password");
        ConsoleUI.printInfo("New Password Requirements: " + PasswordUtil.getPasswordRequirementsDescription());
        String newPass = inputReader.readPassword("Enter New Password");
        String confirm = inputReader.readPassword("Confirm New Password");

        if (!newPass.equals(confirm)) {
            ConsoleUI.printError("New passwords do not match.");
            return;
        }

        boolean ok = customerService.changePassword(customer.getCustomerId(), oldPass, newPass);
        if (ok) {
            ConsoleUI.printSuccess("Password updated successfully.");
        }
    }

    private Account selectCustomerAccount(Customer customer, String prompt) {
        List<Account> accounts = accountService.getAccountsByCustomerId(customer.getCustomerId());
        if (accounts.isEmpty()) {
            ConsoleUI.printWarning("You do not have any open accounts.");
            return null;
        }
        if (accounts.size() == 1) {
            return accounts.get(0);
        }

        System.out.println(prompt + ":");
        for (int i = 0; i < accounts.size(); i++) {
            Account a = accounts.get(i);
            System.out.printf(" %d. %s (%s) - Balance: ₹%.2f [%s]\n",
                    (i + 1), a.getAccountNumber(), a.getAccountType(), a.getBalance(), a.getStatus());
        }
        int sel = inputReader.readInt("Enter number (1-" + accounts.size() + ")", 1, accounts.size());
        return accounts.get(sel - 1);
    }

    // -------------------------------------------------------------
    // Administration & Management Console
    // -------------------------------------------------------------

    private void handleAdminSession() {
        boolean active = true;
        while (active) {
            try {
                SessionManager.validateAndTouch();

                ConsoleUI.printHeader("ADMINISTRATION & MANAGEMENT CONSOLE");
                System.out.println(" 1. View All Customers & Accounts");
                System.out.println(" 2. Search Customer / Account");
                System.out.println(" 3. Lock / Unlock Customer Profile");
                System.out.println(" 4. Calculate & Credit Quarterly Interest (Savings Accounts)");
                System.out.println(" 5. Trigger Daily Balance Snapshot EOD Batch");
                System.out.println(" 6. View Login Security Audit Trail (DB Table: login_audit)");
                System.out.println(" 7. View Transaction Change Audit Trail (DB Table: transaction_audit)");
                System.out.println(" 8. View Generated Log Files (transactions.log, audit.log, account.log)");
                System.out.println(" 9. Logout");
                ConsoleUI.printDivider();

                int choice = inputReader.readInt("Select operation (1-9)", 1, 9);
                switch (choice) {
                    case 1 -> showAllCustomersAndAccounts();
                    case 2 -> handleSearchCustomer();
                    case 3 -> handleUnlockCustomer();
                    case 4 -> handleQuarterlyInterestBatch();
                    case 5 -> handleDailySnapshotBatch();
                    case 6 -> showLoginAudits();
                    case 7 -> showTransactionAudits();
                    case 8 -> showLogFiles();
                    case 9 -> {
                        authService.logout();
                        ConsoleUI.printSuccess("Administrator logged out.");
                        active = false;
                    }
                }
            } catch (SessionExpiredException e) {
                ConsoleUI.printWarning("Admin session expired: " + e.getMessage());
                active = false;
            } catch (Exception e) {
                ConsoleUI.printError("Admin operation error: " + e.getMessage());
            }
        }
    }

    private void showAllCustomersAndAccounts() {
        ConsoleUI.printHeader("ALL REGISTERED CUSTOMERS & ACCOUNTS");
        List<Customer> customers = customerService.getAllCustomers();
        TablePrinter printer = new TablePrinter("ID", "Code", "Name", "Email", "Phone", "Status");
        for (Customer c : customers) {
            printer.addRow(
                    String.valueOf(c.getCustomerId()),
                    c.getCustomerCode(),
                    c.getFullName(),
                    c.getEmail(),
                    c.getPhone(),
                    c.getStatus().name()
            );
        }
        printer.print();

        System.out.println();
        List<Account> accounts = accountService.getAllAccounts();
        TablePrinter accPrinter = new TablePrinter("Account ID", "Customer ID", "Account No", "Type", "Balance (₹)", "Rate %", "Status");
        for (Account a : accounts) {
            accPrinter.addRow(
                    String.valueOf(a.getAccountId()),
                    String.valueOf(a.getCustomerId()),
                    a.getAccountNumber(),
                    a.getAccountType().name(),
                    String.format("%.2f", a.getBalance()),
                    String.format("%.2f", a.getInterestRate()),
                    a.getStatus().name()
            );
        }
        accPrinter.print();
    }

    private void handleSearchCustomer() {
        String query = inputReader.readRequiredString("Enter Customer Code or Email to search");
        Customer c = query.contains("@") ? customerService.getCustomerByEmail(query) : customerService.getCustomerByCode(query);
        ConsoleUI.printHeader("CUSTOMER DETAILS: " + c.getFullName());
        System.out.println(" Customer Code: " + c.getCustomerCode());
        System.out.println(" Email:         " + c.getEmail());
        System.out.println(" Phone:         " + c.getPhone());
        System.out.println(" Status:        " + c.getStatus());
        List<Account> accs = accountService.getAccountsByCustomerId(c.getCustomerId());
        System.out.println(" Total Accounts: " + accs.size());
    }

    private void handleUnlockCustomer() {
        ConsoleUI.printHeader("CUSTOMER ACCOUNT LOCK / UNLOCK");
        String code = inputReader.readRequiredString("Enter Customer Code (e.g. CUST001)");
        Customer c = customerService.getCustomerByCode(code);
        System.out.println("Current status of " + c.getFullName() + ": " + c.getStatus());
        System.out.println(" 1. Unlock Account (Set status = ACTIVE)");
        System.out.println(" 2. Lock Account (Set status = BLOCKED)");
        System.out.println(" 3. Cancel");
        int opt = inputReader.readInt("Select option (1-3)", 1, 3);
        if (opt == 1) {
            customerService.unlockCustomer(c.getCustomerId());
            ConsoleUI.printSuccess("Customer " + c.getCustomerCode() + " unlocked successfully!");
        } else if (opt == 2) {
            customerService.updateCustomerProfile(c);
            ConsoleUI.printWarning("Customer set to BLOCKED.");
        }
    }

    private void handleQuarterlyInterestBatch() {
        ConsoleUI.printHeader("QUARTERLY SAVINGS INTEREST CALCULATION & CREDIT BATCH");
        System.out.println("Business Rule: Calculates Monthly Average Balance (MAB) for the quarter");
        System.out.println("Formula: Quarterly Interest = Sum(MAB_month * rate / 1200)");
        System.out.println("Credited atomically with transaction reference INT-<accountId>-<year>-Q<quarter>");
        ConsoleUI.printDivider();

        int year = inputReader.readInt("Enter Year (e.g. 2026)", 2020, 2035);
        int quarter = inputReader.readInt("Enter Quarter (1 to 4)", 1, 4);

        System.out.println();
        ConsoleUI.printInfo("Running interest calculation for Year " + year + ", Quarter " + quarter + "...");
        List<Transaction> credited = interestService.creditQuarterlyInterestForAllSavings(year, quarter);

        if (credited.isEmpty()) {
            ConsoleUI.printWarning("No savings accounts were eligible, or interest was already credited for Q" + quarter + " " + year + ".");
        } else {
            ConsoleUI.printSuccess("Quarterly interest credited successfully to " + credited.size() + " accounts!");
            TablePrinter printer = new TablePrinter("Ref ID", "Account ID", "Amount (₹)", "Balance Before", "Balance After", "Description");
            for (Transaction t : credited) {
                printer.addRow(
                        t.getTransactionRef(),
                        String.valueOf(t.getAccountId()),
                        String.format("%.2f", t.getAmount()),
                        String.format("%.2f", t.getBalanceBefore()),
                        String.format("%.2f", t.getBalanceAfter()),
                        t.getDescription()
                );
            }
            printer.print();
        }
    }

    private void handleDailySnapshotBatch() {
        ConsoleUI.printHeader("TRIGGER DAILY CLOSING BALANCE SNAPSHOT");
        System.out.println("Executing EOD balance capture for date: " + LocalDate.now());
        interestService.captureDailyClosingBalances();
        ConsoleUI.printSuccess("Daily balances recorded successfully for all active and frozen accounts.");
    }

    private void showLoginAudits() {
        ConsoleUI.printHeader("SECURITY AUDIT TRAIL (login_audit)");
        List<LoginAudit> audits = auditService.getRecentLoginAudits(20);
        TablePrinter printer = new TablePrinter("Audit ID", "User / Email", "Status", "IP Address", "Device", "Time", "Failure / Reason");
        for (LoginAudit a : audits) {
            printer.addRow(
                    String.valueOf(a.getLoginAuditId()),
                    a.getUsername(),
                    a.getLoginStatus().name(),
                    a.getIpAddress() != null ? a.getIpAddress() : "",
                    a.getDeviceInfo() != null ? a.getDeviceInfo() : "",
                    DateUtil.formatDateTime(a.getLoginTime()),
                    a.getFailureReason() != null ? a.getFailureReason() : ""
            );
        }
        printer.print();
    }

    private void showTransactionAudits() {
        ConsoleUI.printHeader("TRANSACTION AUDIT TRAIL (transaction_audit)");
        List<TransactionAudit> audits = auditService.getRecentTransactionAudits(20);
        TablePrinter printer = new TablePrinter("Audit ID", "Txn ID", "Operation", "Old Amount", "New Amount", "Old Status", "New Status", "Changed By", "Audit Time");
        for (TransactionAudit a : audits) {
            printer.addRow(
                    String.valueOf(a.getAuditId()),
                    String.valueOf(a.getTransactionId()),
                    a.getOperation().name(),
                    a.getOldAmount() != null ? a.getOldAmount().toString() : "-",
                    a.getNewAmount() != null ? a.getNewAmount().toString() : "-",
                    a.getOldStatus() != null ? a.getOldStatus() : "-",
                    a.getNewStatus() != null ? a.getNewStatus() : "-",
                    a.getChangedBy() != null ? a.getChangedBy() : "",
                    DateUtil.formatDateTime(a.getAuditTime())
            );
        }
        printer.print();
    }

    private void showLogFiles() {
        ConsoleUI.printHeader("GENERATED SYSTEM LOG FILES");
        System.out.println(" 1. View transactions.log (Financial movements)");
        System.out.println(" 2. View audit.log (Security & Login events)");
        System.out.println(" 3. View account.log (Account lifecycle events)");
        System.out.println(" 4. View application.log (General application log)");
        System.out.println(" 5. Back");
        ConsoleUI.printDivider();

        int logChoice = inputReader.readInt("Select log file to view (1-5)", 1, 5);
        String fileName = switch (logChoice) {
            case 1 -> "logs/transactions.log";
            case 2 -> "logs/audit.log";
            case 3 -> "logs/account.log";
            case 4 -> "logs/application.log";
            default -> null;
        };

        if (fileName != null) {
            File f = new File(fileName);
            if (!f.exists()) {
                ConsoleUI.printWarning("Log file " + fileName + " has not been generated yet or is empty.");
                return;
            }
            try {
                List<String> lines = Files.readAllLines(f.toPath());
                ConsoleUI.printHeader("LOG CONTENT: " + fileName + " (Last 25 entries)");
                int start = Math.max(0, lines.size() - 25);
                for (int i = start; i < lines.size(); i++) {
                    System.out.println(lines.get(i));
                }
                ConsoleUI.printDivider();
            } catch (IOException e) {
                ConsoleUI.printError("Could not read log file: " + e.getMessage());
            }
        }
    }

    private void showDatabaseStatus() {
        ConsoleUI.printHeader("DATABASE & SECURITY STATUS");
        boolean ok = DBConnection.checkConnection();
        if (ok) {
            ConsoleUI.printSuccess("Database Connection: CONNECTED (MySQL 8.0)");
            System.out.println(" JDBC URL           : " + DatabaseConfig.getUrl());
            System.out.println(" Inactivity Timeout : " + DatabaseConfig.getSessionTimeoutMinutes() + " minutes");
            System.out.println(" Lockout Threshold  : " + DatabaseConfig.getMaxFailedAttempts() + " consecutive failed attempts");
            System.out.println(" Hashing Algorithm  : BCrypt (Salt Cost: 12 rounds)");
        } else {
            ConsoleUI.printError("Database Connection: DISCONNECTED. Please verify MySQL service is running.");
        }
    }
}
