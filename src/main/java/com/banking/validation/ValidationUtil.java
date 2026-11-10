package com.banking.validation;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * General validation utility containing regex rules and checks.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,10}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{10,15}$"
    );

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[A-Za-z\\s'-]{2,50}$"
    );

    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile(
            "^[A-Z]{2}[0-9]{6,12}$"
    );

    private ValidationUtil() {
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidName(String name) {
        if (name == null) return false;
        return NAME_PATTERN.matcher(name.trim()).matches();
    }

    public static boolean isValidAccountNumber(String accNumber) {
        if (accNumber == null) return false;
        return ACCOUNT_NUMBER_PATTERN.matcher(accNumber.trim().toUpperCase()).matches();
    }

    public static boolean isValidPositiveAmount(BigDecimal amount) {
        if (amount == null) return false;
        return amount.compareTo(BigDecimal.ZERO) > 0 && amount.scale() <= 2;
    }
}
