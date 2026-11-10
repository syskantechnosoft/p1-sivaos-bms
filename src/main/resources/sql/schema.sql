-- ============================================================================
-- Banking Management System (BMS) - Database Schema
-- Compatible with MySQL 8.0+
-- ============================================================================

DROP DATABASE IF EXISTS banking_db;
CREATE DATABASE banking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE banking_db;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 1. Customers Table
-- ----------------------------------------------------------------------------
CREATE TABLE customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(15) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    date_of_birth DATE NULL,
    address VARCHAR(255) NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_customer_email (email),
    INDEX idx_customer_code (customer_code)
);

-- Trigger: Ensure date of birth is strictly in the past
DELIMITER //

CREATE TRIGGER trg_check_dob_insert
BEFORE INSERT ON customers
FOR EACH ROW
BEGIN
    IF NEW.date_of_birth IS NOT NULL AND NEW.date_of_birth >= CURRENT_DATE THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'Date of birth must be in the past.';
    END IF;
END //

CREATE TRIGGER trg_check_dob_update
BEFORE UPDATE ON customers
FOR EACH ROW
BEGIN
    IF NEW.date_of_birth IS NOT NULL AND NEW.date_of_birth >= CURRENT_DATE THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'Date of birth must be in the past.';
    END IF;
END //

DELIMITER ;

-- ----------------------------------------------------------------------------
-- 2. Accounts Table
-- ----------------------------------------------------------------------------
CREATE TABLE accounts (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    account_type ENUM('SAVINGS', 'CURRENT', 'LOAN', 'DEPOSIT') NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    interest_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    minimum_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    opening_date DATE NOT NULL,
    maturity_date DATE NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'CLOSED', 'FROZEN') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_account_balance
        CHECK (balance >= 0),
    CONSTRAINT chk_interest_rate
        CHECK (interest_rate BETWEEN 0 AND 100),
    CONSTRAINT chk_minimum_balance
        CHECK (minimum_balance >= 0),
    CONSTRAINT chk_maturity_date
        CHECK (maturity_date IS NULL OR maturity_date >= opening_date),
    INDEX idx_accounts_customer (customer_id),
    INDEX idx_accounts_type (account_type),
    INDEX idx_accounts_status (status)
);

-- ----------------------------------------------------------------------------
-- 3. Transactions Table
-- ----------------------------------------------------------------------------
CREATE TABLE transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_ref VARCHAR(40) NOT NULL UNIQUE,
    account_id BIGINT NOT NULL,
    destination_account_id BIGINT NULL,
    transaction_type ENUM('DEPOSIT', 'WITHDRAW', 'MATURITY', 'FUND_TRANSFER', 'INTEREST_CREDIT') NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_before DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    description VARCHAR(255) NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('SUCCESS', 'FAILED', 'PENDING') NOT NULL DEFAULT 'SUCCESS',
    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT fk_transactions_destination
        FOREIGN KEY (destination_account_id) REFERENCES accounts(account_id),
    CONSTRAINT chk_transaction_amount
        CHECK (amount > 0),
    CONSTRAINT chk_transaction_balances
        CHECK (balance_before >= 0 AND balance_after >= 0),
    CONSTRAINT chk_transfer_destination
        CHECK (
            (transaction_type = 'FUND_TRANSFER'
                AND destination_account_id IS NOT NULL
                AND destination_account_id <> account_id)
            OR
            (transaction_type <> 'FUND_TRANSFER'
                AND destination_account_id IS NULL)
        ),
    INDEX idx_transaction_account (account_id),
    INDEX idx_transaction_dest (destination_account_id),
    INDEX idx_transaction_date (transaction_date),
    INDEX idx_transaction_type (transaction_type)
);

