package com.brightloop.sms.dao;

import com.brightloop.sms.model.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class NotificationDao {

    private final JdbcTemplate jdbcTemplate;

    public NotificationDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Notification> notificationRowMapper = (ResultSet rs, int rowNum) -> {
        Notification n = new Notification();
        n.setId(rs.getObject("id", UUID.class));
        n.setUserId(rs.getObject("user_id", UUID.class));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setIsRead(rs.getBoolean("is_read"));
        n.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return n;
    };

    public List<Notification> findByUserId(UUID userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? OR user_id IS NULL ORDER BY created_at DESC LIMIT 50";
        return jdbcTemplate.query(sql, notificationRowMapper, userId);
    }

    public List<Notification> findAll() {
        String sql = "SELECT * FROM notifications ORDER BY created_at DESC LIMIT 50";
        return jdbcTemplate.query(sql, notificationRowMapper);
    }

    public void create(Notification n) {
        UUID id = n.getId() != null ? n.getId() : UUID.randomUUID();
        String sql = "INSERT INTO notifications (id, user_id, title, message, type, is_read) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, id, n.getUserId(), n.getTitle(), n.getMessage(), n.getType(), n.getIsRead() != null ? n.getIsRead() : false);
    }

    public void markAsRead(UUID id) {
        String sql = "UPDATE notifications SET is_read = true WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void markAllAsRead(UUID userId) {
        String sql = "UPDATE notifications SET is_read = true WHERE user_id = ? OR user_id IS NULL";
        jdbcTemplate.update(sql, userId);
    }
}
