package com.brightloop.controller;

import com.brightloop.dto.PlanRequest;
import com.brightloop.model.Plan;
import com.brightloop.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    // CREATE PLAN
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UUID create(@Valid @RequestBody PlanRequest request) {

        return planService.create(request);
    }

    // GET ALL ACTIVE PLANS
    @GetMapping
    public List<Plan> getAll() {

        return planService.getAll();
    }

    // GET PLAN BY ID
    @GetMapping("/{id}")
    public Plan getById(@PathVariable UUID id) {

        return planService.getById(id);
    }

    // UPDATE PLAN
    @PutMapping("/{id}")
    public String update(
            @PathVariable UUID id,
            @Valid @RequestBody PlanRequest request) {

        planService.update(id, request);

        return "Plan updated successfully";
    }

    // ARCHIVE PLAN
    @PatchMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {

        planService.archive(id);
    }
}