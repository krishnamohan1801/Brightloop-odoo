package com.brightloop.service;

import com.brightloop.dao.PlanDAO;
import com.brightloop.dto.PlanRequest;
import com.brightloop.model.Plan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PlanService {

    private final PlanDAO planDAO;

    public PlanService(PlanDAO planDAO) {
        this.planDAO = planDAO;
    }

    // CREATE PLAN
    public UUID create(PlanRequest request) {

        validateDates(request);

        Plan plan = mapRequestToPlan(request);

        return planDAO.create(plan);
    }

    // GET ALL ACTIVE PLANS
    public List<Plan> getAll() {
        return planDAO.findAll();
    }

    // GET PLAN BY ID
    public Plan getById(UUID id) {

        Plan plan = planDAO.findById(id);

        if (plan == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Plan not found"
            );
        }

        return plan;
    }

    // UPDATE PLAN
    public void update(UUID id, PlanRequest request) {

        validateDates(request);

        // Ensure the plan exists and is active.
        getById(id);

        Plan plan = mapRequestToPlan(request);

        if (!planDAO.update(id, plan)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Plan not found"
            );
        }
    }

    // ARCHIVE PLAN
    public void archive(UUID id) {

        if (!planDAO.archive(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Plan not found or already archived"
            );
        }
    }

    // VALIDATE PLAN DATES
    private void validateDates(PlanRequest request) {

        if (request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date cannot be earlier than start date"
            );
        }
    }

    // MAP REQUEST DTO TO MODEL
    private Plan mapRequestToPlan(PlanRequest request) {

        Plan plan = new Plan();

        plan.setName(request.getName());
        plan.setBillingInterval(request.getBillingInterval());
        plan.setPrice(request.getPrice());
        plan.setMinQuantity(request.getMinQuantity());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setAutoClose(request.getAutoClose());
        plan.setCanPause(request.getCanPause());

        return plan;
    }
}