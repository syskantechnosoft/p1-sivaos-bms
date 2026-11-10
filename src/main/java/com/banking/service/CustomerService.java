package com.banking.service;

import com.banking.model.Customer;

import java.util.List;

/**
 * Service contract for managing customer registration, profile, and credentials.
 */
public interface CustomerService {

    Customer registerCustomer(Customer customer, String rawPassword);

    Customer getCustomerById(Long customerId);

    Customer getCustomerByEmail(String email);

    Customer getCustomerByCode(String code);

    List<Customer> getAllCustomers();

    boolean updateCustomerProfile(Customer customer);

    boolean changePassword(Long customerId, String oldPassword, String newPassword);

    boolean unlockCustomer(Long customerId);
}
