package com.brightloop.sms.service;

import com.brightloop.sms.dao.AuditLogDao;
import com.brightloop.sms.dao.InvoiceDao;
import com.brightloop.sms.dao.SubscriptionDao;
import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CustomerService {

    private final UserDao userDao;
    private final SubscriptionDao subscriptionDao;
    private final InvoiceDao invoiceDao;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogDao auditLogDao;

    public CustomerService(
            UserDao userDao,
            SubscriptionDao subscriptionDao,
            InvoiceDao invoiceDao,
            PasswordEncoder passwordEncoder,
            AuditLogDao auditLogDao) {
        this.userDao = userDao;
        this.subscriptionDao = subscriptionDao;
        this.invoiceDao = invoiceDao;
        this.passwordEncoder = passwordEncoder;
        this.auditLogDao = auditLogDao;
    }

    public List<User> getAllCustomers() {
        return userDao.findByRole("customer");
    }

    public List<User> getAllStaff() {
        return userDao.findByRole("staff");
    }

    public User getCustomerById(UUID id) {
        return userDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id));
    }

    public User createCustomer(User user, UUID currentUserId) {
        if (userDao.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("A user with this email already exists");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode("Customer@1234")); // Default password if created by staff
        } else {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        user.setRole("customer");
        user.setIsActive(true);
        User created = userDao.create(user);
        auditLogDao.log(currentUserId, "CUSTOMER_CREATE", "users", created.getId().toString(), "Created customer: " + created.getFullName());
        return created;
    }

    public User updateCustomer(UUID id, User user, UUID currentUserId) {
        user.setId(id);
        user.setRole("customer");
        userDao.update(user);
        auditLogDao.log(currentUserId, "CUSTOMER_UPDATE", "users", id.toString(), "Updated customer details: " + user.getFullName());
        return getCustomerById(id);
    }

    public Map<String, Object> getCustomer360(UUID customerId) {
        User customer = getCustomerById(customerId);
        var subscriptions = subscriptionDao.findAll(null, customerId);
        var invoices = invoiceDao.findAll(null, customerId, false);

        Map<String, Object> details = new HashMap<>();
        details.put("customer", customer);
        details.put("subscriptions", subscriptions);
        details.put("invoices", invoices);
        return details;
    }
}
