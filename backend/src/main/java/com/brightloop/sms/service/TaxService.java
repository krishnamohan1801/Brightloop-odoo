package com.brightloop.sms.service;

import com.brightloop.sms.dao.AuditLogDao;
import com.brightloop.sms.dao.TaxDao;
import com.brightloop.sms.model.Tax;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class TaxService {

    private final TaxDao taxDao;
    private final AuditLogDao auditLogDao;

    public TaxService(TaxDao taxDao, AuditLogDao auditLogDao) {
        this.taxDao = taxDao;
        this.auditLogDao = auditLogDao;
    }

    public List<Tax> getAllTaxes(Boolean activeOnly) {
        return taxDao.findAll(activeOnly);
    }

    public Tax getTaxById(UUID id) {
        return taxDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tax not found with id: " + id));
    }

    public Tax createTax(Tax tax, UUID userId) {
        if (tax.getRate() == null || tax.getRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tax rate cannot be negative");
        }
        Tax created = taxDao.create(tax);
        auditLogDao.log(userId, "TAX_CREATE", "taxes", created.getId().toString(), "Created tax rule: " + created.getName() + " (" + created.getRate() + "%)");
        return created;
    }

    public Tax updateTax(UUID id, Tax tax, UUID userId) {
        tax.setId(id);
        taxDao.update(tax);
        auditLogDao.log(userId, "TAX_UPDATE", "taxes", id.toString(), "Updated tax rule: " + tax.getName());
        return getTaxById(id);
    }

    public void setTaxActive(UUID id, boolean isActive, UUID userId) {
        taxDao.setArchive(id, isActive);
        auditLogDao.log(userId, isActive ? "TAX_RESTORE" : "TAX_ARCHIVE", "taxes", id.toString(), "Changed tax active status to: " + isActive);
    }

    public BigDecimal calculateTaxAmount(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
