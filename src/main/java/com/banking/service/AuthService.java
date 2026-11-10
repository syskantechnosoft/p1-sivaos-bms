package com.banking.service;

import com.banking.model.Customer;

/**
 * Service contract for user authentication and session establishment.
 */
public interface AuthService {

    Customer loginCustomer(String identifier, String password, String ipAddress, String deviceInfo);

    boolean loginAdmin(String username, String password, String ipAddress, String deviceInfo);

    void logout();
}
