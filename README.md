# Secure Banking Management System (BMS) CLI

![Java Version](https://img.shields.io/badge/Java-25%20LTS-orange.svg)
![Build Tool](https://img.shields.io/badge/Maven-3.10+-blue.svg)
![Database](https://img.shields.io/badge/MySQL-8.0+-00758F.svg)
![Testing](https://img.shields.io/badge/JUnit-5%20%26%20Mockito-green.svg)
![Security](https://img.shields.io/badge/BCrypt-12%20Rounds-red.svg)
![Logging](https://img.shields.io/badge/SLF4J-Logback-yellowgreen.svg)

A production-grade, secure, menu-driven CLI Banking Management System built entirely with **Core Java 25**, **JDBC**, and **MySQL 8.0**, following strict Object-Oriented Principles, Domain-Driven Design (DDD), Layered Architecture, and financial security controls.

---

## Table of Contents
1. [Core Features](#core-features)
2. [Security Architecture](#security-architecture)
3. [Domain Model & Database Design](#domain-model--database-design)
4. [Package Structure](#package-structure)
5. [Prerequisites](#prerequisites)
6. [Database Setup & Configuration](#database-setup--configuration)
7. [Build, Test & Run with Maven](#build-test--run-with-maven)
8. [Quarterly Interest Calculation](#quarterly-interest-calculation)
9. [Logging & Audit Trails](#logging--audit-trails)
10. [Demo Credentials](#demo-credentials)
11. [Git Setup & Push to GitHub](#git-setup--push-to-github)

---

## Core Features

- **Customer Profile Onboarding:** Multi-field validation (email RFC 5322, phone format, age >= 18, strong passwords).
- **Multi-Account Support:** `SAVINGS`, `CURRENT`, `LOAN`, and `DEPOSIT` (Fixed Deposit) accounts.
- **ACID Financial Transactions:** Deposits, withdrawals, and fund transfers using explicit JDBC transaction boundaries (`setAutoCommit(false)`, `commit()`, and `rollback()`).
- **Deadlock-Free Fund Transfers:** Locks accounts in consistent numeric order (`min(src, dest)` followed by `max(src, dest)`) using `SELECT ... FOR UPDATE`.
- **Minimum Balance Enforcement:** Prevents savings account withdrawals/transfers that violate the minimum balance policy (e.g. ₹1,000.00).
- **Quarterly Interest Engine:** Computes Monthly Average Balance (MAB) across 3-month calendar windows and automatically credits interest atomically to savings accounts.
- **Full Audit Ledger:** Dual-level logging via database audit tables (`login_audit`, `transaction_audit`) and rolling file appenders (`transactions.log`, `audit.log`, `account.log`).
- **Interactive ANSI Console UI:** Clean styled menus, tabular output, and active session countdown timers.

---

## Security Architecture

### 1. Inactivity Session Timeout (2 Minutes)
- When a customer or administrator authenticates, a `UserSession` is generated.
- The session tracks `lastAccessTimeMillis` and `timeoutMillis` (default: 120,000 ms / 2 minutes).
- Every console interaction, menu transition, and database request executes `SessionManager.validateAndTouch()`.
- If 2 minutes elapse without user input, `SessionExpiredException` is thrown, the session is invalidated, an audit entry is recorded in `logs/audit.log` and `login_audit`, and the user is redirected to the main login menu.

### 2. Automated Account Lockout (3 Failed Attempts)
- All failed authentication attempts are persisted to `login_audit` with `login_status = 'FAILED'`.
- The system queries consecutive failures since the last successful login:
  ```sql
  SELECT COUNT(*) FROM login_audit 
  WHERE LOWER(username) = LOWER(?) 
    AND login_status = 'FAILED' 
    AND login_time > COALESCE(
        (SELECT MAX(login_time) FROM login_audit WHERE LOWER(username) = LOWER(?) AND login_status IN ('SUCCESS', 'LOCKED')),
        '1970-01-01 00:00:00'
    );
  ```
- Upon reaching **3 consecutive failed attempts**, `customers.status` is updated to `'BLOCKED'`, `login_status = 'LOCKED'` is recorded, and `AccountLockedException` is thrown.
- Subsequent login attempts are blocked until an administrator unlocks the customer profile via the Bank Staff portal.

### 3. Password Hashing (BCrypt 12 Rounds)
- Powered by `org.mindrot.jbcrypt.BCrypt`.
- Work factor / salt cost: `12` rounds (`BCrypt.gensalt(12)`).
- Plaintext passwords are never stored in the database and never printed in logs.
- Strict password complexity enforced: Minimum 8 characters, at least one uppercase letter, one lowercase letter, one digit, and one special symbol (`@$!%*?&#^_-`).

---

## Domain Model & Database Design

The relational database (`banking_db`) contains 6 core tables:

```
+--------------------+        1:N       +---------------------+
|     CUSTOMERS      |----------------->|      ACCOUNTS       |
|--------------------|                  |---------------------|
| customer_id (PK)   |                  | account_id (PK)     |
| customer_code (UNI)|                  | customer_id (FK)    |
| email, phone (UNI) |                  | account_number (UNI)|
| password_hash      |                  | account_type        |
| status (ACTIVE/    |                  | balance, interest   |
|         BLOCKED)   |                  | status              |
+--------------------+                  +---------------------+
          |                                        |
          | 1:N                                    | 1:N
          v                                        v
+--------------------+                  +---------------------+
|    LOGIN_AUDIT     |                  |    TRANSACTIONS     |
|--------------------|                  |---------------------|
| login_audit_id(PK) |                  | transaction_id (PK) |
| customer_id (FK)   |                  | transaction_ref(UNI)|
| login_status       |                  | account_id (FK)     |
| failure_reason     |                  | dest_account_id(FK) |
| login_time         |                  | type, amount, status|
+--------------------+                  +---------------------+
                                                   |
                                                   | 1:N
                                                   v
+-------------------------+             +---------------------+
|  ACCOUNT_DAILY_BALANCE  |             |  TRANSACTION_AUDIT  |
|-------------------------|             |---------------------|
| snapshot_id (PK)        |             | audit_id (PK)       |
| account_id (FK)         |             | transaction_id (FK) |
| balance_date, balance   |             | operation, old/new  |
+-------------------------+             | changed_by, time    |
                                        +---------------------+
```

---

## Package Structure

```
d:/Training/Revature/Projects/P1-BMS/
├── pom.xml                                   # Maven configuration (Java 25, Shade, Surefire)
├── README.md                                 # Complete documentation & run guides
├── .gitignore                                # Git ignore file
├── sql/
│   ├── schema.sql                            # DDL schema, tables, triggers, functions, procedures
│   └── sample-data.sql                       # Seed customers, accounts, audits, Q1 daily balances
├── src/
│   ├── main/
│   │   ├── java/com/banking/
│   │   │   ├── app/                          # CLI router, interactive console, styling
│   │   │   │   ├── BankingApp.java           # Application entry point (main)
│   │   │   │   ├── ConsoleUI.java            # ANSI banner, colors, session countdown
│   │   │   │   └── MenuHandler.java          # Customer & Admin menu workflows
│   │   │   ├── config/                       # Properties & DB Connection factory
│   │   │   │   ├── DatabaseConfig.java       # Configuration loader
│   │   │   │   └── DBConnection.java         # JDBC DriverManager factory
│   │   │   ├── dao/                          # Persistence layer contracts
│   │   │   │   ├── AccountDAO.java
│   │   │   │   ├── AuditDAO.java
│   │   │   │   ├── CustomerDAO.java
│   │   │   │   ├── DailyBalanceDAO.java
│   │   │   │   ├── TransactionDAO.java
│   │   │   │   └── impl/                     # JDBC PreparedStatements & row-locking
│   │   │   │       ├── AccountDAOImpl.java
│   │   │   │       ├── AuditDAOImpl.java
│   │   │   │       ├── CustomerDAOImpl.java
│   │   │   │       ├── DailyBalanceDAOImpl.java
│   │   │   │       └── TransactionDAOImpl.java
│   │   │   ├── enums/                        # Domain enumerations
│   │   │   │   ├── AccountStatus.java        # ACTIVE, INACTIVE, CLOSED, FROZEN
│   │   │   │   ├── AccountType.java          # SAVINGS, CURRENT, LOAN, DEPOSIT
│   │   │   │   ├── AuditOperation.java       # INSERT, UPDATE, DELETE
│   │   │   │   ├── CustomerStatus.java       # ACTIVE, INACTIVE, BLOCKED
│   │   │   │   ├── LoginStatus.java          # SUCCESS, FAILED, LOCKED, LOGOUT
│   │   │   │   ├── TransactionStatus.java    # SUCCESS, FAILED, PENDING
│   │   │   │   └── TransactionType.java      # DEPOSIT, WITHDRAW, FUND_TRANSFER, etc.
│   │   │   ├── exceptions/                   # Custom business exceptions hierarchy
│   │   │   │   ├── AccountLockedException.java
│   │   │   │   ├── AccountNotFoundException.java
│   │   │   │   ├── AuthenticationException.java
│   │   │   │   ├── BankingException.java     # Base unchecked exception
│   │   │   │   ├── CustomerNotFoundException.java
│   │   │   │   ├── DatabaseException.java
│   │   │   │   ├── InsufficientFundsException.java
│   │   │   │   ├── InvalidTransactionException.java
│   │   │   │   ├── SessionExpiredException.java
│   │   │   │   └── ValidationException.java
│   │   │   ├── model/                        # Domain entities
│   │   │   │   ├── Account.java
│   │   │   │   ├── AccountDailyBalance.java
│   │   │   │   ├── Customer.java
│   │   │   │   ├── LoginAudit.java
│   │   │   │   ├── Transaction.java
│   │   │   │   ├── TransactionAudit.java
│   │   │   │   └── UserSession.java          # Inactivity timeout session holder
│   │   │   ├── security/                     # Security managers
│   │   │   │   ├── AccountLockoutManager.java # Consecutive failure counter & lock
│   │   │   │   └── SessionManager.java       # 2-minute timeout validator
│   │   │   ├── service/                      # Business services
│   │   │   │   ├── AccountService.java
│   │   │   │   ├── AuditService.java
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── CustomerService.java
│   │   │   │   ├── InterestService.java
│   │   │   │   ├── TransactionService.java
│   │   │   │   └── impl/                     # ACID transaction management
│   │   │   │       ├── AccountServiceImpl.java
│   │   │   │       ├── AuditServiceImpl.java
│   │   │   │       ├── AuthServiceImpl.java
│   │   │   │       ├── CustomerServiceImpl.java
│   │   │   │       ├── InterestServiceImpl.java
│   │   │   │       └── TransactionServiceImpl.java
│   │   │   ├── util/                         # Common utilities
│   │   │   │   ├── AppLogger.java            # Multi-file logger dispatcher
│   │   │   │   ├── DateUtil.java             # Date parsing & formatting
│   │   │   │   ├── InputReader.java          # Safe scanner & session check
│   │   │   │   ├── PasswordUtil.java         # BCrypt hashing & complexity regex
│   │   │   │   ├── TablePrinter.java         # Aligned console table formatter
│   │   │   │   └── TransactionRefUtil.java   # Unique transaction ref generator
│   │   │   └── validation/                   # Custom validation logic
│   │   │       ├── AccountValidator.java
│   │   │       ├── CustomerValidator.java
│   │   │       ├── TransactionValidator.java
│   │   │       ├── ValidationResult.java
│   │   │       └── ValidationUtil.java
│   │   └── resources/
│   │       ├── application.properties        # DB, security, admin configs
│   │       ├── logback.xml                   # Rolling file appenders configuration
│   │       └── sql/                          # Bundled schema & seed data
│   └── test/
│       ├── java/com/banking/
│       │   ├── security/                     # BCrypt, Lockout, and Timeout tests
│       │   │   ├── AccountLockoutTest.java
│       │   │   ├── PasswordSecurityTest.java
│       │   │   └── SessionManagerTest.java
│       │   ├── service/                      # Mockito business unit tests
│       │   │   ├── AccountServiceTest.java
│       │   │   ├── AuthServiceTest.java
│       │   │   ├── InterestServiceTest.java
│       │   │   └── TransactionServiceTest.java
│       │   └── validation/
│       │       └── ValidationTest.java       # Input & domain validation tests
│       └── resources/
│           └── mockito-extensions/           # Java 25 Mockito subclass plugin
```

---

## Prerequisites

- **Java Development Kit:** Java 25 LTS (OpenJDK / Eclipse Adoptium)
- **Apache Maven:** 3.9+ or 3.10+
- **Database Server:** MySQL 8.0+ Community Server running locally on port 3306

Verify your environment:
```powershell
java -version
mvn -version
mysql --version
```

---

## Database Setup & Configuration

### 1. Initialize Schema & Sample Data
Execute the SQL scripts using the MySQL command-line client or MySQL Workbench:
```powershell
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/sample-data.sql
```

### 2. Configure Database Connection
Review and update `src/main/resources/application.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/banking_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.user=root
db.password=root
db.driver=com.mysql.cj.jdbc.Driver

# Security Configurations
security.session.timeout.minutes=2
security.max.failed.attempts=3

# Admin Portal
admin.username=admin@bank.com
admin.password=Admin@123
```

*(Note: Environment variables `DB_URL`, `DB_USER`, and `DB_PASSWORD` can also be used to override credentials at runtime).*

---

## Build, Test & Run with Maven

### 1. Compile the Project
```powershell
mvn clean compile
```

### 2. Execute Unit Tests (JUnit 5 + Mockito)
Runs all 27 unit tests validating BCrypt password hashing, session expiration after 2 minutes, account lockout after 3 failures, ACID transaction integrity, and quarterly interest calculations:
```powershell
mvn test
```

### 3. Package Executable Fat JAR
Produces an Uber JAR containing all dependencies (`target/banking-cli-1.0.0.jar`):
```powershell
mvn package
```

### 4. Run the CLI Application

**Option A: Using Maven Exec Plugin**
```powershell
mvn exec:java
```

**Option B: Using the Standalone Executable Fat JAR**
```powershell
java -jar target/banking-cli-1.0.0.jar
```

---

## Quarterly Interest Calculation

In accordance with banking guidelines, interest on savings accounts is calculated on the **Monthly Average Balance (MAB)** across all calendar days in the month:

$$\text{MAB} = \frac{\sum \text{Daily Closing Balances}}{\text{Total Days in Month}}$$

$$\text{Monthly Interest} = \text{MAB} \times \frac{\text{Annual Rate}}{100 \times 12}$$

$$\text{Quarterly Interest} = \sum_{m=1}^{3} \text{Monthly Interest}_m$$

### Example (Q1: January – March 2026):
- **Account:** `SB100001` (Arun Kumar)
- **Annual Savings Rate:** `4.00%`
- **MAB for January:** ₹100,000 $\rightarrow$ Interest: ₹333.33
- **MAB for February:** ₹100,000 $\rightarrow$ Interest: ₹333.33
- **MAB for March:** ₹100,000 $\rightarrow$ Interest: ₹333.33
- **Total Quarterly Interest:** **₹1,000.00**

### Execution via Administrator Portal:
1. Log in as Administrator (`admin@bank.com` / `Admin@123`).
2. Select Option `4` (*Calculate & Credit Quarterly Interest*).
3. Input Year `2026` and Quarter `1`.
4. The system calculates MAB, credits the amount atomically, and generates reference `INT-1-2026-Q1`. Repeated execution for the same quarter is automatically prevented.

---

## Logging & Audit Trails

The application generates structured, rolling log files inside the `logs/` directory:

| Log File | Logger Name | Description |
|---|---|---|
| `logs/transactions.log` | `com.banking.log.transaction` | Financial transactions: Deposits, withdrawals, fund transfers, and interest credits. |
| `logs/audit.log` | `com.banking.log.audit` | Security events: Login attempts, failed logins, account locks, session timeouts, and logouts. |
| `logs/account.log` | `com.banking.log.account` | Account lifecycle events: Account creations, freezes, unfreezes, and quarterly interest applications. |
| `logs/application.log` | `Root Logger` | General application events, SQL warnings, and system lifecycle logs. |

### Database Audit Tables:
- `login_audit`: Stores username, outcome (`SUCCESS`, `FAILED`, `LOCKED`, `LOGOUT`), client IP, device, and timestamp.
- `transaction_audit`: Populated automatically via database triggers (`trg_transaction_after_insert`, `trg_transaction_after_update`, `trg_transaction_after_delete`).

---

## Demo Credentials

### Customer Accounts (Password: `Pass@123`)
| Customer Code | Name | Email | Default Accounts |
|---|---|---|---|
| **`CUST001`** | Arun Kumar | `arun@example.com` | `SB100001` (Savings: ₹100,000), `CA100001` (Current: ₹50,000) |
| **`CUST002`** | Priya Sharma | `priya@example.com` | `LN100001` (Loan: ₹250,000), `FD100001` (Deposit: ₹200,000) |
| **`CUST003`** | Rahul Raj | `rahul@example.com` | `SB100002` (Savings: ₹75,000) |

### Administrator Portal
- **Username:** `admin@bank.com`
- **Password:** `Admin@123`

---

## Git Setup & Push to GitHub

To initialize Git locally, commit the project, and push to your GitHub repository:

```powershell
# 1. Initialize local Git repository
git init

# 2. Stage all project files (ignoring files defined in .gitignore)
git add .

# 3. Create initial commit
git commit -m "feat: complete Java 25 CLI Banking Management System with security, ACID transactions, and quarterly interest"

# 4. Set default branch to main
git branch -M main

# 5. Link to your remote GitHub repository (replace with your repository URL)
git remote add origin https://github.com/<your-username>/<your-repo-name>.git

# 6. Push code to GitHub
git push -u origin main
```
