package com.brightloop.auth.dao;

import com.brightloop.auth.model.AppUser;
import com.brightloop.auth.model.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserDao {
    private final JdbcTemplate jdbc;

    private final RowMapper<AppUser> mapper = (rs, rowNum) -> {
        Timestamp expiry = rs.getTimestamp("reset_token_expiry");
        UUID id = rs.getObject("id", UUID.class);
        String roleStr = rs.getString("role");
        Role role = roleStr != null ? Role.valueOf(roleStr.toUpperCase()) : Role.CUSTOMER;
        boolean enabled = rs.getBoolean("is_active");
        if (rs.wasNull()) {
            enabled = true;
        }
        return new AppUser(
                id, rs.getString("full_name"), rs.getString("email"),
                rs.getString("password_hash"), role,
                enabled, rs.getString("reset_token_hash"),
                expiry == null ? null : expiry.toLocalDateTime()
        );
    };

    public UserDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<AppUser> findByEmail(String email) {
        return jdbc.query("SELECT * FROM users WHERE lower(email)=lower(?)",
                mapper, email).stream().findFirst();
    }

    public Optional<AppUser> findByResetTokenHash(String hash) {
        return jdbc.query("SELECT * FROM users WHERE reset_token_hash=?",
                mapper, hash).stream().findFirst();
    }

    public UUID insert(String name, String email, String passwordHash, Role role) {
        return jdbc.queryForObject(
                "INSERT INTO users(full_name, email, password_hash, role) VALUES(?, ?, ?, ?::user_role) RETURNING id",
                UUID.class, name, email.toLowerCase(), passwordHash, role.name().toLowerCase());
    }

    public void saveResetToken(UUID userId, String tokenHash, LocalDateTime expiry) {
        jdbc.update("UPDATE users SET reset_token_hash=?, reset_token_expiry=? WHERE id=?",
                tokenHash, Timestamp.valueOf(expiry), userId);
    }

    public void updatePasswordAndClearResetToken(UUID userId, String passwordHash) {
        jdbc.update("""
                UPDATE users SET password_hash=?, reset_token_hash=NULL,
                reset_token_expiry=NULL WHERE id=?
                """, passwordHash, userId);
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE lower(email)=lower(?)", Integer.class, email);
        return count != null && count > 0;
    }
}