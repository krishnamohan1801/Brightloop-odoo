package com.brightloop.customer.service;

import com.brightloop.customer.dao.CustomerDAO;
import com.brightloop.customer.model.Customer;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerDAO customerDAO;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerDAO customerDAO, PasswordEncoder passwordEncoder) {
        this.customerDAO = customerDAO;
        this.passwordEncoder = passwordEncoder;
    }

    public Customer createCustomer(Customer customer) {

        normalize(customer);

        if (customerDAO.existsByEmail(customer.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        String temporarySecret = UUID.randomUUID() + UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(temporarySecret);

        UUID customerId = customerDAO.save(customer, passwordHash);

        return getCustomerById(customerId);
    }

    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    public Customer getCustomerById(UUID id) {
        return customerDAO.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    public Customer updateCustomer(UUID id, Customer customer) {
        getCustomerById(id);
        normalize(customer);

        if (customerDAO.existsByEmailAndIdNot(customer.getEmail(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already belongs to another user");
        }

        customerDAO.update(id, customer);

        return getCustomerById(id);
    }

    public void archiveCustomer(UUID id) {
        int rowsUpdated = customerDAO.archive(id);

        if (rowsUpdated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
        }
    }

    private void normalize(Customer customer) {
        customer.setEmail(customer.getEmail().trim().toLowerCase(Locale.ROOT));
        customer.setFullName(customer.getFullName().trim());

        if (customer.getPhone() != null) {
            customer.setPhone(customer.getPhone().trim());
        }
    }
}
