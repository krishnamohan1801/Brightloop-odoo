package com.brightloop.sms.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardMetricsResponse {
    // Top-level KPI counts
    private Long totalCustomers;
    private Long activeSubscriptions;
    private Long pausedSubscriptions;
    private Long closedSubscriptions;
    private Long quotationSubscriptions;

    private Long totalInvoices;
    private Long paidInvoices;
    private Long openInvoices;
    private Long overdueInvoices;

    // Financial Metrics
    private BigDecimal mrr; // Monthly Recurring Revenue
    private BigDecimal arr; // Annual Recurring Revenue
    private BigDecimal totalInvoiced;
    private BigDecimal totalCollections;
    private BigDecimal totalRefunds;
    private BigDecimal netCollections;
    private BigDecimal outstandingReceivables;
    private BigDecimal overdueExposure;

    // Breakdowns
    private List<Map<String, Object>> collectionsByMethod;
    private List<Map<String, Object>> subscriptionsByPlan;
    private List<Map<String, Object>> revenueTrend;
    private List<Map<String, Object>> recentTransactions;

    public DashboardMetricsResponse() {}

    public Long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(Long totalCustomers) { this.totalCustomers = totalCustomers; }

    public Long getActiveSubscriptions() { return activeSubscriptions; }
    public void setActiveSubscriptions(Long activeSubscriptions) { this.activeSubscriptions = activeSubscriptions; }

    public Long getPausedSubscriptions() { return pausedSubscriptions; }
    public void setPausedSubscriptions(Long pausedSubscriptions) { this.pausedSubscriptions = pausedSubscriptions; }

    public Long getClosedSubscriptions() { return closedSubscriptions; }
    public void setClosedSubscriptions(Long closedSubscriptions) { this.closedSubscriptions = closedSubscriptions; }

    public Long getQuotationSubscriptions() { return quotationSubscriptions; }
    public void setQuotationSubscriptions(Long quotationSubscriptions) { this.quotationSubscriptions = quotationSubscriptions; }

    public Long getTotalInvoices() { return totalInvoices; }
    public void setTotalInvoices(Long totalInvoices) { this.totalInvoices = totalInvoices; }

    public Long getPaidInvoices() { return paidInvoices; }
    public void setPaidInvoices(Long paidInvoices) { this.paidInvoices = paidInvoices; }

    public Long getOpenInvoices() { return openInvoices; }
    public void setOpenInvoices(Long openInvoices) { this.openInvoices = openInvoices; }

    public Long getOverdueInvoices() { return overdueInvoices; }
    public void setOverdueInvoices(Long overdueInvoices) { this.overdueInvoices = overdueInvoices; }

    public BigDecimal getMrr() { return mrr; }
    public void setMrr(BigDecimal mrr) { this.mrr = mrr; }

    public BigDecimal getArr() { return arr; }
    public void setArr(BigDecimal arr) { this.arr = arr; }

    public BigDecimal getTotalInvoiced() { return totalInvoiced; }
    public void setTotalInvoiced(BigDecimal totalInvoiced) { this.totalInvoiced = totalInvoiced; }

    public BigDecimal getTotalCollections() { return totalCollections; }
    public void setTotalCollections(BigDecimal totalCollections) { this.totalCollections = totalCollections; }

    public BigDecimal getTotalRefunds() { return totalRefunds; }
    public void setTotalRefunds(BigDecimal totalRefunds) { this.totalRefunds = totalRefunds; }

    public BigDecimal getNetCollections() { return netCollections; }
    public void setNetCollections(BigDecimal netCollections) { this.netCollections = netCollections; }

    public BigDecimal getOutstandingReceivables() { return outstandingReceivables; }
    public void setOutstandingReceivables(BigDecimal outstandingReceivables) { this.outstandingReceivables = outstandingReceivables; }

    public BigDecimal getOverdueExposure() { return overdueExposure; }
    public void setOverdueExposure(BigDecimal overdueExposure) { this.overdueExposure = overdueExposure; }

    public List<Map<String, Object>> getCollectionsByMethod() { return collectionsByMethod; }
    public void setCollectionsByMethod(List<Map<String, Object>> collectionsByMethod) { this.collectionsByMethod = collectionsByMethod; }

    public List<Map<String, Object>> getSubscriptionsByPlan() { return subscriptionsByPlan; }
    public void setSubscriptionsByPlan(List<Map<String, Object>> subscriptionsByPlan) { this.subscriptionsByPlan = subscriptionsByPlan; }

    public List<Map<String, Object>> getRevenueTrend() { return revenueTrend; }
    public void setRevenueTrend(List<Map<String, Object>> revenueTrend) { this.revenueTrend = revenueTrend; }

    public List<Map<String, Object>> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<Map<String, Object>> recentTransactions) { this.recentTransactions = recentTransactions; }
}
