package com.brightloop.sms.controller;

import com.brightloop.sms.dao.AuditLogDao;
import com.brightloop.sms.dto.DashboardMetricsResponse;
import com.brightloop.sms.model.AuditLog;
import com.brightloop.sms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Finance Dashboard & Analytics", description = "MRR, ARR, collections, overdue exposure, and business metrics")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuditLogDao auditLogDao;

    public DashboardController(DashboardService dashboardService, AuditLogDao auditLogDao) {
        this.dashboardService = dashboardService;
        this.auditLogDao = auditLogDao;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get high-level executive and financial metrics")
    public ResponseEntity<DashboardMetricsResponse> getMetrics() {
        return ResponseEntity.ok(dashboardService.getMetrics());
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get recent system activity and audit history")
    public ResponseEntity<List<AuditLog>> getAuditLogs(@RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(auditLogDao.findRecent(limit));
    }
}
