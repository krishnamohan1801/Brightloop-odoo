package com.brightloop.discount.service;

import com.brightloop.discount.dao.DiscountDAO;
import com.brightloop.discount.model.Discount;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DiscountService {

    private final DiscountDAO discountDAO;

    public DiscountService(DiscountDAO discountDAO) {
        this.discountDAO = discountDAO;
    }

    @Transactional
    public Discount createDiscount(Discount discount) {
        if (discount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount payload is required");
        }

        normalizeDiscount(discount);
        validateDiscount(discount);

        if (discountDAO.existsByCode(discount.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Discount code already exists");
        }

        if (discount.getActive() == null) {
            discount.setActive(true);
        }

        UUID id = discountDAO.save(discount);
        return getDiscountById(id);
    }

    public List<Discount> getAllDiscounts() {
        return discountDAO.findAll();
    }

    public Discount getDiscountById(UUID id) {
        return discountDAO.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discount not found"));
    }

    public Discount getDiscountByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount code is required");
        }

        return discountDAO.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discount code not found"));
    }

    @Transactional
    public Discount updateDiscount(UUID id, Discount discount) {
        getDiscountById(id);

        normalizeDiscount(discount);
        validateDiscount(discount);

        if (discountDAO.existsByCodeAndIdNot(discount.getCode(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Discount code already exists");
        }

        discountDAO.update(id, discount);
        return getDiscountById(id);
    }

    @Transactional
    public Discount updateActiveStatus(UUID id, boolean active) {
        Discount existing = getDiscountById(id);

        if (active && existing.getMaxUses() != null
                && existing.getUsedCount() != null
                && existing.getUsedCount() >= existing.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Discount has reached its maximum usage limit");
        }

        discountDAO.updateActiveStatus(id, active);
        return getDiscountById(id);
    }

    public Discount validateDiscountCode(String code) {
        Discount discount = getDiscountByCode(code);

        if (!Boolean.TRUE.equals(discount.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount is inactive");
        }

        LocalDate today = LocalDate.now();

        if (discount.getStartDate() != null && today.isBefore(discount.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount is not active yet");
        }

        if (discount.getEndDate() != null && today.isAfter(discount.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount has expired");
        }

        if (discount.getMaxUses() != null
                && discount.getUsedCount() != null
                && discount.getUsedCount() >= discount.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount usage limit reached");
        }

        return discount;
    }

    private void normalizeDiscount(Discount discount) {
        if (discount.getCode() != null) {
            discount.setCode(discount.getCode().trim().toUpperCase(Locale.ROOT));
        }

        if (discount.getDiscountType() != null) {
            discount.setDiscountType(discount.getDiscountType().trim().toLowerCase(Locale.ROOT));
        }

        if (discount.getTargetScope() != null) {
            discount.setTargetScope(discount.getTargetScope().trim().toLowerCase(Locale.ROOT));
        }
    }

    private void validateDiscount(Discount discount) {
        if (discount.getCode() == null || discount.getCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount code is required");
        }

        if (discount.getDiscountType() == null
                || (!"fixed".equals(discount.getDiscountType()) && !"percentage".equals(discount.getDiscountType()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount type must be fixed or percentage");
        }

        if (discount.getValue() == null || discount.getValue().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount value must be greater than zero");
        }

        if ("percentage".equals(discount.getDiscountType())
                && discount.getValue().compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percentage discount cannot exceed 100");
        }

        if (discount.getTargetScope() == null
                || (!"subscription".equals(discount.getTargetScope()) && !"product".equals(discount.getTargetScope()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount scope must be subscription or product");
        }

        if ("product".equals(discount.getTargetScope()) && discount.getTargetProductId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product ID is required for product-specific discounts");
        }

        if ("subscription".equals(discount.getTargetScope()) && discount.getTargetProductId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription-wide discounts should not have a target product");
        }

        if (discount.getMinPurchase() != null && discount.getMinPurchase().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum purchase cannot be negative");
        }

        if (discount.getMinQuantity() != null && discount.getMinQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum quantity must be greater than zero");
        }

        if (discount.getMaxUses() != null && discount.getMaxUses() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum uses must be greater than zero");
        }

        if (discount.getStartDate() != null
                && discount.getEndDate() != null
                && discount.getEndDate().isBefore(discount.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
    }
}
