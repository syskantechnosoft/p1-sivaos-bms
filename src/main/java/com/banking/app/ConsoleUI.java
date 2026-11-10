package com.banking.app;

import com.banking.config.DatabaseConfig;
import com.banking.model.UserSession;
import com.banking.security.SessionManager;

/**
 * Console UI styling and presentation helper with ANSI colors and headers.
 */
public final class ConsoleUI {

    // ANSI Escape Codes
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    private ConsoleUI() {
    }

    public static void printBanner() {
        System.out.println(CYAN + BOLD);
        System.out.println("==========================================================================");
        System.out.println("   ____  ____  ____    ____  ___   _   _ _  _____ _   _  ____             ");
        System.out.println("  / ___||  _ \\| __ )  | __ )/ _ \\ | \\ | | |/ /_ _| \\ | |/ ___|            ");
        System.out.println("  \\___ \\| |_) |  _ \\  |  _ \\ |_| ||  \\| | ' / | ||  \\| | |  _             ");
        System.out.println("   ___) |  _ <| |_) | | |_) |  _  || |\\  | . \\ | || |\\  | |_| |            ");
        System.out.println("  |____/|_| \\_\\____/  |____/|_| |_||_| \\_|_|\\_\\___|_| \\_|\\____|            ");
        System.out.println("        Core Java 25 & JDBC Secure Banking CLI Application                ");
        System.out.println("==========================================================================" + RESET);
        System.out.println(WHITE + " Bank: " + DatabaseConfig.getBankName() +
                " | Security: BCrypt (12) + Inactivity Timeout (2m) + Auto-Lock (3x)" + RESET);
        System.out.println();
    }

    public static void printHeader(String title) {
        System.out.println();
        System.out.println(BLUE + BOLD + "--- " + title.toUpperCase() + " ---" + RESET);
        printSessionBar();
    }

    public static void printSessionBar() {
        UserSession session = SessionManager.getCurrentSession();
        if (session != null && !session.isExpired()) {
            long rem = session.getRemainingSeconds();
            String userDesc = session.isAdmin() ? "ADMINISTRATOR" : session.getCustomer().getFullName();
            System.out.println(YELLOW + "[Active Session: " + userDesc + " | Inactivity Timeout in: " +
                    (rem / 60) + "m " + (rem % 60) + "s]" + RESET);
        }
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + BOLD + "[SUCCESS] " + message + RESET);
    }

    public static void printError(String message) {
        System.out.println(RED + BOLD + "[ERROR] " + message + RESET);
    }

    public static void printWarning(String message) {
        System.out.println(YELLOW + BOLD + "[WARNING] " + message + RESET);
    }

    public static void printInfo(String message) {
        System.out.println(CYAN + "[INFO] " + message + RESET);
    }

    public static void printDivider() {
        System.out.println(WHITE + "--------------------------------------------------------------------------" + RESET);
    }
}
