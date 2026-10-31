package com.brightloop.discount.dao;

import com.brightloop.discount.model.Discount;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DiscountDAO {

    private final JdbcTemplate jdbcTemplate;

    public DiscountDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Discount> rowMapper = (rs, rowNum) -> {

        Discount discount = new Discount();

        discount.setId(rs.getObject("id", UUID.class));
        discount.setCode(rs.getString("code"));
        discount.setDiscountType(rs.getString("discount_type"));
        discount.setValue(rs.getBigDecimal("value"));
        discount.setMinPurchase(rs.getBigDecimal("min_purchase"));
        discount.setMinQuantity(rs.getObject("min_quantity", Integer.class));
        discount.setStartDate(rs.getObject("start_date", java.time.LocalDate.class));
        discount.setEndDate(rs.getObject("end_date", java.time.LocalDate.class));
        discount.setMaxUses(rs.getObject("max_uses", Integer.class));
        discount.setUsedCount(rs.getObject("used_count", Integer.class));
        discount.setTargetScope(rs.getString("target_scope"));
        discount.setTargetProductId(rs.getObject("target_product_id", UUID.class));
        discount.setActive(rs.getObject("is_active", Boolean.class));
        discount.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));

        return discount;
    };

    public UUID save(Discount discount) {

        String sql = """
                INSERT INTO discounts (
                    code,
                    discount_type,
                    value,
                    min_purchase,
                    min_quantity,
                    start_date,
                    end_date,
                    max_uses,
                    target_scope,
                    target_product_id,
                    is_active
                )
                VALUES (
                    ?, ?::discount_type, ?, ?, ?, ?, ?, ?, ?::discount_scope, ?, ?
                )
                RETURNING id
                """;

        return jdbcTemplate.queryForObject(
                sql,
                UUID.class,
                discount.getCode(),
                discount.getDiscountType(),
                discount.getValue(),
                discount.getMinPurchase(),
                discount.getMinQuantity(),
                discount.getStartDate(),
                discount.getEndDate(),
                discount.getMaxUses(),
                discount.getTargetScope(),
                discount.getTargetProductId(),
                discount.getActive() == null ? true : discount.getActive()
        );
    }

    public List<Discount> findAll() {
        String sql = """
                SELECT *
                FROM discounts
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Discount> findById(UUID id) {
        String sql = """
                SELECT *
                FROM discounts
                WHERE id = ?
                """;

        List<Discount> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public Optional<Discount> findByCode(String code) {
        String sql = """
                SELECT *
                FROM discounts
                WHERE LOWER(code) = LOWER(?)
                """;

        List<Discount> results = jdbcTemplate.query(sql, rowMapper, code);
        return results.stream().findFirst();
    }

    public int update(UUID id, Discount discount) {
        String sql = """
                UPDATE discounts
                SET code = ?,
                    discount_type = ?::discount_type,
                    value = ?,
                    min_purchase = ?,
                    min_quantity = ?,
                    start_date = ?,
                    end_date = ?,
                    max_uses = ?,
                    target_scope = ?::discount_scope,
                    target_product_id = ?,
                    is_active = ?
                WHERE id = ?
                """;

        return jdbcTemplate.update(
                sql,
                discount.getCode(),
                discount.getDiscountType(),
                discount.getValue(),
                discount.getMinPurchase(),
                discount.getMinQuantity(),
                discount.getStartDate(),
                discount.getEndDate(),
                discount.getMaxUses(),
                discount.getTargetScope(),
                discount.getTargetProductId(),
                discount.getActive(),
                id
        );
    }

    public int updateActiveStatus(UUID id, boolean active) {
        String sql = """
                UPDATE discounts
                SET is_active = ?
                WHERE id = ?
                """;

        return jdbcTemplate.update(sql, active, id);
    }

    public boolean existsByCode(String code) {
        String sql = """
                SELECT COUNT(*)
                FROM discounts
                WHERE LOWER(code) = LOWER(?)
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, code);
        return count != null && count > 0;
    }

    public boolean existsByCodeAndIdNot(String code, UUID id) {
        String sql = """
                SELECT COUNT(*)
                FROM discounts
                WHERE LOWER(code) = LOWER(?)
                  AND id <> ?
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, code, id);
        return count != null && count > 0;
    }
}
