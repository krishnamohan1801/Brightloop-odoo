package com.brightloop.sms.dao;

import com.brightloop.sms.model.AuditLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class AuditLogDao {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AuditLog> auditLogRowMapper = (ResultSet rs, int rowNum) -> {
        AuditLog a = new AuditLog();
        a.setId(rs.getObject("id", UUID.class));
        a.setUserId(rs.getObject("user_id", UUID.class));
        a.setAction(rs.getString("action"));
        a.setEntityType(rs.getString("entity_type"));
        a.setEntityId(rs.getString("entity_id"));
        a.setDetails(rs.getString("details"));
        a.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        try { a.setUserEmail(rs.getString("user_email")); } catch (Exception ignored) {}
        return a;
    };

    public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT a.*, u.email AS user_email FROM audit_logs a LEFT JOIN users u ON a.user_id = u.id ORDER BY a.created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, auditLogRowMapper, limit);
    }

    public void log(UUID userId, String action, String entityType, String entityId, String details) {
        String sql = "INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, UUID.randomUUID(), userId, action, entityType, entityId, details);
    }
}
