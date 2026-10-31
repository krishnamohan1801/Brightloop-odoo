package com.brightloop.sms.controller;

import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.model.Plan;
import com.brightloop.sms.model.User;
import com.brightloop.sms.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans")
@Tag(name = "Billing Plans", description = "Billing interval and recurring subscription plan management")
public class PlanController {

    private final PlanService planService;
    private final UserDao userDao;

    public PlanController(PlanService planService, UserDao userDao) {
        this.planService = planService;
        this.userDao = userDao;
    }

    @GetMapping
    @Operation(summary = "List all billing plans")
    public ResponseEntity<List<Plan>> getPlans(@RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(planService.getAllPlans(activeOnly));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single plan by id")
    public ResponseEntity<Plan> getPlanById(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create billing plan (Admin only)")
    public ResponseEntity<Plan> createPlan(@RequestBody Plan plan, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(planService.createPlan(plan, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update billing plan (Admin only)")
    public ResponseEntity<Plan> updatePlan(@PathVariable UUID id, @RequestBody Plan plan, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(planService.updatePlan(id, plan, userId));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archive or restore billing plan (Admin only)")
    public ResponseEntity<Void> archivePlan(@PathVariable UUID id, @RequestParam boolean active, Authentication authentication) {
        UUID userId = getUserId(authentication);
        planService.setPlanActive(id, active, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getUserId(Authentication auth) {
        if (auth == null) return null;
        return userDao.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
