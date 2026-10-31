package com.brightloop.sms.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PasswordResetTokenDao {

    private final JdbcTemplate jdbcTemplate;

    public PasswordResetTokenDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createToken(UUID userId, String token, OffsetDateTime expiryDate) {
        String sql = "INSERT INTO password_reset_tokens (id, user_id, token, expiry_date, used) VALUES (?, ?, ?, ?, false)";
        jdbcTemplate.update(sql, UUID.randomUUID(), userId, token, Timestamp.from(expiryDate.toInstant()));
    }

    public Optional<Map<String, Object>> findByToken(String token) {
        String sql = "SELECT * FROM password_reset_tokens WHERE token = ?";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, token);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public void markAsUsed(String token) {
        String sql = "UPDATE password_reset_tokens SET used = true WHERE token = ?";
        jdbcTemplate.update(sql, token);
    }
}
