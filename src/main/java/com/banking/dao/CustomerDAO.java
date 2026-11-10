package com.banking.dao;

import com.banking.enums.CustomerStatus;
import com.banking.model.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for Customer entities.
 */
public interface CustomerDAO {

    Optional<Customer> findById(Long customerId);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByCustomerCode(String customerCode);

    List<Customer> findAll();

    Customer save(Customer customer);

    boolean update(Customer customer);

    boolean updateStatus(Long customerId, CustomerStatus status);

    boolean updatePassword(Long customerId, String newPasswordHash);

    String generateNextCustomerCode();
}
