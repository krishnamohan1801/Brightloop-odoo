package com.brightloop.sms.service;

import com.brightloop.sms.dao.AuditLogDao;
import com.brightloop.sms.dao.PlanDao;
import com.brightloop.sms.model.Plan;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PlanService {

    private final PlanDao planDao;
    private final AuditLogDao auditLogDao;

    public PlanService(PlanDao planDao, AuditLogDao auditLogDao) {
        this.planDao = planDao;
        this.auditLogDao = auditLogDao;
    }

    public List<Plan> getAllPlans(Boolean activeOnly) {
        return planDao.findAll(activeOnly);
    }

    public Plan getPlanById(UUID id) {
        return planDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Billing plan not found with id: " + id));
    }

    public Plan createPlan(Plan plan, UUID userId) {
        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Plan price cannot be negative");
        }
        if (plan.getStartDate() == null) {
            plan.setStartDate(java.time.LocalDate.now());
        }
        Plan created = planDao.create(plan);
        auditLogDao.log(userId, "PLAN_CREATE", "plans", created.getId().toString(), "Created plan: " + created.getName() + " (" + created.getBillingInterval() + ")");
        return created;
    }

    public Plan updatePlan(UUID id, Plan plan, UUID userId) {
        plan.setId(id);
        planDao.update(plan);
        auditLogDao.log(userId, "PLAN_UPDATE", "plans", id.toString(), "Updated plan: " + plan.getName());
        return getPlanById(id);
    }

    public void setPlanActive(UUID id, boolean isActive, UUID userId) {
        planDao.setArchive(id, isActive);
        auditLogDao.log(userId, isActive ? "PLAN_RESTORE" : "PLAN_ARCHIVE", "plans", id.toString(), "Changed plan active status to: " + isActive);
    }
}