-- ----------------------------------------------------------------------------
-- 4. Login Audit Table
-- ----------------------------------------------------------------------------
CREATE TABLE login_audit (
    login_audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NULL,
    username VARCHAR(100) NOT NULL,
    login_status ENUM('SUCCESS', 'FAILED', 'LOCKED', 'LOGOUT') NOT NULL,
    ip_address VARCHAR(45) NULL,
    device_info VARCHAR(255) NULL,
    failure_reason VARCHAR(255) NULL,
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_login_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON DELETE SET NULL,
    INDEX idx_login_customer (customer_id),
    INDEX idx_login_username (username),
    INDEX idx_login_time (login_time),
    INDEX idx_login_status (login_status)
);

-- ----------------------------------------------------------------------------
-- 5. Transaction Audit Table
-- ----------------------------------------------------------------------------
CREATE TABLE transaction_audit (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    operation ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
    old_amount DECIMAL(15,2) NULL,
    new_amount DECIMAL(15,2) NULL,
    old_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NULL,
    changed_by VARCHAR(100) NULL,
    audit_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
    INDEX idx_audit_transaction (transaction_id),
    INDEX idx_audit_time (audit_time)
);

-- ----------------------------------------------------------------------------
-- 6. Account Daily Balance Table (For MAB & Quarterly Interest Calculation)
-- ----------------------------------------------------------------------------
CREATE TABLE account_daily_balance (
    snapshot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    balance_date DATE NOT NULL,
    closing_balance DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_balance_account
        FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT chk_closing_balance
        CHECK (closing_balance >= 0),
    CONSTRAINT uk_account_balance_date
        UNIQUE (account_id, balance_date),
    INDEX idx_balance_date (balance_date)
);

-- ----------------------------------------------------------------------------
-- 7. Audit Triggers
-- ----------------------------------------------------------------------------
DELIMITER $$

CREATE TRIGGER trg_transaction_after_insert
AFTER INSERT ON transactions
FOR EACH ROW
BEGIN
    INSERT INTO transaction_audit (
        transaction_id,
        operation,
        new_amount,
        new_status,
        changed_by
    ) VALUES (
        NEW.transaction_id,
        'INSERT',
        NEW.amount,
        NEW.status,
        CURRENT_USER()
    );
END$$

CREATE TRIGGER trg_transaction_after_update
AFTER UPDATE ON transactions
FOR EACH ROW
BEGIN
    INSERT INTO transaction_audit (
        transaction_id,
        operation,
        old_amount,
        new_amount,
        old_status,
        new_status,
        changed_by
    ) VALUES (
        NEW.transaction_id,
        'UPDATE',
        OLD.amount,
        NEW.amount,
        OLD.status,
        NEW.status,
        CURRENT_USER()
    );
END$$

CREATE TRIGGER trg_transaction_after_delete
AFTER DELETE ON transactions
FOR EACH ROW
BEGIN
    INSERT INTO transaction_audit (
        transaction_id,
        operation,
        old_amount,
        old_status,
        changed_by
    ) VALUES (
        OLD.transaction_id,
        'DELETE',
        OLD.amount,
        OLD.status,
        CURRENT_USER()
    );
END$$

DELIMITER ;

-- ----------------------------------------------------------------------------
-- 8. Stored Functions
-- ----------------------------------------------------------------------------
DELIMITER $$

