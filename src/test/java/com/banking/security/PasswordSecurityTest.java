package com.banking.security;

import com.banking.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Password Security & BCrypt Tests")
class PasswordSecurityTest {

    @Test
    @DisplayName("Should hash plain password and successfully verify with BCrypt")
    void shouldHashAndVerifyPassword() {
        String raw = "SecurePass@2026";
        String hash = PasswordUtil.hash(raw);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$"));
        assertTrue(PasswordUtil.verify(raw, hash));
        assertFalse(PasswordUtil.verify("WrongPassword@123", hash));
    }

    @Test
    @DisplayName("Should enforce complexity requirements")
    void shouldEnforcePasswordComplexity() {
        // Valid
        assertTrue(PasswordUtil.isValidComplexity("Pass@123"));
        assertTrue(PasswordUtil.isValidComplexity("Revature#2026"));
        assertTrue(PasswordUtil.isValidComplexity("Admin$999"));

        // Invalid: missing special char
        assertFalse(PasswordUtil.isValidComplexity("Password123"));
        // Invalid: missing uppercase
        assertFalse(PasswordUtil.isValidComplexity("pass@123"));
        // Invalid: missing digit
        assertFalse(PasswordUtil.isValidComplexity("Password@"));
        // Invalid: too short
        assertFalse(PasswordUtil.isValidComplexity("P@1a"));
        // Invalid: null
        assertFalse(PasswordUtil.isValidComplexity(null));
    }

    @Test
    @DisplayName("Should fail verification safely on malformed hash")
    void shouldHandleMalformedHashGracefully() {
        assertFalse(PasswordUtil.verify("Pass@123", "not_a_valid_hash"));
        assertFalse(PasswordUtil.verify(null, "$2a$12$dummy"));
        assertFalse(PasswordUtil.verify("Pass@123", null));
    }
}
