package com.brightloop.sms.service;

import com.brightloop.sms.dto.DashboardMetricsResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardMetricsResponse getMetrics() {
        DashboardMetricsResponse resp = new DashboardMetricsResponse();

        // 1. Subscription & Customer Counts
        Long customers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE role = 'customer'", Long.class);
        resp.setTotalCustomers(customers != null ? customers : 0L);

        Long activeSubs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscriptions WHERE status = 'active'", Long.class);
        resp.setActiveSubscriptions(activeSubs != null ? activeSubs : 0L);

        Long pausedSubs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscriptions WHERE status = 'paused'", Long.class);
        resp.setPausedSubscriptions(pausedSubs != null ? pausedSubs : 0L);

        Long closedSubs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscriptions WHERE status = 'closed'", Long.class);
        resp.setClosedSubscriptions(closedSubs != null ? closedSubs : 0L);

        Long quoteSubs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscriptions WHERE status = 'quotation'", Long.class);
        resp.setQuotationSubscriptions(quoteSubs != null ? quoteSubs : 0L);

        // 2. Invoice Counts
        Long totalInvoices = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoices", Long.class);
        resp.setTotalInvoices(totalInvoices != null ? totalInvoices : 0L);

        Long paidInvoices = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoices WHERE status = 'paid'", Long.class);
        resp.setPaidInvoices(paidInvoices != null ? paidInvoices : 0L);

        Long openInvoices = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoices WHERE status = 'confirmed'", Long.class);
        resp.setOpenInvoices(openInvoices != null ? openInvoices : 0L);

        Long overdueInvoices = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM invoices WHERE status NOT IN ('paid', 'cancelled') AND due_date < CURRENT_DATE",
                Long.class
        );
        resp.setOverdueInvoices(overdueInvoices != null ? overdueInvoices : 0L);

        // 3. Financial Collections & Invoiced Amounts
        BigDecimal totalInvoiced = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0.00) FROM invoices WHERE status != 'cancelled'",
                BigDecimal.class
        );
        resp.setTotalInvoiced(totalInvoiced != null ? totalInvoiced : BigDecimal.ZERO);

        BigDecimal collections = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0.00) FROM payments",
                BigDecimal.class
        );
        resp.setTotalCollections(collections != null ? collections : BigDecimal.ZERO);

        BigDecimal refunds = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0.00) FROM refunds",
                BigDecimal.class
        );
        resp.setTotalRefunds(refunds != null ? refunds : BigDecimal.ZERO);

        BigDecimal netCollections = (collections != null ? collections : BigDecimal.ZERO)
                .subtract(refunds != null ? refunds : BigDecimal.ZERO);
        resp.setNetCollections(netCollections);

        BigDecimal outstanding = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(inv.total_amount) - COALESCE(SUM(p.amount), 0.00), 0.00) " +
                "FROM invoices inv " +
                "LEFT JOIN payments p ON inv.id = p.invoice_id " +
                "WHERE inv.status != 'cancelled' AND inv.status != 'paid'",
                BigDecimal.class
        );
        resp.setOutstandingReceivables(outstanding != null && outstanding.signum() > 0 ? outstanding : BigDecimal.ZERO);

        BigDecimal overdueExp = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(inv.total_amount) - COALESCE(SUM(p.amount), 0.00), 0.00) " +
                "FROM invoices inv " +
                "LEFT JOIN payments p ON inv.id = p.invoice_id " +
                "WHERE inv.status NOT IN ('paid', 'cancelled') AND inv.due_date < CURRENT_DATE",
                BigDecimal.class
        );
        resp.setOverdueExposure(overdueExp != null && overdueExp.signum() > 0 ? overdueExp : BigDecimal.ZERO);

        // 4. MRR & ARR (Calculated strictly from active subscriptions and their plan intervals)
        BigDecimal calculatedMrr = calculateMrr();
        resp.setMrr(calculatedMrr);
        resp.setArr(calculatedMrr.multiply(new BigDecimal("12")));

        // 5. Breakdowns
        resp.setCollectionsByMethod(getCollectionsByMethod());
        resp.setSubscriptionsByPlan(getSubscriptionsByPlan());
        resp.setRevenueTrend(getMonthlyRevenueTrend());
        resp.setRecentTransactions(getRecentTransactions());

        return resp;
    }

    private BigDecimal calculateMrr() {
        String sql =
            "SELECT s.id, p.billing_interval, COALESCE(SUM(si.subtotal), p.price) AS sub_total " +
            "FROM subscriptions s " +
            "JOIN plans p ON s.plan_id = p.id " +
            "LEFT JOIN subscription_items si ON s.id = si.subscription_id " +
            "WHERE s.status = 'active' " +
            "GROUP BY s.id, p.billing_interval, p.price";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        BigDecimal mrr = BigDecimal.ZERO;

        for (Map<String, Object> row : rows) {
            String interval = (String) row.get("billing_interval");
            BigDecimal subtotal = (BigDecimal) row.get("sub_total");
            if (subtotal == null) subtotal = BigDecimal.ZERO;

            BigDecimal normalizedMonthly = switch (interval != null ? interval.toLowerCase() : "monthly") {
                case "yearly" -> subtotal.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
                case "weekly" -> subtotal.multiply(new BigDecimal("52")).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
                case "daily" -> subtotal.multiply(new BigDecimal("30"));
                default -> subtotal; // monthly
            };
            mrr = mrr.add(normalizedMonthly);
        }

        return mrr;
    }

    private List<Map<String, Object>> getCollectionsByMethod() {
        String sql = "SELECT payment_method, COALESCE(SUM(amount), 0.00) AS total_amount, COUNT(*) AS count " +
                     "FROM payments GROUP BY payment_method ORDER BY total_amount DESC";
        return jdbcTemplate.queryForList(sql);
    }

    private List<Map<String, Object>> getSubscriptionsByPlan() {
        String sql = "SELECT p.name AS plan_name, p.billing_interval, COUNT(s.id) AS active_count " +
                     "FROM plans p " +
                     "LEFT JOIN subscriptions s ON p.id = s.plan_id AND s.status = 'active' " +
                     "GROUP BY p.id, p.name, p.billing_interval ORDER BY active_count DESC";
        return jdbcTemplate.queryForList(sql);
    }

    private List<Map<String, Object>> getMonthlyRevenueTrend() {
        String sql = "SELECT TO_CHAR(payment_date, 'YYYY-MM') AS month, COALESCE(SUM(amount), 0.00) AS revenue " +
                     "FROM payments " +
                     "GROUP BY TO_CHAR(payment_date, 'YYYY-MM') " +
                     "ORDER BY month ASC LIMIT 12";
        return jdbcTemplate.queryForList(sql);
    }

    private List<Map<String, Object>> getRecentTransactions() {
        String sql = "SELECT p.id, p.reference_number, p.amount, p.payment_method, p.payment_date, " +
                     "u.full_name AS customer_name, inv.invoice_number " +
                     "FROM payments p " +
                     "JOIN users u ON p.customer_id = u.id " +
                     "JOIN invoices inv ON p.invoice_id = inv.id " +
                     "ORDER BY p.payment_date DESC, p.created_at DESC LIMIT 10";
        return jdbcTemplate.queryForList(sql);
    }
}
