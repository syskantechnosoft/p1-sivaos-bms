package com.banking.service.impl;

import com.banking.dao.CustomerDAO;
import com.banking.enums.CustomerStatus;
import com.banking.exceptions.AuthenticationException;
import com.banking.exceptions.CustomerNotFoundException;
import com.banking.exceptions.ValidationException;
import com.banking.model.Customer;
import com.banking.service.CustomerService;
import com.banking.util.AppLogger;
import com.banking.util.PasswordUtil;
import com.banking.validation.CustomerValidator;
import com.banking.validation.ValidationResult;

import java.util.List;

/**
 * Implementation of CustomerService.
 */
public class CustomerServiceImpl implements CustomerService {

    private final CustomerDAO customerDAO;
    private final CustomerValidator customerValidator;

    public CustomerServiceImpl(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
        this.customerValidator = new CustomerValidator();
    }

    public CustomerServiceImpl(CustomerDAO customerDAO, CustomerValidator customerValidator) {
        this.customerDAO = customerDAO;
        this.customerValidator = customerValidator;
    }

    @Override
    public Customer registerCustomer(Customer customer, String rawPassword) {
        ValidationResult validation = customerValidator.validate(customer, rawPassword);
        if (!validation.isValid()) {
            throw new ValidationException(validation.getErrors());
        }

        // Check uniqueness of email
        if (customerDAO.findByEmail(customer.getEmail()).isPresent()) {
            throw new ValidationException("Email address '" + customer.getEmail() + "' is already registered.");
        }

        // Generate customer code if not present
        if (customer.getCustomerCode() == null || customer.getCustomerCode().trim().isEmpty()) {
            customer.setCustomerCode(customerDAO.generateNextCustomerCode());
        }

        // Hash password securely with BCrypt
        String hashed = PasswordUtil.hash(rawPassword);
        customer.setPasswordHash(hashed);
        customer.setStatus(CustomerStatus.ACTIVE);

        Customer saved = customerDAO.save(customer);
        AppLogger.logAccount("CUSTOMER_REGISTERED: ID={}, Code={}, Email={}",
                saved.getCustomerId(), saved.getCustomerCode(), saved.getEmail());
        return saved;
    }

    @Override
    public Customer getCustomerById(Long customerId) {
        return customerDAO.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + customerId + " not found."));
    }

    @Override
    public Customer getCustomerByEmail(String email) {
        return customerDAO.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with email '" + email + "' not found."));
    }

    @Override
    public Customer getCustomerByCode(String code) {
        return customerDAO.findByCustomerCode(code)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with code '" + code + "' not found."));
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    @Override
    public boolean updateCustomerProfile(Customer customer) {
        Customer existing = getCustomerById(customer.getCustomerId());
        existing.setFirstName(customer.getFirstName());
        existing.setLastName(customer.getLastName());
        existing.setPhone(customer.getPhone());
        existing.setAddress(customer.getAddress());
        boolean ok = customerDAO.update(existing);
        if (ok) {
            AppLogger.logAccount("CUSTOMER_UPDATED: ID={}, Code={}", existing.getCustomerId(), existing.getCustomerCode());
        }
        return ok;
    }

    @Override
    public boolean changePassword(Long customerId, String oldPassword, String newPassword) {
        Customer customer = getCustomerById(customerId);
        if (!PasswordUtil.verify(oldPassword, customer.getPasswordHash())) {
            throw new AuthenticationException("Current password does not match.");
        }

        if (!PasswordUtil.isValidComplexity(newPassword)) {
            throw new ValidationException("New password does not meet requirements: " + PasswordUtil.getPasswordRequirementsDescription());
        }

        String newHash = PasswordUtil.hash(newPassword);
        boolean ok = customerDAO.updatePassword(customerId, newHash);
        if (ok) {
            AppLogger.logAudit("PASSWORD_CHANGED: Customer ID {} changed password successfully.", customerId);
        }
        return ok;
    }

    @Override
    public boolean unlockCustomer(Long customerId) {
        Customer customer = getCustomerById(customerId);
        boolean ok = customerDAO.updateStatus(customerId, CustomerStatus.ACTIVE);
        if (ok) {
            AppLogger.logAudit("ACCOUNT_UNLOCKED: Customer {} ({}) unlocked.", customer.getCustomerCode(), customer.getEmail());
        }
        return ok;
    }
}
