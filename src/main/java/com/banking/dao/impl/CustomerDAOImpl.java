package com.banking.dao.impl;

import com.banking.config.DBConnection;
import com.banking.dao.CustomerDAO;
import com.banking.enums.CustomerStatus;
import com.banking.exceptions.DatabaseException;
import com.banking.model.Customer;
import com.banking.util.AppLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of CustomerDAO.
 */
public class CustomerDAOImpl implements CustomerDAO {

    @Override
    public Optional<Customer> findById(Long customerId) {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCustomer(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error finding customer by ID: {}", e.getMessage());
            throw new DatabaseException("Failed to find customer by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        String sql = "SELECT * FROM customers WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCustomer(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error finding customer by email: {}", e.getMessage());
            throw new DatabaseException("Failed to find customer by email", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Customer> findByCustomerCode(String customerCode) {
        String sql = "SELECT * FROM customers WHERE customer_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerCode.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCustomer(rs));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Error finding customer by code: {}", e.getMessage());
            throw new DatabaseException("Failed to find customer by code", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Customer> findAll() {
        String sql = "SELECT * FROM customers ORDER BY customer_id ASC";
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToCustomer(rs));
            }
        } catch (SQLException e) {
            AppLogger.error("Error finding all customers: {}", e.getMessage());
            throw new DatabaseException("Failed to list customers", e);
        }
        return list;
    }

    @Override
    public Customer save(Customer customer) {
        String sql = "INSERT INTO customers (customer_code, first_name, last_name, email, phone, " +
                "password_hash, date_of_birth, address, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getCustomerCode());
            ps.setString(2, customer.getFirstName());
            ps.setString(3, customer.getLastName());
            ps.setString(4, customer.getEmail().toLowerCase());
            ps.setString(5, customer.getPhone());
            ps.setString(6, customer.getPasswordHash());
            if (customer.getDateOfBirth() != null) {
                ps.setDate(7, Date.valueOf(customer.getDateOfBirth()));
            } else {
                ps.setNull(7, Types.DATE);
            }
            ps.setString(8, customer.getAddress());
            ps.setString(9, customer.getStatus() != null ? customer.getStatus().name() : CustomerStatus.ACTIVE.name());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Creating customer failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    customer.setCustomerId(generatedKeys.getLong(1));
                }
            }
            return customer;
        } catch (SQLException e) {
            AppLogger.error("Error saving customer: {}", e.getMessage());
            throw new DatabaseException("Failed to save customer: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Customer customer) {
        String sql = "UPDATE customers SET first_name = ?, last_name = ?, phone = ?, address = ? WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getFirstName());
            ps.setString(2, customer.getLastName());
            ps.setString(3, customer.getPhone());
            ps.setString(4, customer.getAddress());
            ps.setLong(5, customer.getCustomerId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.error("Error updating customer: {}", e.getMessage());
            throw new DatabaseException("Failed to update customer", e);
        }
    }

    @Override
    public boolean updateStatus(Long customerId, CustomerStatus status) {
        String sql = "UPDATE customers SET status = ? WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, customerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.error("Error updating customer status: {}", e.getMessage());
            throw new DatabaseException("Failed to update customer status", e);
        }
    }

    @Override
    public boolean updatePassword(Long customerId, String newPasswordHash) {
        String sql = "UPDATE customers SET password_hash = ? WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setLong(2, customerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.error("Error updating password: {}", e.getMessage());
            throw new DatabaseException("Failed to update password", e);
        }
    }

    @Override
    public String generateNextCustomerCode() {
        String sql = "SELECT MAX(customer_id) FROM customers";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            long nextId = 1;
            if (rs.next()) {
                nextId = rs.getLong(1) + 1;
            }
            return String.format("CUST%03d", nextId);
        } catch (SQLException e) {
            return "CUST" + System.currentTimeMillis() % 10000;
        }
    }

    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getLong("customer_id"));
        c.setCustomerCode(rs.getString("customer_code"));
        c.setFirstName(rs.getString("first_name"));
        c.setLastName(rs.getString("last_name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setPasswordHash(rs.getString("password_hash"));
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) {
            c.setDateOfBirth(dob.toLocalDate());
        }
        c.setAddress(rs.getString("address"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            c.setStatus(CustomerStatus.valueOf(statusStr));
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            c.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            c.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return c;
    }
}
