import React, { useState, useEffect } from 'react';
import api from '../api';
import { useAuth } from '../context/AuthContext';
import {
  Repeat,
  PauseCircle,
  PlayCircle,
  XCircle,
  Eye,
  Calendar,
  Layers,
  X,
  AlertCircle,
  CheckCircle2
} from 'lucide-react';

const Subscriptions = () => {
  const { user } = useAuth();
  const isCustomer = user?.role === 'customer';

  const [subscriptions, setSubscriptions] = useState([]);
  const [filterStatus, setFilterStatus] = useState('');
  const [loading, setLoading] = useState(true);
  const [viewingSub, setViewingSub] = useState(null);

  const fetchSubscriptions = async () => {
    try {
      setLoading(true);
      const res = await api.get('/subscriptions', {
        params: { status: filterStatus || undefined },
      });
      setSubscriptions(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubscriptions();
  }, [filterStatus]);

  const handlePause = async (id) => {
    try {
      await api.post(`/subscriptions/${id}/pause`);
      fetchSubscriptions();
      alert('Subscription paused successfully.');
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleResume = async (id) => {
    try {
      await api.post(`/subscriptions/${id}/resume`);
      fetchSubscriptions();
      alert('Subscription resumed to Active status.');
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleClose = async (id) => {
    const reason = window.prompt('Please enter cancellation reason:', 'Customer requested closure');
    if (reason === null) return;
    try {
      await api.post(`/subscriptions/${id}/close?reason=${encodeURIComponent(reason)}`);
      fetchSubscriptions();
      alert('Subscription closed.');
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: '800', letterSpacing: '-0.02em', color: '#0f172a' }}>
            {isCustomer ? 'My Subscriptions' : 'Subscription Lifecycle Management'}
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
            Part 2: Active recurring subscriptions, pause/resume rules, next billing dates, and closure workflows.
          </p>
        </div>

        {/* Status Filter */}
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <span style={{ fontSize: '0.82rem', fontWeight: '600', color: '#64748b' }}>Filter Status:</span>
          <select
            className="form-control"
            style={{ width: '160px', padding: '6px 12px', fontSize: '0.82rem' }}
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
          >
            <option value="">All Statuses</option>
            <option value="active">Active</option>
            <option value="paused">Paused</option>
            <option value="confirmed">Confirmed</option>
            <option value="quotation">Quotation</option>
            <option value="closed">Closed</option>
          </select>
        </div>
      </div>

      {/* Subscriptions Table */}
      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Subscription #</th>
                <th>Customer</th>
                <th>Plan Name</th>
                <th>Interval</th>
                <th>Next Billing Date</th>
                <th>Status</th>
                <th>Lifecycle Actions</th>
              </tr>
            </thead>
            <tbody>
              {subscriptions.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                    No subscriptions found matching criteria.
                  </td>
                </tr>
              ) : (
                subscriptions.map((s) => (
                  <tr key={s.id}>
                    <td style={{ fontFamily: 'monospace', fontWeight: '700', color: '#0f172a' }}>
                      {s.subscriptionNumber}
                    </td>
                    <td>
                      <div style={{ fontWeight: '600' }}>{s.customerName}</div>
                      <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{s.customerEmail}</div>
                    </td>
                    <td>
                      <span className="badge badge-secondary">{s.planName}</span>
                    </td>
                    <td style={{ textTransform: 'capitalize' }}>{s.billingInterval}</td>
                    <td style={{ fontWeight: '600', color: '#4f46e5' }}>{s.nextBillingDate}</td>
                    <td>
                      <span className={`badge badge-${s.status === 'active' ? 'active' : s.status === 'paused' ? 'paused' : s.status === 'closed' ? 'closed' : 'draft'}`}>
                        {s.status}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          className="btn btn-outline btn-sm"
                          onClick={() => setViewingSub(s)}
                          title="View contract details"
                        >
                          <Eye size={13} />
                          <span>Details</span>
                        </button>

                        {s.status === 'active' && (
                          <button
                            className="btn btn-outline btn-sm"
                            onClick={() => handlePause(s.id)}
                            title="Pause subscription"
                          >
                            <PauseCircle size={13} color="#f59e0b" />
                            <span>Pause</span>
                          </button>
                        )}

                        {s.status === 'paused' && (
                          <button
                            className="btn btn-success btn-sm"
                            onClick={() => handleResume(s.id)}
                            title="Resume subscription"
                          >
                            <PlayCircle size={13} />
                            <span>Resume</span>
                          </button>
                        )}

                        {s.status !== 'closed' && (
                          <button
                            className="btn btn-outline btn-sm"
                            onClick={() => handleClose(s.id)}
                            title="Cancel / Close subscription"
                          >
                            <XCircle size={13} color="#ef4444" />
                            <span>Close</span>
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Subscription Details Modal */}
      {viewingSub && (
        <div className="modal-backdrop">
          <div className="modal-content" style={{ maxWidth: '650px' }}>
            <div className="modal-header">
              <div>
                <div className="modal-title">Subscription Contract: {viewingSub.subscriptionNumber}</div>
                <div style={{ fontSize: '0.8rem', color: '#64748b' }}>Customer: {viewingSub.customerName}</div>
              </div>
              <button onClick={() => setViewingSub(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <div className="modal-body">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px', background: '#f8fafc', padding: '14px', borderRadius: '8px', marginBottom: '16px' }}>
                <div>
                  <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: '700' }}>Status</div>
                  <div style={{ marginTop: '2px' }}>
                    <span className={`badge badge-${viewingSub.status === 'active' ? 'active' : viewingSub.status === 'paused' ? 'paused' : 'draft'}`}>
                      {viewingSub.status}
                    </span>
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: '700' }}>Next Billing</div>
                  <div style={{ fontWeight: '600', color: '#4f46e5', marginTop: '2px' }}>{viewingSub.nextBillingDate}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: '700' }}>Payment Terms</div>
                  <div style={{ fontWeight: '600', marginTop: '2px' }}>{viewingSub.paymentTerms}</div>
                </div>
              </div>

              <h4 style={{ fontSize: '0.9rem', fontWeight: '700', marginBottom: '8px' }}>Line Items:</h4>
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Product</th>
                      <th>Quantity</th>
                      <th>Unit Price</th>
                      <th>Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    {viewingSub.items?.map((it, idx) => (
                      <tr key={idx}>
                        <td>
                          <div style={{ fontWeight: '600' }}>{it.productName}</div>
                          {it.variantName && <div style={{ fontSize: '0.72rem', color: '#64748b' }}>Variant: {it.variantName}</div>}
                        </td>
                        <td>{it.quantity}</td>
                        <td>${it.unitPrice}</td>
                        <td style={{ fontWeight: '700', color: '#4f46e5' }}>${it.subtotal}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {viewingSub.notes && (
                <div style={{ marginTop: '16px', background: '#f8fafc', padding: '12px', borderRadius: '8px', fontSize: '0.82rem', color: '#475569' }}>
                  <strong>Notes:</strong> {viewingSub.notes}
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setViewingSub(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Subscriptions;
