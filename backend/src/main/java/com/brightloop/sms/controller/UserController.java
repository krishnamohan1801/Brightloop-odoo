package com.brightloop.sms.controller;

import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.dto.RegisterRequest;
import com.brightloop.sms.model.User;
import com.brightloop.sms.service.AuthService;
import com.brightloop.sms.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users & Customers", description = "Customer management, 360 overview, and staff administration")
public class UserController {

    private final CustomerService customerService;
    private final AuthService authService;
    private final UserDao userDao;

    public UserController(CustomerService customerService, AuthService authService, UserDao userDao) {
        this.customerService = customerService;
        this.authService = authService;
        this.userDao = userDao;
    }

    @GetMapping("/customers")
    @Operation(summary = "List all customers")
    public ResponseEntity<List<User>> getCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/customers/{id}")
    @Operation(summary = "Get single customer by id")
    public ResponseEntity<User> getCustomerById(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @PostMapping("/customers")
    @Operation(summary = "Create customer record")
    public ResponseEntity<User> createCustomer(@RequestBody User user, Authentication authentication) {
        UUID currentUserId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(customerService.createCustomer(user, currentUserId));
    }

    @PutMapping("/customers/{id}")
    @Operation(summary = "Update customer record")
    public ResponseEntity<User> updateCustomer(@PathVariable UUID id, @RequestBody User user, Authentication authentication) {
        UUID currentUserId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(customerService.updateCustomer(id, user, currentUserId));
    }

    @GetMapping("/customers/{id}/360")
    @Operation(summary = "Get customer 360 overview (subscriptions, invoices, profile)")
    public ResponseEntity<Map<String, Object>> getCustomer360(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomer360(id));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all staff members (Admin only)")
    public ResponseEntity<List<User>> getStaff() {
        return ResponseEntity.ok(customerService.getAllStaff());
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add new staff member (Admin only)")
    public ResponseEntity<?> createStaff(@Valid @RequestBody RegisterRequest req, Authentication authentication) {
        req.setRole("staff");
        return ResponseEntity.ok(authService.register(req, "admin"));
    }

    private UUID getUserIdFromAuth(Authentication auth) {
        if (auth == null) return null;
        return userDao.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
