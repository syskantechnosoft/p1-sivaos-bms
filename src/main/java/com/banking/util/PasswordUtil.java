package com.banking.util;

import org.mindrot.jbcrypt.BCrypt;

import java.util.regex.Pattern;

/**
 * Utility for secure BCrypt password hashing, verification, and policy enforcement.
 */
public final class PasswordUtil {

    private static final int DEFAULT_LOG_ROUNDS = 12;

    // Minimum 8 characters, at least 1 uppercase, 1 lowercase, 1 digit, 1 special character
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).{8,64}$"
    );

    private PasswordUtil() {
        // Private constructor for utility class
    }

    /**
     * Hashes a plain-text password using BCrypt with salt rounds = 12.
     *
     * @param plainPassword plain-text password
     * @return BCrypt hashed password
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(DEFAULT_LOG_ROUNDS));
    }

    /**
     * Verifies a plain-text password against a BCrypt hash.
     *
     * @param plainPassword  plain-text password to check
     * @param hashedPassword stored BCrypt hash
     * @return true if password matches, false otherwise
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Malformed hash
            return false;
        }
    }

    /**
     * Validates whether the password meets bank security complexity requirements.
     *
     * @param password candidate password
     * @return true if valid
     */
    public static boolean isValidComplexity(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public static String getPasswordRequirementsDescription() {
        return "Must be 8-64 characters long and include at least one uppercase letter, " +
                "one lowercase letter, one digit, and one special character (@#$%^&+=!._-).";
    }

    public static void main(String[] args) {
        String pwd = (args != null && args.length > 0) ? args[0] : "Pass@123";
        System.out.println("BCRYPT_HASH: " + hash(pwd));
    }
}