CREATE FUNCTION fn_monthly_average_balance(
    p_account_id BIGINT,
    p_year INT,
    p_month INT
)
RETURNS DECIMAL(15,2)
READS SQL DATA
BEGIN
    DECLARE v_total_balance DECIMAL(20,2) DEFAULT 0;
    DECLARE v_days INT DEFAULT 0;
    DECLARE v_result DECIMAL(15,2) DEFAULT 0;

    SELECT
        COALESCE(SUM(closing_balance), 0),
        COUNT(*)
    INTO
        v_total_balance,
        v_days
    FROM account_daily_balance
    WHERE account_id = p_account_id
      AND YEAR(balance_date) = p_year
      AND MONTH(balance_date) = p_month;

    SET v_days = DAY(
        LAST_DAY(
            STR_TO_DATE(
                CONCAT(p_year, '-', p_month, '-01'),
                '%Y-%m-%d'
            )
        )
    );

    IF (
        SELECT COUNT(*)
        FROM account_daily_balance
        WHERE account_id = p_account_id
          AND balance_date >= STR_TO_DATE(
              CONCAT(p_year, '-', LPAD(p_month, 2, '0'), '-01'),
              '%Y-%m-%d'
          )
          AND balance_date <= LAST_DAY(
              STR_TO_DATE(
                  CONCAT(p_year, '-', LPAD(p_month, 2, '0'), '-01'),
                  '%Y-%m-%d'
              )
          )
    ) < v_days THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Incomplete daily balance snapshots for the month';
    END IF;

    SET v_result = v_total_balance / v_days;
    RETURN ROUND(v_result, 2);
END$$

CREATE FUNCTION fn_quarterly_interest(
    p_account_id BIGINT,
    p_year INT,
    p_quarter INT
)
RETURNS DECIMAL(15,2)
READS SQL DATA
BEGIN
    DECLARE v_rate DECIMAL(5,2);
    DECLARE v_total_interest DECIMAL(20,4) DEFAULT 0;
    DECLARE v_month INT;
    DECLARE v_start_month INT;

    SELECT interest_rate
    INTO v_rate
    FROM accounts
    WHERE account_id = p_account_id
      AND account_type = 'SAVINGS';

    IF v_rate IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Account not found or account is not a savings account';
    END IF;

    IF p_quarter NOT BETWEEN 1 AND 4 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid quarter';
    END IF;

    SET v_start_month = (p_quarter - 1) * 3 + 1;
    SET v_month = v_start_month;

    WHILE v_month < v_start_month + 3 DO
        SET v_total_interest =
            v_total_interest +
            (
                fn_monthly_average_balance(
                    p_account_id,
                    p_year,
                    v_month
                ) * v_rate / 1200
            );
        SET v_month = v_month + 1;
    END WHILE;

    RETURN ROUND(v_total_interest, 2);
END$$

DELIMITER ;

