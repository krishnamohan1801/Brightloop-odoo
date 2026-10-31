package com.brightloop.auth.service;

import com.brightloop.auth.dao.UserDao;
import com.brightloop.auth.dto.AuthDtos.*;
import com.brightloop.auth.model.*;
import com.brightloop.auth.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {
    private final UserDao userDao;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public AuthService(UserDao userDao, PasswordEncoder encoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       EmailService emailService) {
        this.userDao = userDao;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @Transactional
    public ApiMessage registerCustomer(RegisterRequest request) {
        if (userDao.existsByEmail(request.email()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        try {
            // Public registration is always CUSTOMER. Never accept a role from the request.
            userDao.insert(request.fullName().trim(), request.email().trim().toLowerCase(),
                    encoder.encode(request.password()), Role.CUSTOMER);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }
        return new ApiMessage("Customer account created successfully.");
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        AppUser user = userDao.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        UserDetails principal = User.withUsername(user.email()).password(user.passwordHash())
                .authorities("ROLE_" + user.role().name()).build();
        String jwt = jwtService.generateToken(principal, user.role().name());
        return new AuthResponse(jwt, "Bearer", jwtService.getExpirationMs(),
                user.email(), user.fullName(), user.role().name());
    }

    @Transactional
    public ApiMessage sendResetLink(String email) {
        userDao.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString();
            String tokenHash = sha256(rawToken);
            userDao.saveResetToken(user.id(), tokenHash, LocalDateTime.now().plusMinutes(15));
            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.email(), resetLink);
        });
        // Do not reveal whether the email address exists.
        return new ApiMessage("If an account exists for that email, a password reset link has been sent.");
    }

    @Transactional
    public ApiMessage resetPassword(ResetPasswordRequest request) {
        AppUser user = userDao.findByResetTokenHash(sha256(request.token()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid or expired password reset link."));
        if (user.resetTokenExpiry() == null || user.resetTokenExpiry().isBefore(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired password reset link.");

        userDao.updatePasswordAndClearResetToken(user.id(), encoder.encode(request.newPassword()));
        return new ApiMessage("Password has been reset. Please log in again.");
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }
}
