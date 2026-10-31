import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { useAuth } from '../context/AuthContext';
import {
  Receipt,
  Play,
  Printer,
  CreditCard,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Eye,
  X,
  Filter
} from 'lucide-react';

const Invoices = () => {
  const { user } = useAuth();
  const isCustomer = user?.role === 'customer';
  const isAdminOrStaff = user?.role === 'admin' || user?.role === 'staff';
  const navigate = useNavigate();

  const [invoices, setInvoices] = useState([]);
  const [filterStatus, setFilterStatus] = useState('');
  const [filterOverdue, setFilterOverdue] = useState(false);
  const [loading, setLoading] = useState(true);
  const [billingRunning, setBillingRunning] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState(null);

  const fetchInvoices = async () => {
    try {
      setLoading(true);
      const res = await api.get('/invoices', {
        params: {
          status: filterStatus || undefined,
          overdueOnly: filterOverdue ? true : undefined,
        },
      });
      setInvoices(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoices();
  }, [filterStatus, filterOverdue]);

  const handleTriggerBilling = async () => {
    setBillingRunning(true);
    try {
      const res = await api.post('/billing/run-job');
      alert(`Automated Billing Completed!\nGenerated ${res.data.invoicesGenerated} new invoice(s).`);
      fetchInvoices();
    } catch (err) {
      alert('Billing execution error: ' + (err.response?.data?.message || err.message));
    } finally {
      setBillingRunning(false);
    }
  };

  const handleConfirmInvoice = async (id) => {
    try {
      await api.post(`/invoices/${id}/confirm`);
      fetchInvoices();
      if (selectedInvoice) {
        const updated = await api.get(`/invoices/${id}`);
        setSelectedInvoice(updated.data);
      }
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleCancelInvoice = async (id) => {
    const reason = window.prompt('Reason for cancelling invoice:', 'Billing cycle correction');
    if (reason === null) return;
    try {
      await api.post(`/invoices/${id}/cancel?reason=${encodeURIComponent(reason)}`);
      fetchInvoices();
      if (selectedInvoice) {
        setSelectedInvoice(null);
      }
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  const formatCurrency = (val) => {
    const num = Number(val || 0);
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(num);
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: '800', letterSpacing: '-0.02em', color: '#0f172a' }}>
            {isCustomer ? 'My Invoices & Receipts' : 'Billing & Invoice Management'}
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
            Part 3: Automated recurring invoices, automated tax and discount snapshotting, balance tracking, and print receipts.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          {!isCustomer && (
            <button
              onClick={handleTriggerBilling}
              disabled={billingRunning}
              className="btn btn-primary"
              style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <Play size={16} />
              <span>{billingRunning ? 'Generating Invoices...' : 'Run Billing Scheduler'}</span>
            </button>
          )}
        </div>
      </div>

      {/* Filter Bar */}
      <div style={{ display: 'flex', gap: '12px', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap' }}>
        <button
          onClick={() => { setFilterStatus(''); setFilterOverdue(false); }}
          className={`btn btn-sm ${!filterStatus && !filterOverdue ? 'btn-primary' : 'btn-outline'}`}
        >
          All Invoices
        </button>
        <button
          onClick={() => { setFilterStatus('confirmed'); setFilterOverdue(false); }}
          className={`btn btn-sm ${filterStatus === 'confirmed' ? 'btn-primary' : 'btn-outline'}`}
        >
          Open / Confirmed
        </button>
        <button
          onClick={() => { setFilterStatus('paid'); setFilterOverdue(false); }}
          className={`btn btn-sm ${filterStatus === 'paid' ? 'btn-primary' : 'btn-outline'}`}
        >
          Fully Paid
        </button>
        <button
          onClick={() => { setFilterStatus(''); setFilterOverdue(true); }}
          className={`btn btn-sm ${filterOverdue ? 'btn-danger' : 'btn-outline'}`}
          style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <AlertTriangle size={14} />
          <span>Overdue Exposure</span>
        </button>
      </div>

      {/* Invoices Table */}
      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Invoice #</th>
                <th>Customer</th>
                <th>Billing Period</th>
                <th>Due Date</th>
                <th>Total</th>
                <th>Paid</th>
                <th>Balance Due</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {invoices.length === 0 ? (
                <tr>
                  <td colSpan={9} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                    No invoices matching filter criteria.
                  </td>
                </tr>
              ) : (
                invoices.map((inv) => (
                  <tr key={inv.id}>
                    <td style={{ fontFamily: 'monospace', fontWeight: '700', color: '#0f172a' }}>
                      {inv.invoiceNumber}
                    </td>
                    <td>
                      <div style={{ fontWeight: '600' }}>{inv.customerName}</div>
                      <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{inv.customerEmail}</div>
                    </td>
                    <td style={{ fontSize: '0.82rem', color: '#475569' }}>
                      {inv.billingPeriodStart} to {inv.billingPeriodEnd}
                    </td>
                    <td style={{ fontSize: '0.82rem', fontWeight: inv.isOverdue ? '700' : '500', color: inv.isOverdue ? '#ef4444' : '#0f172a' }}>
                      {inv.dueDate}
                    </td>
                    <td style={{ fontWeight: '700' }}>{formatCurrency(inv.totalAmount)}</td>
                    <td style={{ color: '#10b981', fontWeight: '600' }}>{formatCurrency(inv.amountPaid)}</td>
                    <td style={{ fontWeight: '700', color: Number(inv.balanceDue) > 0 ? '#ef4444' : '#10b981' }}>
                      {formatCurrency(inv.balanceDue)}
                    </td>
                    <td>
                      <span className={`badge badge-${inv.status === 'paid' ? 'paid' : inv.isOverdue ? 'overdue' : inv.status === 'confirmed' ? 'confirmed' : 'draft'}`}>
                        {inv.isOverdue ? 'Overdue' : inv.status}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        <button
                          className="btn btn-outline btn-sm"
                          onClick={() => setSelectedInvoice(inv)}
                          title="View and print invoice"
                        >
                          <Eye size={13} />
                          <span>View</span>
                        </button>

                        {inv.status !== 'paid' && inv.status !== 'cancelled' && (
                          <button
                            className="btn btn-success btn-sm"
                            onClick={() => navigate(`/payment-simulator?invoiceId=${inv.id}`)}
                            title="Open dummy payment simulator"
                          >
                            <CreditCard size={13} />
                            <span>Pay (Demo)</span>
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

      {/* Invoice Detail & Printable Receipt Modal */}
      {selectedInvoice && (
        <div className="modal-backdrop">
          <div className="modal-content" style={{ maxWidth: '750px', background: '#ffffff' }}>
            <div className="modal-header" style={{ borderBottom: '1px solid #e2e8f0' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Receipt size={22} color="#4f46e5" />
                <div className="modal-title">Invoice #{selectedInvoice.invoiceNumber}</div>
              </div>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button onClick={handlePrint} className="btn btn-outline btn-sm" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <Printer size={14} />
                  <span>Print Receipt</span>
                </button>
                <button onClick={() => setSelectedInvoice(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                  <X size={20} />
                </button>
              </div>
            </div>

            <div className="modal-body" id="printable-invoice" style={{ padding: '28px' }}>
              {/* Header Box */}
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '28px' }}>
                <div>
                  <div style={{ fontSize: '1.4rem', fontWeight: '800', color: '#4f46e5', letterSpacing: '-0.02em' }}>
                    Brightloop Software
                  </div>
                  <div style={{ fontSize: '0.8rem', color: '#64748b', marginTop: '4px' }}>
                    Subscription Billing Division<br />
                    billing@brightloop.com | +1 800-BRIGHTLOOP
                  </div>
                </div>

                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '0.78rem', textTransform: 'uppercase', color: '#64748b', fontWeight: '700' }}>Invoice Status</div>
                  <div style={{ marginTop: '4px' }}>
                    <span className={`badge badge-${selectedInvoice.status === 'paid' ? 'paid' : selectedInvoice.isOverdue ? 'overdue' : 'confirmed'}`} style={{ fontSize: '0.85rem' }}>
                      {selectedInvoice.isOverdue ? 'OVERDUE' : selectedInvoice.status}
                    </span>
                  </div>
                </div>
              </div>

              {/* Billed To & Dates */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', background: '#f8fafc', padding: '16px', borderRadius: '10px', marginBottom: '24px' }}>
                <div>
                  <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: '700', marginBottom: '4px' }}>Billed To</div>
                  <div style={{ fontWeight: '700', fontSize: '0.95rem' }}>{selectedInvoice.customerName}</div>
                  <div style={{ fontSize: '0.82rem', color: '#475569' }}>{selectedInvoice.customerEmail}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '0.78rem', color: '#64748b' }}>
                    <strong>Billing Period:</strong> {selectedInvoice.billingPeriodStart} to {selectedInvoice.billingPeriodEnd}
                  </div>
                  <div style={{ fontSize: '0.78rem', color: '#64748b', marginTop: '3px' }}>
                    <strong>Issue Date:</strong> {selectedInvoice.issueDate}
                  </div>
                  <div style={{ fontSize: '0.78rem', color: selectedInvoice.isOverdue ? '#ef4444' : '#64748b', fontWeight: selectedInvoice.isOverdue ? '700' : '400', marginTop: '3px' }}>
                    <strong>Due Date:</strong> {selectedInvoice.dueDate}
                  </div>
                </div>
              </div>

              {/* Line Items Table */}
              <div className="table-container" style={{ marginBottom: '20px' }}>
                <table>
                  <thead>
                    <tr>
                      <th>Description</th>
                      <th>Qty</th>
                      <th>Unit Price</th>
                      <th>Tax Rate</th>
                      <th>Tax Amt</th>
                      <th style={{ textAlign: 'right' }}>Line Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedInvoice.items?.map((it, idx) => (
                      <tr key={idx}>
                        <td>
                          <div style={{ fontWeight: '600' }}>{it.description}</div>
                        </td>
                        <td>{it.quantity}</td>
                        <td>{formatCurrency(it.unitPrice)}</td>
                        <td>{it.taxRate}%</td>
                        <td>{formatCurrency(it.taxAmount)}</td>
                        <td style={{ textAlign: 'right', fontWeight: '700' }}>
                          {formatCurrency(it.subtotal)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Totals Summary */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '24px' }}>
                <div style={{ width: '280px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem' }}>
                    <span style={{ color: '#64748b' }}>Subtotal:</span>
                    <span style={{ fontWeight: '600' }}>{formatCurrency(selectedInvoice.subtotal)}</span>
                  </div>

                  {Number(selectedInvoice.discountAmount) > 0 && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem', color: '#10b981' }}>
                      <span>Discount Applied:</span>
                      <span style={{ fontWeight: '600' }}>-{formatCurrency(selectedInvoice.discountAmount)}</span>
                    </div>
                  )}

                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem' }}>
                    <span style={{ color: '#64748b' }}>Taxes:</span>
                    <span style={{ fontWeight: '600' }}>{formatCurrency(selectedInvoice.taxAmount)}</span>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '1.05rem', fontWeight: '800', borderTop: '1px solid #e2e8f0', paddingTop: '8px' }}>
                    <span>Total Amount:</span>
                    <span style={{ color: '#0f172a' }}>{formatCurrency(selectedInvoice.totalAmount)}</span>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem', color: '#10b981' }}>
                    <span>Amount Paid:</span>
                    <span style={{ fontWeight: '700' }}>{formatCurrency(selectedInvoice.amountPaid)}</span>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '1.05rem', fontWeight: '800', borderTop: '2px solid #0f172a', paddingTop: '8px', color: Number(selectedInvoice.balanceDue) > 0 ? '#ef4444' : '#10b981' }}>
                    <span>Balance Due:</span>
                    <span>{formatCurrency(selectedInvoice.balanceDue)}</span>
                  </div>
                </div>
              </div>

              {/* Payments History on this invoice */}
              {selectedInvoice.payments && selectedInvoice.payments.length > 0 && (
                <div>
                  <h4 style={{ fontSize: '0.88rem', fontWeight: '700', marginBottom: '8px', color: '#1e293b' }}>
                    Payment Allocation History:
                  </h4>
                  <div style={{ background: '#f8fafc', padding: '12px', borderRadius: '8px', fontSize: '0.8rem' }}>
                    {selectedInvoice.payments.map((p) => (
                      <div key={p.id} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                        <span>
                          <strong>{p.referenceNumber}</strong> ({p.paymentMethod}) on {p.paymentDate}
                        </span>
                        <span style={{ fontWeight: '700', color: '#10b981' }}>{formatCurrency(p.amount)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="modal-footer">
              {isAdminOrStaff && selectedInvoice.status === 'draft' && (
                <button
                  className="btn btn-primary"
                  onClick={() => handleConfirmInvoice(selectedInvoice.id)}
                >
                  Confirm Invoice
                </button>
              )}

              {isAdminOrStaff && selectedInvoice.status !== 'paid' && selectedInvoice.status !== 'cancelled' && (
                <button
                  className="btn btn-danger"
                  onClick={() => handleCancelInvoice(selectedInvoice.id)}
                >
                  Cancel Invoice
                </button>
              )}

              {selectedInvoice.status !== 'paid' && selectedInvoice.status !== 'cancelled' && (
                <button
                  className="btn btn-success"
                  onClick={() => {
                    navigate(`/payment-simulator?invoiceId=${selectedInvoice.id}`);
                    setSelectedInvoice(null);
                  }}
                >
                  <CreditCard size={16} />
                  <span>Pay Now (Demo Simulator)</span>
                </button>
              )}

              <button className="btn btn-secondary" onClick={() => setSelectedInvoice(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Invoices;
