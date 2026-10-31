import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../api';
import { useAuth } from '../context/AuthContext';
import {
  CreditCard,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Clock,
  RotateCcw,
  Receipt,
  DollarSign,
  Smartphone,
  Building,
  Banknote,
  X
} from 'lucide-react';

const PaymentSimulator = () => {
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const initialInvoiceId = searchParams.get('invoiceId') || '';

  const [invoices, setInvoices] = useState([]);
  const [payments, setPayments] = useState([]);
  const [refunds, setRefunds] = useState([]);
  const [loading, setLoading] = useState(true);

  // Payment Form
  const [selectedInvoiceId, setSelectedInvoiceId] = useState(initialInvoiceId);
  const [amount, setAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('upi');
  const [simulatedOutcome, setSimulatedOutcome] = useState('successful');
  const [notes, setNotes] = useState('');
  const [processing, setProcessing] = useState(false);
  const [simResult, setSimResult] = useState(null);

  // Refund Modal
  const [showRefundModal, setShowRefundModal] = useState(false);
  const [selectedPaymentForRefund, setSelectedPaymentForRefund] = useState(null);
  const [refundAmount, setRefundAmount] = useState('');
  const [refundReason, setRefundReason] = useState('Customer requested test refund');

  const fetchData = async () => {
    try {
      setLoading(true);
      const [invRes, pmtRes, refRes] = await Promise.all([
        api.get('/invoices'),
        api.get('/payments'),
        api.get('/payments/refunds'),
      ]);
      setInvoices(invRes.data);
      setPayments(pmtRes.data);
      setRefunds(refRes.data);

      if (initialInvoiceId && !amount) {
        const target = invRes.data.find((i) => i.id === initialInvoiceId);
        if (target) {
          setAmount(target.balanceDue);
        }
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleInvoiceChange = (invId) => {
    setSelectedInvoiceId(invId);
    const target = invoices.find((i) => i.id === invId);
    if (target) {
      setAmount(target.balanceDue);
    }
  };

  const handleSimulatePayment = async (e) => {
    e.preventDefault();
    if (!selectedInvoiceId) {
      alert('Please select an invoice to pay');
      return;
    }

    setProcessing(true);
    setSimResult(null);

    try {
      const res = await api.post('/payments/simulate', {
        invoiceId: selectedInvoiceId,
        amount: Number(amount),
        paymentMethod,
        simulatedOutcome,
        notes,
      });
      setSimResult(res.data);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    } finally {
      setProcessing(false);
    }
  };

  const handleProcessRefund = async (e) => {
    e.preventDefault();
    try {
      await api.post('/payments/refund', {
        paymentId: selectedPaymentForRefund.id,
        amount: Number(refundAmount),
        reason: refundReason,
      });
      setShowRefundModal(false);
      setSelectedPaymentForRefund(null);
      fetchData();
      alert('Simulated refund issued successfully.');
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const formatCurrency = (val) => {
    const num = Number(val || 0);
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(num);
  };

  const currentInvoice = invoices.find((i) => i.id === selectedInvoiceId);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: '800', letterSpacing: '-0.02em', color: '#0f172a' }}>
            Dummy Payment Simulator &amp; Refunds
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
            Part 3: Internally developed payment simulator. Test UPI, Cards, Bank Transfers, partial payments, and refund reversals without real money.
          </p>
        </div>

        <div style={{ background: '#f8fafc', border: '1px solid #cbd5e1', padding: '8px 16px', borderRadius: '8px', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.82rem', color: '#475569' }}>
          <ShieldCheck size={18} color="#10b981" />
          <span>Internal Sandbox Mode (No Real Gateway)</span>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1.8fr', gap: '24px', marginBottom: '28px' }}>
        {/* Payment Simulator Panel */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Simulate Transaction</div>
              <div className="card-subtitle">Choose invoice, payment rail, and outcome</div>
            </div>
            <CreditCard size={18} color="#4f46e5" />
          </div>

          <form onSubmit={handleSimulatePayment}>
            <div className="form-group">
              <label className="form-label">Select Outstanding Invoice</label>
              <select
                className="form-control"
                required
                value={selectedInvoiceId}
                onChange={(e) => handleInvoiceChange(e.target.value)}
              >
                <option value="">-- Choose Invoice --</option>
                {invoices.filter((i) => i.status !== 'cancelled').map((inv) => (
                  <option key={inv.id} value={inv.id}>
                    {inv.invoiceNumber} — {inv.customerName} (Bal: ${inv.balanceDue}) {inv.status === 'paid' ? '[PAID]' : ''}
                  </option>
                ))}
              </select>
            </div>

            {currentInvoice && (
              <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '12px', marginBottom: '16px', fontSize: '0.82rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                  <span style={{ color: '#64748b' }}>Total Invoice Amount:</span>
                  <span style={{ fontWeight: '700' }}>{formatCurrency(currentInvoice.totalAmount)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                  <span style={{ color: '#64748b' }}>Already Paid:</span>
                  <span style={{ color: '#10b981', fontWeight: '700' }}>{formatCurrency(currentInvoice.amountPaid)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid #e2e8f0', paddingTop: '6px' }}>
                  <span style={{ fontWeight: '700', color: '#0f172a' }}>Outstanding Balance:</span>
                  <span style={{ fontWeight: '800', color: Number(currentInvoice.balanceDue) > 0 ? '#ef4444' : '#10b981' }}>
                    {formatCurrency(currentInvoice.balanceDue)}
                  </span>
                </div>
              </div>
            )}

            <div className="form-group">
              <label className="form-label">Payment Amount ($) - Partial Payments Supported</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                className="form-control"
                placeholder="e.g. 250.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Simulated Payment Method</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                <button
                  type="button"
                  onClick={() => setPaymentMethod('upi')}
                  style={{
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid',
                    borderColor: paymentMethod === 'upi' ? '#4f46e5' : '#e2e8f0',
                    background: paymentMethod === 'upi' ? '#eef2ff' : '#ffffff',
                    color: paymentMethod === 'upi' ? '#4f46e5' : '#475569',
                    fontWeight: '600',
                    fontSize: '0.82rem',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    justifyContent: 'center',
                  }}
                >
                  <Smartphone size={16} />
                  <span>UPI Instant</span>
                </button>

                <button
                  type="button"
                  onClick={() => setPaymentMethod('credit_card')}
                  style={{
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid',
                    borderColor: paymentMethod === 'credit_card' ? '#4f46e5' : '#e2e8f0',
                    background: paymentMethod === 'credit_card' ? '#eef2ff' : '#ffffff',
                    color: paymentMethod === 'credit_card' ? '#4f46e5' : '#475569',
                    fontWeight: '600',
                    fontSize: '0.82rem',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    justifyContent: 'center',
                  }}
                >
                  <CreditCard size={16} />
                  <span>Credit / Debit Card</span>
                </button>

                <button
                  type="button"
                  onClick={() => setPaymentMethod('bank_transfer')}
                  style={{
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid',
                    borderColor: paymentMethod === 'bank_transfer' ? '#4f46e5' : '#e2e8f0',
                    background: paymentMethod === 'bank_transfer' ? '#eef2ff' : '#ffffff',
                    color: paymentMethod === 'bank_transfer' ? '#4f46e5' : '#475569',
                    fontWeight: '600',
                    fontSize: '0.82rem',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    justifyContent: 'center',
                  }}
                >
                  <Building size={16} />
                  <span>Bank Transfer / ACH</span>
                </button>

                <button
                  type="button"
                  onClick={() => setPaymentMethod('cash')}
                  style={{
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid',
                    borderColor: paymentMethod === 'cash' ? '#4f46e5' : '#e2e8f0',
                    background: paymentMethod === 'cash' ? '#eef2ff' : '#ffffff',
                    color: paymentMethod === 'cash' ? '#4f46e5' : '#475569',
                    fontWeight: '600',
                    fontSize: '0.82rem',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    justifyContent: 'center',
                  }}
                >
                  <Banknote size={16} />
                  <span>Manual / Cash</span>
                </button>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Simulated Outcome (Demo Control)</label>
              <select
                className="form-control"
                value={simulatedOutcome}
                onChange={(e) => setSimulatedOutcome(e.target.value)}
              >
                <option value="successful">Successful (Funds Allocated &amp; Balance Reduced)</option>
                <option value="failed">Failed (Simulator Decline - Balance Untouched)</option>
                <option value="pending">Pending (Awaiting Settlement - Queued)</option>
              </select>
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', marginTop: '8px' }}
              disabled={processing || (currentInvoice && Number(currentInvoice.balanceDue) <= 0)}
            >
              {processing ? 'Processing Simulator...' : 'Execute Payment Simulation'}
            </button>
          </form>

          {/* Simulation Output Banner */}
          {simResult && (
            <div
              style={{
                marginTop: '20px',
                padding: '16px',
                borderRadius: '8px',
                border: '1px solid',
                background: simResult.outcome === 'successful' ? '#ecfdf5' : simResult.outcome === 'failed' ? '#fef2f2' : '#fffbeb',
                borderColor: simResult.outcome === 'successful' ? '#a7f3d0' : simResult.outcome === 'failed' ? '#fecaca' : '#fde68a',
                color: simResult.outcome === 'successful' ? '#065f46' : simResult.outcome === 'failed' ? '#991b1b' : '#92400e',
                fontSize: '0.85rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: '700', marginBottom: '6px' }}>
                {simResult.outcome === 'successful' ? (
                  <CheckCircle2 size={18} color="#10b981" />
                ) : simResult.outcome === 'failed' ? (
                  <AlertCircle size={18} color="#ef4444" />
                ) : (
                  <Clock size={18} color="#f59e0b" />
                )}
                <span>Outcome: {simResult.outcome.toUpperCase()} [Simulated]</span>
              </div>
              <div>{simResult.message}</div>
              <div style={{ marginTop: '8px', fontSize: '0.78rem', color: '#475569' }}>
                Reference: <strong style={{ fontFamily: 'monospace' }}>{simResult.referenceNumber}</strong> | Remaining Balance: <strong>${simResult.remainingBalance}</strong>
              </div>
            </div>
          )}
        </div>

        {/* Transaction History & Refund Actions */}
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Simulated Payment History ({payments.length})</div>
              <div className="card-subtitle">Allocations against invoices &amp; refund reversal controls</div>
            </div>
          </div>

          <div className="table-container" style={{ maxHeight: '460px', overflowY: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th>Reference #</th>
                  <th>Invoice</th>
                  <th>Method</th>
                  <th>Paid Amount</th>
                  <th>Refunded</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {payments.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                      No payments simulated yet.
                    </td>
                  </tr>
                ) : (
                  payments.map((p) => {
                    const maxRefund = Number(p.amount) - Number(p.refundedAmount || 0);
                    return (
                      <tr key={p.id}>
                        <td>
                          <div style={{ fontFamily: 'monospace', fontWeight: '700', color: '#0f172a' }}>
                            {p.referenceNumber}
                          </div>
                          <div style={{ fontSize: '0.72rem', color: '#64748b' }}>{p.paymentDate}</div>
                        </td>
                        <td style={{ fontWeight: '600', color: '#4f46e5' }}>{p.invoiceNumber}</td>
                        <td style={{ textTransform: 'capitalize' }}>{String(p.paymentMethod).replace('_', ' ')}</td>
                        <td style={{ fontWeight: '700', color: '#10b981' }}>{formatCurrency(p.amount)}</td>
                        <td style={{ color: Number(p.refundedAmount) > 0 ? '#ef4444' : '#64748b' }}>
                          {formatCurrency(p.refundedAmount)}
                        </td>
                        <td>
                          {maxRefund > 0 ? (
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => {
                                setSelectedPaymentForRefund(p);
                                setRefundAmount(maxRefund.toFixed(2));
                                setShowRefundModal(true);
                              }}
                              style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
                            >
                              <RotateCcw size={12} />
                              <span>Refund</span>
                            </button>
                          ) : (
                            <span className="badge badge-draft">Fully Refunded</span>
                          )}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Refunds History Table */}
      {refunds.length > 0 && (
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Simulated Refunds Log ({refunds.length})</div>
              <div className="card-subtitle">Reversals processed without erasing payment records</div>
            </div>
            <RotateCcw size={18} color="#ef4444" />
          </div>

          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Refund ID</th>
                  <th>Payment Ref</th>
                  <th>Invoice</th>
                  <th>Refund Amount</th>
                  <th>Reason</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {refunds.map((r) => (
                  <tr key={r.id}>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.78rem' }}>{r.id.substring(0, 8)}...</td>
                    <td style={{ fontFamily: 'monospace', fontWeight: '600' }}>{r.referenceNumber}</td>
                    <td style={{ fontWeight: '600', color: '#4f46e5' }}>{r.invoiceNumber}</td>
                    <td style={{ fontWeight: '700', color: '#ef4444' }}>{formatCurrency(r.amount)}</td>
                    <td style={{ color: '#64748b' }}>{r.reason || 'Requested test reversal'}</td>
                    <td style={{ fontSize: '0.78rem', color: '#64748b' }}>
                      {r.createdAt ? r.createdAt.substring(0, 19).replace('T', ' ') : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Refund Modal */}
      {showRefundModal && selectedPaymentForRefund && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">Simulate Payment Refund</div>
              <button onClick={() => setShowRefundModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleProcessRefund}>
              <div className="modal-body">
                <div style={{ background: '#f8fafc', padding: '12px', borderRadius: '8px', marginBottom: '16px', fontSize: '0.82rem' }}>
                  <div>Original Payment: <strong>{selectedPaymentForRefund.referenceNumber}</strong></div>
                  <div>Invoice: <strong>{selectedPaymentForRefund.invoiceNumber}</strong></div>
                  <div>Total Paid: <strong>{formatCurrency(selectedPaymentForRefund.amount)}</strong></div>
                  <div>Already Refunded: <strong>{formatCurrency(selectedPaymentForRefund.refundedAmount)}</strong></div>
                </div>

                <div className="form-group">
                  <label className="form-label">Refund Amount ($) - Full or Partial</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    max={Number(selectedPaymentForRefund.amount) - Number(selectedPaymentForRefund.refundedAmount || 0)}
                    required
                    className="form-control"
                    value={refundAmount}
                    onChange={(e) => setRefundAmount(e.target.value)}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Reason for Refund</label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    value={refundReason}
                    onChange={(e) => setRefundReason(e.target.value)}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowRefundModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-danger">
                  Execute Refund Simulation
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default PaymentSimulator;
