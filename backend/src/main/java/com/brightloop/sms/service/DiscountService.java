package com.brightloop.sms.service;

import com.brightloop.sms.dao.AuditLogDao;
import com.brightloop.sms.dao.DiscountDao;
import com.brightloop.sms.model.Discount;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DiscountService {

    private final DiscountDao discountDao;
    private final AuditLogDao auditLogDao;

    public DiscountService(DiscountDao discountDao, AuditLogDao auditLogDao) {
        this.discountDao = discountDao;
        this.auditLogDao = auditLogDao;
    }

    public List<Discount> getAllDiscounts(Boolean activeOnly) {
        return discountDao.findAll(activeOnly);
    }

    public Discount getDiscountById(UUID id) {
        return discountDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discount not found with id: " + id));
    }

    public Optional<Discount> findByCode(String code) {
        return discountDao.findByCode(code);
    }

    public Discount createDiscount(Discount discount, UUID userId) {
        if (discount.getCode() == null || discount.getCode().isBlank()) {
            throw new IllegalArgumentException("Discount code is required");
        }
        if (discount.getValue() == null || discount.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than zero");
        }
        if (discount.getStartDate() == null) {
            discount.setStartDate(LocalDate.now());
        }
        if (discount.getEndDate() == null) {
            discount.setEndDate(LocalDate.now().plusMonths(6));
        }
        Discount created = discountDao.create(discount);
        auditLogDao.log(userId, "DISCOUNT_CREATE", "discounts", created.getId().toString(), "Created discount rule: " + created.getCode() + " (" + created.getValue() + " " + created.getDiscountType() + ")");
        return created;
    }

    public Discount updateDiscount(UUID id, Discount discount, UUID userId) {
        discount.setId(id);
        discountDao.update(discount);
        auditLogDao.log(userId, "DISCOUNT_UPDATE", "discounts", id.toString(), "Updated discount rule: " + discount.getCode());
        return getDiscountById(id);
    }

    public void setDiscountActive(UUID id, boolean isActive, UUID userId) {
        discountDao.setArchive(id, isActive);
        auditLogDao.log(userId, isActive ? "DISCOUNT_RESTORE" : "DISCOUNT_ARCHIVE", "discounts", id.toString(), "Changed discount active status to: " + isActive);
    }

    /**
     * Evaluates discount eligibility and calculates discount amount.
     */
    public BigDecimal calculateDiscountAmount(Discount discount, BigDecimal purchaseAmount, int totalQuantity, UUID productId) {
        if (discount == null || !discount.getIsActive()) {
            return BigDecimal.ZERO;
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(discount.getStartDate()) || today.isAfter(discount.getEndDate())) {
            return BigDecimal.ZERO;
        }

        if (discount.getMaxUses() != null && discount.getUsedCount() >= discount.getMaxUses()) {
            return BigDecimal.ZERO;
        }

        if (discount.getMinPurchase() != null && purchaseAmount.compareTo(discount.getMinPurchase()) < 0) {
            return BigDecimal.ZERO;
        }

        if (discount.getMinQuantity() != null && totalQuantity < discount.getMinQuantity()) {
            return BigDecimal.ZERO;
        }

        if ("product".equalsIgnoreCase(discount.getTargetScope()) && discount.getTargetProductId() != null) {
            if (productId == null || !discount.getTargetProductId().equals(productId)) {
                return BigDecimal.ZERO;
            }
        }

        BigDecimal discountAmount;
        if ("percentage".equalsIgnoreCase(discount.getDiscountType())) {
            discountAmount = purchaseAmount.multiply(discount.getValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            discountAmount = discount.getValue();
        }

        // Discount cannot exceed the total amount
        if (discountAmount.compareTo(purchaseAmount) > 0) {
            return purchaseAmount;
        }

        return discountAmount;
    }
}
