package com.brightloop.sms.controller;

import com.brightloop.sms.config.JwtTokenProvider;
import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.dto.*;
import com.brightloop.sms.model.User;
import com.brightloop.sms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication, registration and password recovery")
public class AuthController {

    private final AuthService authService;
    private final UserDao userDao;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthService authService, UserDao userDao, JwtTokenProvider tokenProvider) {
        this.authService = authService;
        this.userDao = userDao;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new account")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            Authentication authentication) {
        String currentRole = authentication != null ? getRoleFromAuth(authentication) : "customer";
        return ResponseEntity.ok(authService.register(request, currentRole));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset link via registered email")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using received reset link token")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Retrieve current authenticated user details")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userDao.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPasswordHash(null);
        return ResponseEntity.ok(user);
    }

    private String getRoleFromAuth(Authentication auth) {
        if (auth == null || auth.getAuthorities().isEmpty()) return "customer";
        String authority = auth.getAuthorities().iterator().next().getAuthority();
        return authority.replace("ROLE_", "").toLowerCase();
    }
}
