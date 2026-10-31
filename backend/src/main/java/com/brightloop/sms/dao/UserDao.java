package com.brightloop.sms.dao;

import com.brightloop.sms.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserDao {

    private final JdbcTemplate jdbcTemplate;

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (ResultSet rs, int rowNum) -> {
        User user = new User();
        user.setId(rs.getObject("id", UUID.class));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setIsActive(rs.getBoolean("is_active"));
        user.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        user.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        return user;
    };

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        List<User> list = jdbcTemplate.query(sql, userRowMapper, email);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<User> findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> list = jdbcTemplate.query(sql, userRowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public List<User> findByRole(String role) {
        String sql = "SELECT * FROM users WHERE role = ?::user_role ORDER BY full_name ASC";
        return jdbcTemplate.query(sql, userRowMapper, role);
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public User create(User user) {
        UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
        String sql = "INSERT INTO users (id, email, password_hash, role, full_name, phone, is_active) " +
                     "VALUES (?, ?, ?, ?::user_role, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(
            sql,
            userRowMapper,
            id,
            user.getEmail(),
            user.getPasswordHash(),
            user.getRole(),
            user.getFullName(),
            user.getPhone(),
            user.getIsActive() != null ? user.getIsActive() : true
        );
    }

    public void update(User user) {
        String sql = "UPDATE users SET full_name = ?, phone = ?, is_active = ?, role = ?::user_role WHERE id = ?";
        jdbcTemplate.update(sql, user.getFullName(), user.getPhone(), user.getIsActive(), user.getRole(), user.getId());
    }

    public void updatePassword(UUID userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        jdbcTemplate.update(sql, passwordHash, userId);
    }

    public long countByRole(String role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?::user_role";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, role);
        return count != null ? count : 0L;
    }
}
