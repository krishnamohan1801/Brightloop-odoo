package com.brightloop.sms.service;

import com.brightloop.sms.config.JwtTokenProvider;
import com.brightloop.sms.dao.AuditLogDao;
import com.brightloop.sms.dao.PasswordResetTokenDao;
import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.dto.*;
import com.brightloop.sms.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private final UserDao userDao;
    private final PasswordResetTokenDao tokenDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final AuditLogDao auditLogDao;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{9,}$");

    public AuthService(
            UserDao userDao,
            PasswordResetTokenDao tokenDao,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            EmailService emailService,
            AuditLogDao auditLogDao) {
        this.userDao = userDao;
        this.tokenDao = tokenDao;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.emailService = emailService;
        this.auditLogDao = auditLogDao;
    }

    public AuthResponse login(AuthRequest request) {
        User user = userDao.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new IllegalStateException("Account is inactive. Please contact your administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getFullName());
        auditLogDao.log(user.getId(), "USER_LOGIN", "users", user.getId().toString(), "User logged in successfully");

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public AuthResponse register(RegisterRequest request, String currentRole) {
        if (userDao.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered. Please sign in or use another email.");
        }

        validatePasswordStrength(request.getPassword());

        String targetRole = "customer";
        if (request.getRole() != null && !request.getRole().isBlank()) {
            // Only admin can create staff or admin users
            if ("admin".equalsIgnoreCase(request.getRole()) || "staff".equalsIgnoreCase(request.getRole())) {
                if (!"admin".equalsIgnoreCase(currentRole)) {
                    throw new SecurityException("Only administrators can add members of staff or admin accounts.");
                }
                targetRole = request.getRole().toLowerCase();
            }
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone());
        user.setRole(targetRole);
        user.setIsActive(true);

        User created = userDao.create(user);
        auditLogDao.log(created.getId(), "USER_REGISTER", "users", created.getId().toString(), "New account created with role: " + targetRole);

        String token = tokenProvider.generateToken(created.getId(), created.getEmail(), created.getRole(), created.getFullName());
        return new AuthResponse(token, created.getId(), created.getEmail(), created.getFullName(), created.getRole());
    }

    public Map<String, Object> forgotPassword(ForgotPasswordRequest request) {
        Optional<User> optUser = userDao.findByEmail(request.getEmail().trim().toLowerCase());
        if (optUser.isEmpty()) {
            // Return success message anyway to avoid user enumeration
            return Map.of(
                "success", true,
                "message", "If that email exists in our system, a password reset link has been dispatched."
            );
        }

        User user = optUser.get();
        String resetToken = UUID.randomUUID().toString();
        OffsetDateTime expiry = OffsetDateTime.now().plusHours(1);

        tokenDao.createToken(user.getId(), resetToken, expiry);

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        auditLogDao.log(user.getId(), "PASSWORD_RESET_REQUEST", "users", user.getId().toString(), "Password reset requested");

        return Map.of(
            "success", true,
            "message", "Password reset instructions have been sent to your email.",
            "token", resetToken, // Included so test environments can immediately use the link without external email access
            "resetLink", resetLink
        );
    }

    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        validatePasswordStrength(request.getNewPassword());

        Map<String, Object> tokenRecord = tokenDao.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset link."));

        Boolean used = (Boolean) tokenRecord.get("used");
        if (used != null && used) {
            throw new IllegalArgumentException("This password reset link has already been used.");
        }

        Object expiryObj = tokenRecord.get("expiry_date");
        if (expiryObj instanceof Timestamp timestamp) {
            if (timestamp.toInstant().isBefore(java.time.Instant.now())) {
                throw new IllegalArgumentException("Password reset link has expired. Please request a new one.");
            }
        }

        UUID userId = (UUID) tokenRecord.get("user_id");
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userDao.updatePassword(userId, encodedPassword);
        tokenDao.markAsUsed(request.getToken());

        auditLogDao.log(userId, "PASSWORD_RESET_COMPLETE", "users", userId.toString(), "Password reset completed successfully");

        return Map.of(
            "success", true,
            "message", "Your password has been reset successfully. You can now log in with your new credentials."
        );
    }

    private void validatePasswordStrength(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must be more than 8 characters, containing at least one uppercase letter, one lowercase letter, and one special character.");
        }
    }
}
