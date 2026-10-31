import React, { useState, useEffect } from 'react';
import api from '../api';
import { useAuth } from '../context/AuthContext';
import {
  FileSpreadsheet,
  Plus,
  CheckCircle2,
  XCircle,
  Eye,
  Trash2,
  Layers,
  Calendar,
  X,
  FileCheck
} from 'lucide-react';

const Quotations = () => {
  const { user } = useAuth();
  const isCustomer = user?.role === 'customer';
  const isAdminOrStaff = user?.role === 'admin' || user?.role === 'staff';

  const [quotations, setQuotations] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [products, setProducts] = useState([]);
  const [plans, setPlans] = useState([]);
  const [taxes, setTaxes] = useState([]);
  const [discounts, setDiscounts] = useState([]);
  const [loading, setLoading] = useState(true);

  // Create Quotation Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedTemplateId, setSelectedTemplateId] = useState('');
  const [quoteForm, setQuoteForm] = useState({
    customerId: '',
    planId: '',
    discountId: '',
    paymentTerms: 'Due on receipt',
    validityDays: 30,
    notes: '',
  });

  const [lineItems, setLineItems] = useState([
    { productId: '', variantId: '', quantity: 1, taxId: '', unitPrice: 0 },
  ]);

  // Detail Modal
  const [viewingQuote, setViewingQuote] = useState(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [qRes, tRes, pRes, plRes, txRes, dRes] = await Promise.all([
        api.get('/quotations'),
        api.get('/quotations/templates'),
        api.get('/products'),
        api.get('/plans'),
        api.get('/taxes'),
        api.get('/discounts'),
      ]);
      setQuotations(qRes.data);
      setTemplates(tRes.data);
      setProducts(pRes.data);
      setPlans(plRes.data);
      setTaxes(txRes.data);
      setDiscounts(dRes.data);

      if (!isCustomer) {
        const cRes = await api.get('/users/customers');
        setCustomers(cRes.data);
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

  const handleTemplateChange = (templateId) => {
    setSelectedTemplateId(templateId);
    if (!templateId) return;
    const template = templates.find((t) => t.id === templateId);
    if (template) {
      setQuoteForm((prev) => ({
        ...prev,
        planId: template.defaultPlanId || prev.planId,
        validityDays: template.validityDays || 30,
      }));
    }
  };

  const handleProductChange = (index, productId) => {
    const prod = products.find((p) => p.id === productId);
    const newItems = [...lineItems];
    newItems[index].productId = productId;
    newItems[index].variantId = '';
    newItems[index].unitPrice = prod ? Number(prod.salesPrice) : 0;
    setLineItems(newItems);
  };

  const handleVariantChange = (index, variantId) => {
    const newItems = [...lineItems];
    newItems[index].variantId = variantId;
    const prod = products.find((p) => p.id === newItems[index].productId);
    let base = prod ? Number(prod.salesPrice) : 0;
    if (variantId && prod && prod.variants) {
      const v = prod.variants.find((v) => v.id === variantId);
      if (v) base += Number(v.extraPrice);
    }
    newItems[index].unitPrice = base;
    setLineItems(newItems);
  };

  const addLineItem = () => {
    setLineItems([...lineItems, { productId: '', variantId: '', quantity: 1, taxId: '', unitPrice: 0 }]);
  };

  const removeLineItem = (index) => {
    setLineItems(lineItems.filter((_, i) => i !== index));
  };

  const handleCreateQuotation = async (e) => {
    e.preventDefault();
    if (!quoteForm.customerId && !isCustomer) {
      alert('Please select a customer');
      return;
    }
    if (!quoteForm.planId) {
      alert('Please select a billing plan');
      return;
    }

    try {
      const payload = {
        quotation: {
          customerId: isCustomer ? user.id : quoteForm.customerId,
          planId: quoteForm.planId,
          discountId: quoteForm.discountId || null,
          paymentTerms: quoteForm.paymentTerms,
          notes: quoteForm.notes,
        },
        items: lineItems.filter((item) => item.productId).map((item) => ({
          productId: item.productId,
          variantId: item.variantId || null,
          quantity: Number(item.quantity),
          unitPrice: Number(item.unitPrice),
          taxId: item.taxId || null,
          subtotal: Number(item.unitPrice) * Number(item.quantity),
        })),
      };

      await api.post('/quotations', payload);
      setShowCreateModal(false);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleAcceptQuote = async (id) => {
    if (!window.confirm('Accept this quotation offer and convert it to an active subscription?')) return;
    try {
      await api.post(`/quotations/${id}/accept`);
      fetchData();
      alert('Quotation accepted! Subscription is now Active.');
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleRejectQuote = async (id) => {
    if (!window.confirm('Reject this quotation offer?')) return;
    try {
      await api.post(`/quotations/${id}/reject`);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: '800', letterSpacing: '-0.02em', color: '#0f172a' }}>
            {isCustomer ? 'Quotation Offers' : 'Quotations & Sales Proposals'}
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
            Part 2: Prepare offers from templates, configure line items &amp; taxes, and convert accepted quotes into active subscriptions.
          </p>
        </div>

        {isAdminOrStaff && (
          <button
            className="btn btn-primary"
            onClick={() => {
              setQuoteForm({
                customerId: customers[0]?.id || '',
                planId: plans[0]?.id || '',
                discountId: '',
                paymentTerms: 'Due on receipt',
                validityDays: 30,
                notes: '',
              });
              setLineItems([{ productId: products[0]?.id || '', variantId: '', quantity: 1, taxId: '', unitPrice: products[0]?.salesPrice || 0 }]);
              setShowCreateModal(true);
            }}
          >
            <Plus size={16} />
            <span>Prepare Quotation Offer</span>
          </button>
        )}
      </div>

      {/* Quotation Table */}
      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Quote Number</th>
                <th>Customer</th>
                <th>Billing Plan</th>
                <th>Payment Terms</th>
                <th>Valid Until</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {quotations.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                    No quotation offers found.
                  </td>
                </tr>
              ) : (
                quotations.map((q) => (
                  <tr key={q.id}>
                    <td style={{ fontFamily: 'monospace', fontWeight: '700', color: '#0f172a' }}>
                      {q.subscriptionNumber}
                    </td>
                    <td>
                      <div style={{ fontWeight: '600' }}>{q.customerName}</div>
                      <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{q.customerEmail}</div>
                    </td>
                    <td>
                      <span className="badge badge-secondary">{q.planName}</span>
                    </td>
                    <td>{q.paymentTerms}</td>
                    <td style={{ fontSize: '0.82rem', color: '#475569' }}>
                      {q.expiryDate || '30 days from creation'}
                    </td>
                    <td>
                      <span className={`badge badge-${q.status === 'quotation' ? 'warning' : q.status === 'active' ? 'active' : 'draft'}`}>
                        {q.status}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          className="btn btn-outline btn-sm"
                          onClick={() => setViewingQuote(q)}
                        >
                          <Eye size={13} />
                          <span>View</span>
                        </button>

                        {(q.status === 'quotation' || q.status === 'draft') && (
                          <>
                            <button
                              className="btn btn-success btn-sm"
                              onClick={() => handleAcceptQuote(q.id)}
                              title="Accept quote &amp; convert to active subscription"
                            >
                              <CheckCircle2 size={13} />
                              <span>Accept Offer</span>
                            </button>
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => handleRejectQuote(q.id)}
                              title="Decline proposal"
                            >
                              <XCircle size={13} color="#ef4444" />
                            </button>
                          </>
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

      {/* Create Quotation Modal */}
      {showCreateModal && (
        <div className="modal-backdrop">
          <div className="modal-content" style={{ maxWidth: '850px' }}>
            <div className="modal-header">
              <div>
                <div className="modal-title">Prepare Quotation Offer</div>
                <div style={{ fontSize: '0.8rem', color: '#64748b' }}>
                  Per PDF: Start from a template or select custom plan and product variants.
                </div>
              </div>
              <button onClick={() => setShowCreateModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleCreateQuotation}>
              <div className="modal-body">
                {/* Template & Customer Row */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '16px' }}>
                  <div className="form-group" style={{ margin: 0 }}>
                    <label className="form-label">Quotation Template</label>
                    <select
                      className="form-control"
                      value={selectedTemplateId}
                      onChange={(e) => handleTemplateChange(e.target.value)}
                    >
                      <option value="">-- Choose Template --</option>
                      {templates.map((t) => (
                        <option key={t.id} value={t.id}>{t.name}</option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group" style={{ margin: 0 }}>
                    <label className="form-label">Customer</label>
                    <select
                      className="form-control"
                      required
                      value={quoteForm.customerId}
                      onChange={(e) => setQuoteForm({ ...quoteForm, customerId: e.target.value })}
                    >
                      <option value="">-- Select Customer --</option>
                      {customers.map((c) => (
                        <option key={c.id} value={c.id}>{c.fullName} ({c.email})</option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group" style={{ margin: 0 }}>
                    <label className="form-label">Assigned Billing Plan</label>
                    <select
                      className="form-control"
                      required
                      value={quoteForm.planId}
                      onChange={(e) => setQuoteForm({ ...quoteForm, planId: e.target.value })}
                    >
                      <option value="">-- Select Plan --</option>
                      {plans.map((pl) => (
                        <option key={pl.id} value={pl.id}>
                          {pl.name} (${pl.price}/{pl.billingInterval})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Line Items Section */}
                <div style={{ border: '1px solid #e2e8f0', borderRadius: '10px', padding: '16px', marginBottom: '16px', background: '#f8fafc' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                    <span style={{ fontWeight: '700', fontSize: '0.9rem', color: '#1e293b' }}>Quotation Product Lines</span>
                    <button type="button" onClick={addLineItem} className="btn btn-outline btn-sm">
                      + Add Product Line
                    </button>
                  </div>

                  {lineItems.map((item, idx) => {
                    const prod = products.find((p) => p.id === item.productId);
                    return (
                      <div
                        key={idx}
                        style={{
                          display: 'grid',
                          gridTemplateColumns: '2fr 1.5fr 1fr 1fr 1fr auto',
                          gap: '10px',
                          alignItems: 'center',
                          marginBottom: '10px',
                          background: '#ffffff',
                          padding: '10px',
                          borderRadius: '8px',
                          border: '1px solid #e2e8f0',
                        }}
                      >
                        <div>
                          <label style={{ fontSize: '0.72rem', color: '#64748b', display: 'block', marginBottom: '4px' }}>Product</label>
                          <select
                            className="form-control"
                            style={{ padding: '6px 10px', fontSize: '0.82rem' }}
                            value={item.productId}
                            onChange={(e) => handleProductChange(idx, e.target.value)}
                            required
                          >
                            <option value="">Select Product</option>
                            {products.map((p) => (
                              <option key={p.id} value={p.id}>{p.name} (${p.salesPrice})</option>
                            ))}
                          </select>
                        </div>

                        <div>
                          <label style={{ fontSize: '0.72rem', color: '#64748b', display: 'block', marginBottom: '4px' }}>Variant (Optional)</label>
                          <select
                            className="form-control"
                            style={{ padding: '6px 10px', fontSize: '0.82rem' }}
                            value={item.variantId}
                            onChange={(e) => handleVariantChange(idx, e.target.value)}
                            disabled={!prod || !prod.variants || prod.variants.length === 0}
                          >
                            <option value="">Base / None</option>
                            {prod?.variants?.map((v) => (
                              <option key={v.id} value={v.id}>{v.variantName} (+${v.extraPrice})</option>
                            ))}
                          </select>
                        </div>

                        <div>
                          <label style={{ fontSize: '0.72rem', color: '#64748b', display: 'block', marginBottom: '4px' }}>Quantity</label>
                          <input
                            type="number"
                            min="1"
                            className="form-control"
                            style={{ padding: '6px 10px', fontSize: '0.82rem' }}
                            value={item.quantity}
                            onChange={(e) => {
                              const newItems = [...lineItems];
                              newItems[idx].quantity = e.target.value;
                              setLineItems(newItems);
                            }}
                          />
                        </div>

                        <div>
                          <label style={{ fontSize: '0.72rem', color: '#64748b', display: 'block', marginBottom: '4px' }}>Tax Rule</label>
                          <select
                            className="form-control"
                            style={{ padding: '6px 10px', fontSize: '0.82rem' }}
                            value={item.taxId}
                            onChange={(e) => {
                              const newItems = [...lineItems];
                              newItems[idx].taxId = e.target.value;
                              setLineItems(newItems);
                            }}
                          >
                            <option value="">No Tax</option>
                            {taxes.map((t) => (
                              <option key={t.id} value={t.id}>{t.name} ({t.rate}%)</option>
                            ))}
                          </select>
                        </div>

                        <div>
                          <label style={{ fontSize: '0.72rem', color: '#64748b', display: 'block', marginBottom: '4px' }}>Subtotal</label>
                          <div style={{ fontWeight: '700', fontSize: '0.9rem', color: '#4f46e5' }}>
                            ${(Number(item.unitPrice) * Number(item.quantity || 1)).toFixed(2)}
                          </div>
                        </div>

                        <div>
                          {lineItems.length > 1 && (
                            <button
                              type="button"
                              onClick={() => removeLineItem(idx)}
                              style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '6px', marginTop: '16px' }}
                              title="Remove item line"
                            >
                              <Trash2 size={16} color="#ef4444" />
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* Discounts & Terms Row */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label">Promotional Discount (Optional)</label>
                    <select
                      className="form-control"
                      value={quoteForm.discountId}
                      onChange={(e) => setQuoteForm({ ...quoteForm, discountId: e.target.value })}
                    >
                      <option value="">-- No Discount --</option>
                      {discounts.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.code} - {d.value} {d.discountType === 'percentage' ? '%' : '$'} ({d.targetScope})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Payment Terms</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="e.g. Net 15, Due on receipt"
                      value={quoteForm.paymentTerms}
                      onChange={(e) => setQuoteForm({ ...quoteForm, paymentTerms: e.target.value })}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Proposal Notes &amp; Terms</label>
                  <textarea
                    rows={2}
                    className="form-control"
                    placeholder="Offer conditions, custom terms, etc."
                    value={quoteForm.notes}
                    onChange={(e) => setQuoteForm({ ...quoteForm, notes: e.target.value })}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Generate Proposal
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* View Quotation Details Modal */}
      {viewingQuote && (
        <div className="modal-backdrop">
          <div className="modal-content" style={{ maxWidth: '650px' }}>
            <div className="modal-header">
              <div>
                <div className="modal-title">Quotation Proposal: {viewingQuote.subscriptionNumber}</div>
                <div style={{ fontSize: '0.8rem', color: '#64748b' }}>For {viewingQuote.customerName}</div>
              </div>
              <button onClick={() => setViewingQuote(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <div className="modal-body">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px', background: '#f8fafc', padding: '14px', borderRadius: '8px', marginBottom: '16px' }}>
                <div>
                  <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: '700' }}>Plan</div>
                  <div style={{ fontWeight: '600' }}>{viewingQuote.planName}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: '700' }}>Interval</div>
                  <div style={{ fontWeight: '600', textTransform: 'capitalize' }}>{viewingQuote.billingInterval}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: '700' }}>Valid Until</div>
                  <div style={{ fontWeight: '600' }}>{viewingQuote.expiryDate || '30 days'}</div>
                </div>
              </div>

              <h4 style={{ fontSize: '0.9rem', fontWeight: '700', marginBottom: '8px' }}>Line Items:</h4>
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Product</th>
                      <th>Qty</th>
                      <th>Unit Price</th>
                      <th>Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    {viewingQuote.items?.map((it, idx) => (
                      <tr key={idx}>
                        <td>
                          <div style={{ fontWeight: '600' }}>{it.productName}</div>
                          {it.variantName && <div style={{ fontSize: '0.72rem', color: '#64748b' }}>{it.variantName}</div>}
                        </td>
                        <td>{it.quantity}</td>
                        <td>${it.unitPrice}</td>
                        <td style={{ fontWeight: '700', color: '#4f46e5' }}>${it.subtotal}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {viewingQuote.notes && (
                <div style={{ marginTop: '16px', background: '#f8fafc', padding: '12px', borderRadius: '8px', fontSize: '0.82rem', color: '#475569' }}>
                  <strong>Terms / Notes:</strong> {viewingQuote.notes}
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setViewingQuote(null)}>
                Close
              </button>
              {(viewingQuote.status === 'quotation' || viewingQuote.status === 'draft') && (
                <button
                  className="btn btn-success"
                  onClick={() => {
                    handleAcceptQuote(viewingQuote.id);
                    setViewingQuote(null);
                  }}
                >
                  <CheckCircle2 size={16} />
                  <span>Accept and Activate Subscription</span>
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Quotations;
