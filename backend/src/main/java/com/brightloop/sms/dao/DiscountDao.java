package com.brightloop.sms.dao;

import com.brightloop.sms.model.Discount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DiscountDao {

    private final JdbcTemplate jdbcTemplate;

    public DiscountDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Discount> discountRowMapper = (ResultSet rs, int rowNum) -> {
        Discount d = new Discount();
        d.setId(rs.getObject("id", UUID.class));
        d.setCode(rs.getString("code"));
        d.setDiscountType(rs.getString("discount_type"));
        d.setValue(rs.getBigDecimal("value"));
        d.setMinPurchase(rs.getBigDecimal("min_purchase"));
        d.setMinQuantity(rs.getInt("min_quantity"));
        d.setStartDate(rs.getObject("start_date", LocalDate.class));
        d.setEndDate(rs.getObject("end_date", LocalDate.class));
        d.setMaxUses(rs.getObject("max_uses") != null ? rs.getInt("max_uses") : null);
        d.setUsedCount(rs.getInt("used_count"));
        d.setTargetScope(rs.getString("target_scope"));
        d.setTargetProductId(rs.getObject("target_product_id", UUID.class));
        d.setIsActive(rs.getBoolean("is_active"));
        d.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return d;
    };

    public List<Discount> findAll(Boolean activeOnly) {
        String sql = activeOnly != null && activeOnly
            ? "SELECT * FROM discounts WHERE is_active = true AND end_date >= CURRENT_DATE ORDER BY code ASC"
            : "SELECT * FROM discounts ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, discountRowMapper);
    }

    public Optional<Discount> findById(UUID id) {
        String sql = "SELECT * FROM discounts WHERE id = ?";
        List<Discount> list = jdbcTemplate.query(sql, discountRowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<Discount> findByCode(String code) {
        String sql = "SELECT * FROM discounts WHERE UPPER(code) = UPPER(?)";
        List<Discount> list = jdbcTemplate.query(sql, discountRowMapper, code);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Discount create(Discount discount) {
        UUID id = discount.getId() != null ? discount.getId() : UUID.randomUUID();
        String sql = "INSERT INTO discounts (id, code, discount_type, value, min_purchase, min_quantity, " +
                     "start_date, end_date, max_uses, used_count, target_scope, target_product_id, is_active) " +
                     "VALUES (?, UPPER(?), ?::discount_type, ?, ?, ?, ?, ?, ?, ?, ?::discount_scope, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(
            sql,
            discountRowMapper,
            id,
            discount.getCode(),
            discount.getDiscountType(),
            discount.getValue(),
            discount.getMinPurchase() != null ? discount.getMinPurchase() : 0.00,
            discount.getMinQuantity() != null ? discount.getMinQuantity() : 0,
            discount.getStartDate(),
            discount.getEndDate(),
            discount.getMaxUses(),
            discount.getUsedCount() != null ? discount.getUsedCount() : 0,
            discount.getTargetScope() != null ? discount.getTargetScope() : "subscription",
            discount.getTargetProductId(),
            discount.getIsActive() != null ? discount.getIsActive() : true
        );
    }

    public void update(Discount discount) {
        String sql = "UPDATE discounts SET code = UPPER(?), discount_type = ?::discount_type, value = ?, " +
                     "min_purchase = ?, min_quantity = ?, start_date = ?, end_date = ?, max_uses = ?, " +
                     "target_scope = ?::discount_scope, target_product_id = ?, is_active = ? WHERE id = ?";
        jdbcTemplate.update(
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
            discount.getIsActive(),
            discount.getId()
        );
    }

    public void incrementUsedCount(UUID id) {
        String sql = "UPDATE discounts SET used_count = used_count + 1 WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void setArchive(UUID id, boolean isActive) {
        String sql = "UPDATE discounts SET is_active = ? WHERE id = ?";
        jdbcTemplate.update(sql, isActive, id);
    }
}