-- ----------------------------------------------------------------------------
-- 9. Stored Procedures
-- ----------------------------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE sp_process_transaction(
    IN p_transaction_ref VARCHAR(40),
    IN p_account_id BIGINT,
    IN p_destination_account_id BIGINT,
    IN p_transaction_type VARCHAR(30),
    IN p_amount DECIMAL(15,2),
    IN p_description VARCHAR(255)
)
BEGIN
    DECLARE v_balance DECIMAL(15,2);
    DECLARE v_destination_balance DECIMAL(15,2);
    DECLARE v_new_balance DECIMAL(15,2);
    DECLARE v_account_type VARCHAR(20);
    DECLARE v_account_status VARCHAR(20);
    DECLARE v_destination_status VARCHAR(20);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_amount <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Transaction amount must be positive';
    END IF;

    START TRANSACTION;

    SELECT balance, account_type, status
    INTO v_balance, v_account_type, v_account_status
    FROM accounts
    WHERE account_id = p_account_id
    FOR UPDATE;

    IF v_account_status <> 'ACTIVE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Source account is not active';
    END IF;

    SET v_new_balance = v_balance;

    CASE p_transaction_type
        WHEN 'DEPOSIT' THEN
            SET v_new_balance = v_balance + p_amount;

        WHEN 'MATURITY' THEN
            IF v_account_type <> 'DEPOSIT' THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Maturity credit requires a deposit account';
            END IF;
            SET v_new_balance = v_balance + p_amount;

        WHEN 'INTEREST_CREDIT' THEN
            IF v_account_type <> 'SAVINGS' THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Interest credit requires a savings account';
            END IF;
            SET v_new_balance = v_balance + p_amount;

        WHEN 'WITHDRAW' THEN
            IF v_balance < p_amount THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Insufficient balance';
            END IF;

            IF v_account_type = 'SAVINGS'
               AND v_balance - p_amount < (
                   SELECT minimum_balance
                   FROM accounts
                   WHERE account_id = p_account_id
               ) THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Minimum savings balance requirement violated';
            END IF;

            SET v_new_balance = v_balance - p_amount;

        WHEN 'FUND_TRANSFER' THEN
            IF p_destination_account_id IS NULL
               OR p_destination_account_id = p_account_id THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Invalid destination account';
            END IF;

            IF v_balance < p_amount THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Insufficient transfer balance';
            END IF;

            SELECT balance, status
            INTO v_destination_balance, v_destination_status
            FROM accounts
            WHERE account_id = p_destination_account_id
            FOR UPDATE;

            IF v_destination_status <> 'ACTIVE' THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Destination account is not active';
            END IF;

            SET v_new_balance = v_balance - p_amount;

            UPDATE accounts
            SET balance = v_new_balance
            WHERE account_id = p_account_id;

            UPDATE accounts
            SET balance = balance + p_amount
            WHERE account_id = p_destination_account_id;

        ELSE
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Invalid transaction type';
    END CASE;

    IF p_transaction_type <> 'FUND_TRANSFER' THEN
        UPDATE accounts
        SET balance = v_new_balance
        WHERE account_id = p_account_id;
    END IF;

    INSERT INTO transactions (
        transaction_ref,
        account_id,
        destination_account_id,
        transaction_type,
        amount,
        balance_before,
        balance_after,
        description,
        status
    )
    VALUES (
        p_transaction_ref,
        p_account_id,
        IF(p_transaction_type = 'FUND_TRANSFER', p_destination_account_id, NULL),
        p_transaction_type,
        p_amount,
        v_balance,
        v_new_balance,
        p_description,
        'SUCCESS'
    );

    COMMIT;
END$$

CREATE PROCEDURE sp_capture_daily_balances(
    IN p_balance_date DATE
)
BEGIN
    INSERT INTO account_daily_balance (
        account_id,
        balance_date,
        closing_balance
    )
    SELECT
        account_id,
        p_balance_date,
        balance
    FROM accounts
    WHERE status IN ('ACTIVE', 'FROZEN')
    ON DUPLICATE KEY UPDATE
        closing_balance = VALUES(closing_balance);
END$$

CREATE PROCEDURE sp_credit_quarterly_interest(
    IN p_year INT,
    IN p_quarter INT
)
BEGIN
    DECLARE v_account_id BIGINT;
    DECLARE v_interest DECIMAL(15,2);
    DECLARE v_transaction_ref VARCHAR(40);
    DECLARE v_done INT DEFAULT 0;

    DECLARE cur_accounts CURSOR FOR
        SELECT account_id
        FROM accounts
        WHERE account_type = 'SAVINGS'
          AND status = 'ACTIVE';

    DECLARE CONTINUE HANDLER FOR NOT FOUND
        SET v_done = 1;

    IF p_quarter NOT BETWEEN 1 AND 4 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Quarter must be between 1 and 4';
    END IF;

    OPEN cur_accounts;

    interest_loop: LOOP
        FETCH cur_accounts INTO v_account_id;
        IF v_done = 1 THEN
            LEAVE interest_loop;
        END IF;

        SET v_interest = fn_quarterly_interest(v_account_id, p_year, p_quarter);
        SET v_transaction_ref = CONCAT('INT-', v_account_id, '-', p_year, '-Q', p_quarter);

        IF v_interest > 0 THEN
            CALL sp_process_transaction(
                v_transaction_ref,
                v_account_id,
                NULL,
                'INTEREST_CREDIT',
                v_interest,
                CONCAT('Quarterly interest credit Q', p_quarter, ' ', p_year)
            );
        END IF;
    END LOOP;

    CLOSE cur_accounts;
END$$

DELIMITER ;
