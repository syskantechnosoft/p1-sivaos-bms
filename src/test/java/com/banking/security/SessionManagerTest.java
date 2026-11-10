package com.banking.security;

import com.banking.exceptions.SessionExpiredException;
import com.banking.model.Customer;
import com.banking.model.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Session & Inactivity Timeout Tests")
class SessionManagerTest {

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setCustomerId(1L);
        sampleCustomer.setCustomerCode("CUST001");
        sampleCustomer.setEmail("arun@example.com");
        sampleCustomer.setFirstName("Arun");
        sampleCustomer.setLastName("Kumar");
        SessionManager.invalidateCurrentSession();
    }

    @AfterEach
    void tearDown() {
        SessionManager.invalidateCurrentSession();
    }

    @Test
    @DisplayName("Should create active session with default 2 minutes timeout")
    void shouldCreateActiveSession() {
        UserSession session = SessionManager.createCustomerSession(sampleCustomer);

        assertNotNull(session);
        assertTrue(session.isActive());
        assertFalse(session.isExpired());
        assertEquals("arun@example.com", session.getSessionUser());
        assertFalse(session.isAdmin());
        // Default 2 minutes = 120,000 ms
        assertEquals(120_000L, session.getTimeoutMillis());
    }

    @Test
    @DisplayName("Should expire session after inactivity exceeds 2 minutes")
    void shouldExpireAfterInactivity() {
        UserSession session = SessionManager.createCustomerSession(sampleCustomer);

        // Simulate 2 minutes + 1 second of inactivity (121,000 ms ago)
        long expiredTime = System.currentTimeMillis() - 121_000L;
        session.setLastAccessTimeMillis(expiredTime);

        assertTrue(session.isExpired());

        // Validating expired session should throw SessionExpiredException
        assertThrows(SessionExpiredException.class, SessionManager::validateAndTouch);
    }

    @Test
    @DisplayName("Should touch and update last access timestamp on valid activity")
    void shouldTouchAndUpdateActivityTime() {
        UserSession session = SessionManager.createCustomerSession(sampleCustomer);
        long initialTime = session.getLastAccessTimeMillis();

        // Simulate small delay
        session.setLastAccessTimeMillis(initialTime - 5000);
        UserSession validated = SessionManager.validateAndTouch();

        assertNotNull(validated);
        assertTrue(validated.getLastAccessTimeMillis() > (initialTime - 5000));
    }

    @Test
    @DisplayName("Should invalidate session cleanly upon logout")
    void shouldInvalidateOnLogout() {
        SessionManager.createCustomerSession(sampleCustomer);
        assertTrue(SessionManager.hasActiveSession());

        SessionManager.invalidateCurrentSession();
        assertFalse(SessionManager.hasActiveSession());
        assertThrows(SessionExpiredException.class, SessionManager::validateAndTouch);
    }
}
