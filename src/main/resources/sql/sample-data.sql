-- ============================================================================
-- Banking Management System (BMS) - Seed Data & Initial Transactions
-- ============================================================================

USE banking_db;

-- ----------------------------------------------------------------------------
-- 1. Insert Customers
-- Default demo password for all 3 customers: Pass@123
-- BCrypt (12 rounds) hash: $2a$12$/J/F4x8Mn1Q2K/9jQQGf7.Nt1e0yulJmd4vMddD1seV9/AzNyKL.y
-- ----------------------------------------------------------------------------
INSERT INTO customers (
    customer_code,
    first_name,
    last_name,
    email,
    phone,
    password_hash,
    date_of_birth,
    address,
    status
) VALUES 
(
    'CUST001',
    'Arun',
    'Kumar',
    'arun@example.com',
    '9876543210',
    '$2a$12$/J/F4x8Mn1Q2K/9jQQGf7.Nt1e0yulJmd4vMddD1seV9/AzNyKL.y',
    '1990-05-15',
    'Madurai, Tamil Nadu',
    'ACTIVE'
),
(
    'CUST002',
    'Priya',
    'Sharma',
    'priya@example.com',
    '9876543211',
    '$2a$12$/J/F4x8Mn1Q2K/9jQQGf7.Nt1e0yulJmd4vMddD1seV9/AzNyKL.y',
    '1992-08-20',
    'Chennai, Tamil Nadu',
    'ACTIVE'
),
(
    'CUST003',
    'Rahul',
    'Raj',
    'rahul@example.com',
    '9876543212',
    '$2a$12$/J/F4x8Mn1Q2K/9jQQGf7.Nt1e0yulJmd4vMddD1seV9/AzNyKL.y',
    '1988-11-10',
    'Coimbatore, Tamil Nadu',
    'ACTIVE'
);

-- ----------------------------------------------------------------------------
-- 2. Insert Accounts
-- ----------------------------------------------------------------------------
INSERT INTO accounts (
    customer_id,
    account_number,
    account_type,
    balance,
    interest_rate,
    minimum_balance,
    opening_date,
    maturity_date,
    status
) VALUES 
-- Arun: Savings Account
(1, 'SB100001', 'SAVINGS', 100000.00, 4.00, 1000.00, '2025-01-01', NULL, 'ACTIVE'),

-- Arun: Current Account
(1, 'CA100001', 'CURRENT', 50000.00, 0.00, 0.00, '2025-02-01', NULL, 'ACTIVE'),

-- Priya: Loan Account
(2, 'LN100001', 'LOAN', 250000.00, 9.00, 0.00, '2025-03-01', NULL, 'ACTIVE'),

-- Priya: Deposit Account
(2, 'FD100001', 'DEPOSIT', 200000.00, 7.00, 0.00, '2025-04-01', '2027-04-01', 'ACTIVE'),

-- Rahul: Savings Account
(3, 'SB100002', 'SAVINGS', 75000.00, 4.00, 1000.00, '2025-05-01', NULL, 'ACTIVE');

-- ----------------------------------------------------------------------------
-- 3. Insert Login Audit History
-- ----------------------------------------------------------------------------
INSERT INTO login_audit (
    customer_id,
    username,
    login_status,
    ip_address,
    device_info,
    failure_reason
) VALUES 
(
    1,
    'arun@example.com',
    'SUCCESS',
    '192.168.1.10',
    'Windows 11 CLI',
    NULL
),
(
    1,
    'arun@example.com',
    'FAILED',
    '192.168.1.10',
    'Windows 11 CLI',
    'Invalid password'
),
(
    2,
    'priya@example.com',
    'SUCCESS',
    '192.168.1.20',
    'macOS Terminal',
    NULL
),
(
    NULL,
    'unknown@example.com',
    'FAILED',
    '192.168.1.30',
    'Linux Bash',
    'User not found'
);

-- ----------------------------------------------------------------------------
-- 4. Generate Daily Balance Snapshots for Q1 2026 (Jan 1 to Mar 31, 2026)
-- ----------------------------------------------------------------------------
SET @start_date = '2026-01-01';
SET @end_date = '2026-03-31';

INSERT INTO account_daily_balance (
    account_id,
    balance_date,
    closing_balance
)
WITH RECURSIVE calendar AS (
    SELECT @start_date AS balance_date
    UNION ALL
    SELECT DATE_ADD(balance_date, INTERVAL 1 DAY)
    FROM calendar
    WHERE balance_date < @end_date
)
SELECT
    1,
    balance_date,
    100000.00
FROM calendar;

-- ----------------------------------------------------------------------------
-- 5. Seed Initial Sample Financial Transactions
-- ----------------------------------------------------------------------------
CALL sp_process_transaction(
    'TXN-SAMPLE-001',
    1,
    NULL,
    'DEPOSIT',
    5000.00,
    'Initial cash deposit'
);

CALL sp_process_transaction(
    'TXN-SAMPLE-002',
    1,
    NULL,
    'WITHDRAW',
    2000.00,
    'ATM cash withdrawal'
);

CALL sp_process_transaction(
    'TXN-SAMPLE-003',
    2,
    NULL,
    'DEPOSIT',
    10000.00,
    'Merchant settlement deposit'
);

CALL sp_process_transaction(
    'TXN-SAMPLE-004',
    1,
    2,
    'FUND_TRANSFER',
    3000.00,
    'Inter-account fund transfer'
);
