package com.brightloop.auth.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppUser(
        UUID id,
        String fullName,
        String email,
        String passwordHash,
        Role role,
        boolean enabled,
        String resetTokenHash,
        LocalDateTime resetTokenExpiry
) {}