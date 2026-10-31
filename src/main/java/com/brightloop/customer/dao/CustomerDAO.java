package com.brightloop.customer.dao;

import com.brightloop.customer.model.Customer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CustomerDAO {

    private final JdbcTemplate jdbcTemplate;

    public CustomerDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Customer> customerRowMapper = (rs, rowNum) -> {

        Customer customer = new Customer();

        customer.setId(rs.getObject("id", UUID.class));
        customer.setEmail(rs.getString("email"));
        customer.setRole(rs.getString("role"));
        customer.setFullName(rs.getString("full_name"));
        customer.setPhone(rs.getString("phone"));
        customer.setActive(rs.getBoolean("is_active"));

        customer.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        customer.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));

        return customer;
    };

    private static final String SELECT_COLUMNS = """
            SELECT id, email, role, full_name, phone,
                   is_active, created_at, updated_at
            FROM users
            """;

    public UUID save(Customer customer, String passwordHash) {

        String sql = """
                INSERT INTO users
                (email, password_hash, role, full_name, phone)
                VALUES (?, ?, 'customer'::user_role, ?, ?)
                RETURNING id
                """;

        return jdbcTemplate.queryForObject(
                sql,
                UUID.class,
                customer.getEmail(),
                passwordHash,
                customer.getFullName(),
                customer.getPhone()
        );
    }

    public boolean existsByEmail(String email) {

        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM users
                    WHERE LOWER(email) = LOWER(?)
                )
                """;

        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(sql, Boolean.class, email)
        );
    }

    public boolean existsByEmailAndIdNot(String email, UUID id) {

        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM users
                    WHERE LOWER(email) = LOWER(?)
                    AND id <> ?
                )
                """;

        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(sql, Boolean.class, email, id)
        );
    }

    public List<Customer> findAll() {

        String sql = SELECT_COLUMNS + """
                WHERE role = 'customer'
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, customerRowMapper);
    }

    public Optional<Customer> findById(UUID id) {

        String sql = SELECT_COLUMNS + """
                WHERE id = ?
                AND role = 'customer'
                """;

        List<Customer> customers = jdbcTemplate.query(sql, customerRowMapper, id);

        return customers.stream().findFirst();
    }

    public int update(UUID id, Customer customer) {

        String sql = """
                UPDATE users
                SET email = ?,
                    full_name = ?,
                    phone = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                AND role = 'customer'
                """;

        return jdbcTemplate.update(
                sql,
                customer.getEmail(),
                customer.getFullName(),
                customer.getPhone(),
                id
        );
    }

    public int archive(UUID id) {

        String sql = """
                UPDATE users
                SET is_active = FALSE,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                AND role = 'customer'
                """;

        return jdbcTemplate.update(sql, id);
    }
}
