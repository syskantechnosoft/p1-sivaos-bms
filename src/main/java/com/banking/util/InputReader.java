package com.banking.util;

import com.banking.app.ConsoleUI;
import com.banking.exceptions.SessionExpiredException;
import com.banking.security.SessionManager;

import java.io.Console;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Scanner;

/**
 * Robust console input reader with session inactivity validation and cancellation support.
 */
public class InputReader {

    private final Scanner scanner;
    private final Console console;

    public InputReader() {
        this.scanner = new Scanner(System.in);
        this.console = System.console();
    }

    public InputReader(Scanner scanner) {
        this.scanner = scanner;
        this.console = null;
    }

    /**
     * Reads a line of text. Checks session expiration before and after input.
     */
    public String readString(String prompt) {
        checkSessionIfActive();
        System.out.print(ConsoleUI.WHITE + prompt + ": " + ConsoleUI.RESET);
        String input = scanner.hasNextLine() ? scanner.nextLine() : "";
        checkSessionIfActive();
        return input.trim();
    }

    public String readRequiredString(String prompt) {
        while (true) {
            String val = readString(prompt);
            if (!val.isEmpty()) {
                return val;
            }
            ConsoleUI.printWarning("This field cannot be empty. Please enter a value (or 'cancel' to abort).");
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String raw = readString(prompt);
            if (raw.equalsIgnoreCase("cancel")) {
                throw new RuntimeException("Operation cancelled by user.");
            }
            try {
                int val = Integer.parseInt(raw);
                if (val >= min && val <= max) {
                    return val;
                }
                ConsoleUI.printWarning("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                ConsoleUI.printWarning("Invalid integer format. Please enter a valid number.");
            }
        }
    }

    public BigDecimal readBigDecimal(String prompt) {
        while (true) {
            String raw = readString(prompt);
            if (raw.equalsIgnoreCase("cancel") || raw.equals("0")) {
                throw new RuntimeException("Operation cancelled by user.");
            }
            try {
                BigDecimal val = new BigDecimal(raw);
                if (val.compareTo(BigDecimal.ZERO) > 0) {
                    return val;
                }
                ConsoleUI.printWarning("Amount must be greater than zero.");
            } catch (NumberFormatException e) {
                ConsoleUI.printWarning("Invalid amount format. (Example: 2500.00)");
            }
        }
    }

    public LocalDate readDate(String prompt, boolean allowPastOnly) {
        while (true) {
            String raw = readString(prompt + " (YYYY-MM-DD)");
            if (raw.equalsIgnoreCase("cancel") || raw.isEmpty()) {
                return null;
            }
            try {
                LocalDate date = DateUtil.parseDate(raw);
                if (allowPastOnly && !date.isBefore(LocalDate.now())) {
                    ConsoleUI.printWarning("Date must be in the past.");
                    continue;
                }
                return date;
            } catch (Exception e) {
                ConsoleUI.printWarning("Invalid date. Please use format YYYY-MM-DD (e.g. 1995-10-20).");
            }
        }
    }

    public String readPassword(String prompt) {
        checkSessionIfActive();
        System.out.print(ConsoleUI.WHITE + prompt + ": " + ConsoleUI.RESET);
        String pass;
        if (console != null) {
            char[] chars = console.readPassword();
            pass = (chars != null) ? new String(chars) : "";
        } else {
            pass = scanner.hasNextLine() ? scanner.nextLine() : "";
        }
        checkSessionIfActive();
        return pass != null ? pass.trim() : "";
    }

    private void checkSessionIfActive() {
        if (SessionManager.getCurrentSession() != null) {
            // Validate and touch
            SessionManager.validateAndTouch();
        }
    }
}
