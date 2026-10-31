import React, { useState, useEffect } from 'react';
import api from '../api';
import { useAuth } from '../context/AuthContext';
import {
  TrendingUp,
  DollarSign,
  Repeat,
  AlertTriangle,
  Receipt,
  Users,
  CreditCard,
  Play,
  CheckCircle2,
  Clock,
  ArrowUpRight
} from 'lucide-react';

const Dashboard = () => {
  const { user } = useAuth();
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [billingRunning, setBillingRunning] = useState(false);
  const [billingResult, setBillingResult] = useState(null);

  const fetchMetrics = () => {
    api.get('/dashboard/metrics')
      .then((res) => setMetrics(res.data))
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchMetrics();
  }, []);

  const handleTriggerBilling = async () => {
    setBillingRunning(true);
    setBillingResult(null);
    try {
      const res = await api.post('/billing/run-job');
      setBillingResult(res.data);
      fetchMetrics();
    } catch (err) {
      alert('Billing execution error: ' + (err.response?.data?.message || err.message));
    } finally {
      setBillingRunning(false);
    }
  };

  const formatCurrency = (val) => {
    const num = Number(val || 0);
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(num);
  };

  if (loading) {
    return <div style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>Loading finance metrics...</div>;
  }

  const isCustomer = user?.role === 'customer';

  return (
    <div>
      {/* Top Banner / Action Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: '800', letterSpacing: '-0.02em', color: '#0f172a' }}>
            {isCustomer ? `Welcome back, ${user?.fullName}` : 'Executive Finance Overview'}
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
            {isCustomer
              ? 'Manage your subscriptions, view quotations, and pay recurring invoices.'
              : 'Real-time MRR, recurring subscription lifecycle, collections, and automated billing metrics.'}
          </p>
        </div>

        {!isCustomer && (
          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              onClick={handleTriggerBilling}
              disabled={billingRunning}
              className="btn btn-primary"
              style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <Play size={16} />
              <span>{billingRunning ? 'Processing Billing...' : 'Trigger Billing Job Now'}</span>
            </button>
          </div>
        )}
      </div>

      {/* Scheduler execution alert */}
      {billingResult && (
        <div style={{ background: '#ecfdf5', border: '1px solid #a7f3d0', padding: '14px 18px', borderRadius: '10px', color: '#065f46', fontSize: '0.85rem', marginBottom: '24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <CheckCircle2 size={18} color="#10b981" />
            <span>
              <strong>Billing Scheduler Completed:</strong> Evaluated {billingResult.dueSubscriptionsChecked} due subscription(s). Generated {billingResult.invoicesGenerated} new invoice(s).
            </span>
          </div>
          <button onClick={() => setBillingResult(null)} style={{ background: 'none', border: 'none', color: '#065f46', cursor: 'pointer', fontWeight: '700' }}>✕</button>
        </div>
      )}

      {/* KPI Cards Grid */}
      <div className="metrics-grid">
        <div className="metric-card">
          <div>
            <div className="metric-title">Monthly Recurring (MRR)</div>
            <div className="metric-value" style={{ color: '#4f46e5' }}>{formatCurrency(metrics?.mrr)}</div>
            <div className="metric-subtext">Annual ARR: {formatCurrency(metrics?.arr)}</div>
          </div>
          <div className="metric-icon-box" style={{ background: 'rgba(79, 70, 229, 0.1)', color: '#4f46e5' }}>
            <TrendingUp size={24} />
          </div>
        </div>

        <div className="metric-card">
          <div>
            <div className="metric-title">Net Collections</div>
            <div className="metric-value" style={{ color: '#10b981' }}>{formatCurrency(metrics?.netCollections)}</div>
            <div className="metric-subtext">Gross: {formatCurrency(metrics?.totalCollections)} (Refunds: {formatCurrency(metrics?.totalRefunds)})</div>
          </div>
          <div className="metric-icon-box" style={{ background: 'rgba(16, 185, 129, 0.1)', color: '#10b981' }}>
            <DollarSign size={24} />
          </div>
        </div>

        <div className="metric-card">
          <div>
            <div className="metric-title">Outstanding Receivables</div>
            <div className="metric-value" style={{ color: '#d97706' }}>{formatCurrency(metrics?.outstandingReceivables)}</div>
            <div className="metric-subtext">{metrics?.openInvoices || 0} unpaid confirmed invoice(s)</div>
          </div>
          <div className="metric-icon-box" style={{ background: 'rgba(245, 158, 11, 0.1)', color: '#d97706' }}>
            <Clock size={24} />
          </div>
        </div>

        <div className="metric-card">
          <div>
            <div className="metric-title">Overdue Exposure</div>
            <div className="metric-value" style={{ color: '#ef4444' }}>{formatCurrency(metrics?.overdueExposure)}</div>
            <div className="metric-subtext">{metrics?.overdueInvoices || 0} invoice(s) past due date</div>
          </div>
          <div className="metric-icon-box" style={{ background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444' }}>
            <AlertTriangle size={24} />
          </div>
        </div>
      </div>

      {/* Secondary KPI Row */}
      <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))' }}>
        <div className="metric-card" style={{ padding: '16px 20px' }}>
          <div>
            <div className="metric-title">Active Subscriptions</div>
            <div style={{ fontSize: '1.4rem', fontWeight: '800', marginTop: '4px', color: '#0f172a' }}>{metrics?.activeSubscriptions || 0}</div>
          </div>
          <Repeat size={20} color="#6366f1" />
        </div>

        <div className="metric-card" style={{ padding: '16px 20px' }}>
          <div>
            <div className="metric-title">Total Customers</div>
            <div style={{ fontSize: '1.4rem', fontWeight: '800', marginTop: '4px', color: '#0f172a' }}>{metrics?.totalCustomers || 0}</div>
          </div>
          <Users size={20} color="#0284c7" />
        </div>

        <div className="metric-card" style={{ padding: '16px 20px' }}>
          <div>
            <div className="metric-title">Total Invoices</div>
            <div style={{ fontSize: '1.4rem', fontWeight: '800', marginTop: '4px', color: '#0f172a' }}>{metrics?.totalInvoices || 0}</div>
          </div>
          <Receipt size={20} color="#8b5cf6" />
        </div>

        <div className="metric-card" style={{ padding: '16px 20px' }}>
          <div>
            <div className="metric-title">Paid Invoices</div>
            <div style={{ fontSize: '1.4rem', fontWeight: '800', marginTop: '4px', color: '#10b981' }}>{metrics?.paidInvoices || 0}</div>
          </div>
          <CheckCircle2 size={20} color="#10b981" />
        </div>
      </div>

      {/* Analytical Visual Breakdown */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '24px', marginBottom: '28px' }}>
        {/* Collections by Payment Method */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Collections by Simulated Method</div>
              <div className="card-subtitle">Realized income across dummy payment gateways</div>
            </div>
            <CreditCard size={18} color="#64748b" />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {(!metrics?.collectionsByMethod || metrics.collectionsByMethod.length === 0) ? (
              <p style={{ color: '#64748b', fontSize: '0.85rem' }}>No payment collections recorded yet.</p>
            ) : (
              metrics.collectionsByMethod.map((item, idx) => (
                <div key={idx} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', background: '#f8fafc', borderRadius: '8px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#4f46e5' }}></div>
                    <span style={{ fontWeight: '600', fontSize: '0.88rem', textTransform: 'capitalize' }}>
                      {String(item.payment_method).replace('_', ' ')}
                    </span>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontWeight: '700', fontSize: '0.92rem' }}>{formatCurrency(item.total_amount)}</div>
                    <div style={{ fontSize: '0.72rem', color: '#64748b' }}>{item.count} transaction(s)</div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Subscriptions by Plan */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Active Subscriptions by Plan</div>
              <div className="card-subtitle">Distribution across billing intervals</div>
            </div>
            <Repeat size={18} color="#64748b" />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {(!metrics?.subscriptionsByPlan || metrics.subscriptionsByPlan.length === 0) ? (
              <p style={{ color: '#64748b', fontSize: '0.85rem' }}>No subscription plans found.</p>
            ) : (
              metrics.subscriptionsByPlan.map((plan, idx) => (
                <div key={idx} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', background: '#f8fafc', borderRadius: '8px' }}>
                  <div>
                    <span style={{ fontWeight: '600', fontSize: '0.88rem' }}>{plan.plan_name}</span>
                    <span className="badge badge-secondary" style={{ marginLeft: '8px', fontSize: '0.68rem' }}>
                      {plan.billing_interval}
                    </span>
                  </div>
                  <div style={{ fontWeight: '700', fontSize: '0.95rem', color: '#4f46e5' }}>
                    {plan.active_count} active
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Recent Transactions Table */}
      <div className="card">
        <div className="card-header">
          <div>
            <div className="card-title">Recent Simulated Payment Transactions</div>
            <div className="card-subtitle">Verified against invoice balances</div>
          </div>
        </div>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Reference #</th>
                <th>Customer</th>
                <th>Invoice</th>
                <th>Method</th>
                <th>Amount</th>
                <th>Date</th>
                <th>Type</th>
              </tr>
            </thead>
            <tbody>
              {(!metrics?.recentTransactions || metrics.recentTransactions.length === 0) ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                    No payment transactions recorded yet.
                  </td>
                </tr>
              ) : (
                metrics.recentTransactions.map((tx) => (
                  <tr key={tx.id}>
                    <td style={{ fontFamily: 'monospace', fontWeight: '600' }}>{tx.reference_number}</td>
                    <td>{tx.customer_name}</td>
                    <td style={{ fontWeight: '600', color: '#4f46e5' }}>{tx.invoice_number}</td>
                    <td style={{ textTransform: 'capitalize' }}>{String(tx.payment_method).replace('_', ' ')}</td>
                    <td style={{ fontWeight: '700', color: '#10b981' }}>{formatCurrency(tx.amount)}</td>
                    <td style={{ color: '#64748b', fontSize: '0.82rem' }}>{tx.payment_date}</td>
                    <td>
                      <span className="badge badge-success">Simulated Demo</span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
