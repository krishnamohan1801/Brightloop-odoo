import React, { useState, useEffect } from 'react';
import api from '../api';
import { useAuth } from '../context/AuthContext';
import {
  Percent,
  Plus,
  Edit2,
  Archive,
  RotateCcw,
  CheckCircle2,
  X,
  Receipt,
  Tag
} from 'lucide-react';

const DiscountsTaxes = () => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'admin';

  const [activeTab, setActiveTab] = useState('discounts');
  const [discounts, setDiscounts] = useState([]);
  const [taxes, setTaxes] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);

  // Discount modal
  const [showDiscountModal, setShowDiscountModal] = useState(false);
  const [editingDiscount, setEditingDiscount] = useState(null);
  const [discountForm, setDiscountForm] = useState({
    code: '',
    discountType: 'percentage',
    value: '',
    minPurchase: '0.00',
    minQuantity: 0,
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date(Date.now() + 180 * 86400000).toISOString().split('T')[0],
    maxUses: '',
    targetScope: 'subscription',
    targetProductId: '',
  });

  // Tax modal
  const [showTaxModal, setShowTaxModal] = useState(false);
  const [editingTax, setEditingTax] = useState(null);
  const [taxForm, setTaxForm] = useState({
    name: '',
    rate: '',
  });

  const fetchData = async () => {
    try {
      setLoading(true);
      const [dRes, tRes, pRes] = await Promise.all([
        api.get('/discounts'),
        api.get('/taxes'),
        api.get('/products'),
      ]);
      setDiscounts(dRes.data);
      setTaxes(tRes.data);
      setProducts(pRes.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSaveDiscount = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...discountForm,
        maxUses: discountForm.maxUses ? Number(discountForm.maxUses) : null,
        targetProductId: discountForm.targetProductId || null,
      };
      if (editingDiscount) {
        await api.put(`/discounts/${editingDiscount.id}`, payload);
      } else {
        await api.post('/discounts', payload);
      }
      setShowDiscountModal(false);
      setEditingDiscount(null);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleToggleDiscountArchive = async (id, currentActive) => {
    try {
      await api.patch(`/discounts/${id}/archive?active=${!currentActive}`);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleSaveTax = async (e) => {
    e.preventDefault();
    try {
      if (editingTax) {
        await api.put(`/taxes/${editingTax.id}`, taxForm);
      } else {
        await api.post('/taxes', taxForm);
      }
      setShowTaxModal(false);
      setEditingTax(null);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleToggleTaxArchive = async (id, currentActive) => {
    try {
      await api.patch(`/taxes/${id}/archive?active=${!currentActive}`);
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
            Tax Rules &amp; Promotional Discounts
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
            Admin Configuration: Configure automated VAT/GST rates and rule-based percentage or fixed discount promotions.
          </p>
        </div>

        {isAdmin && (
          <div>
            {activeTab === 'discounts' ? (
              <button
                className="btn btn-primary"
                onClick={() => {
                  setEditingDiscount(null);
                  setDiscountForm({
                    code: '',
                    discountType: 'percentage',
                    value: '',
                    minPurchase: '0.00',
                    minQuantity: 0,
                    startDate: new Date().toISOString().split('T')[0],
                    endDate: new Date(Date.now() + 180 * 86400000).toISOString().split('T')[0],
                    maxUses: '',
                    targetScope: 'subscription',
                    targetProductId: '',
                  });
                  setShowDiscountModal(true);
                }}
              >
                <Plus size={16} />
                <span>Create Discount Rule</span>
              </button>
            ) : (
              <button
                className="btn btn-primary"
                onClick={() => {
                  setEditingTax(null);
                  setTaxForm({ name: '', rate: '' });
                  setShowTaxModal(true);
                }}
              >
                <Plus size={16} />
                <span>Add Tax Rule</span>
              </button>
            )}
          </div>
        )}
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '8px', borderBottom: '1px solid #e2e8f0', marginBottom: '24px' }}>
        <button
          onClick={() => setActiveTab('discounts')}
          style={{
            padding: '10px 20px',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'discounts' ? '2px solid #4f46e5' : '2px solid transparent',
            color: activeTab === 'discounts' ? '#4f46e5' : '#64748b',
            fontWeight: activeTab === 'discounts' ? '700' : '500',
            cursor: 'pointer',
            fontSize: '0.92rem',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <Tag size={18} />
          <span>Discounts &amp; Promos ({discounts.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('taxes')}
          style={{
            padding: '10px 20px',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'taxes' ? '2px solid #4f46e5' : '2px solid transparent',
            color: activeTab === 'taxes' ? '#4f46e5' : '#64748b',
            fontWeight: activeTab === 'taxes' ? '700' : '500',
            cursor: 'pointer',
            fontSize: '0.92rem',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <Percent size={18} />
          <span>Taxes (GST / VAT) ({taxes.length})</span>
        </button>
      </div>

      {/* Discounts Tab */}
      {activeTab === 'discounts' && (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Promo Code</th>
                  <th>Type</th>
                  <th>Value</th>
                  <th>Conditions</th>
                  <th>Valid Period</th>
                  <th>Usage Limit</th>
                  <th>Scope</th>
                  <th>Status</th>
                  {isAdmin && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {discounts.length === 0 ? (
                  <tr>
                    <td colSpan={9} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                      No discount rules configured yet.
                    </td>
                  </tr>
                ) : (
                  discounts.map((d) => (
                    <tr key={d.id}>
                      <td style={{ fontFamily: 'monospace', fontWeight: '700', color: '#4f46e5' }}>
                        {d.code}
                      </td>
                      <td style={{ textTransform: 'capitalize' }}>{d.discountType}</td>
                      <td style={{ fontWeight: '700', color: '#10b981' }}>
                        {d.discountType === 'percentage' ? `${d.value}%` : `$${d.value}`}
                      </td>
                      <td style={{ fontSize: '0.78rem', color: '#64748b' }}>
                        Min spend: ${d.minPurchase || '0'} | Min qty: {d.minQuantity || '0'}
                      </td>
                      <td style={{ fontSize: '0.78rem', color: '#475569' }}>
                        {d.startDate} to {d.endDate}
                      </td>
                      <td>
                        <span style={{ fontWeight: '600' }}>{d.usedCount}</span>
                        {d.maxUses ? ` / ${d.maxUses}` : ' (Unlimited)'}
                      </td>
                      <td>
                        <span className="badge badge-secondary" style={{ textTransform: 'capitalize' }}>
                          {d.targetScope}
                        </span>
                      </td>
                      <td>
                        <span className={`badge badge-${d.isActive ? 'active' : 'cancelled'}`}>
                          {d.isActive ? 'Active' : 'Archived'}
                        </span>
                      </td>
                      {isAdmin && (
                        <td>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => {
                                setEditingDiscount(d);
                                setDiscountForm({
                                  code: d.code,
                                  discountType: d.discountType,
                                  value: d.value,
                                  minPurchase: d.minPurchase || '0.00',
                                  minQuantity: d.minQuantity || 0,
                                  startDate: d.startDate,
                                  endDate: d.endDate,
                                  maxUses: d.maxUses || '',
                                  targetScope: d.targetScope || 'subscription',
                                  targetProductId: d.targetProductId || '',
                                });
                                setShowDiscountModal(true);
                              }}
                            >
                              <Edit2 size={13} />
                            </button>
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => handleToggleDiscountArchive(d.id, d.isActive)}
                              title={d.isActive ? 'Archive discount' : 'Restore discount'}
                            >
                              {d.isActive ? <Archive size={13} /> : <RotateCcw size={13} />}
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Taxes Tab */}
      {activeTab === 'taxes' && (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Tax Rule Name</th>
                  <th>Applicable Rate (%)</th>
                  <th>Status</th>
                  {isAdmin && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {taxes.length === 0 ? (
                  <tr>
                    <td colSpan={4} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                      No tax rules configured yet.
                    </td>
                  </tr>
                ) : (
                  taxes.map((t) => (
                    <tr key={t.id}>
                      <td style={{ fontWeight: '700', color: '#0f172a' }}>{t.name}</td>
                      <td style={{ fontWeight: '800', color: '#4f46e5' }}>{t.rate}%</td>
                      <td>
                        <span className={`badge badge-${t.isActive ? 'active' : 'cancelled'}`}>
                          {t.isActive ? 'Active' : 'Archived'}
                        </span>
                      </td>
                      {isAdmin && (
                        <td>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => {
                                setEditingTax(t);
                                setTaxForm({ name: t.name, rate: t.rate });
                                setShowTaxModal(true);
                              }}
                            >
                              <Edit2 size={13} />
                            </button>
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => handleToggleTaxArchive(t.id, t.isActive)}
                              title={t.isActive ? 'Archive tax' : 'Restore tax'}
                            >
                              {t.isActive ? <Archive size={13} /> : <RotateCcw size={13} />}
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Discount Modal */}
      {showDiscountModal && (
        <div className="modal-backdrop">
          <div className="modal-content" style={{ maxWidth: '650px' }}>
            <div className="modal-header">
              <div className="modal-title">{editingDiscount ? 'Edit Discount Rule' : 'Create Discount Rule'}</div>
              <button onClick={() => setShowDiscountModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSaveDiscount}>
              <div className="modal-body">
                <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label">Coupon Code (Uppercase)</label>
                    <input
                      type="text"
                      required
                      className="form-control"
                      placeholder="e.g. ODOOBIZ"
                      value={discountForm.code}
                      onChange={(e) => setDiscountForm({ ...discountForm, code: e.target.value.toUpperCase() })}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Discount Type</label>
                    <select
                      className="form-control"
                      value={discountForm.discountType}
                      onChange={(e) => setDiscountForm({ ...discountForm, discountType: e.target.value })}
                    >
                      <option value="percentage">Percentage (%)</option>
                      <option value="fixed">Fixed Amount ($)</option>
                    </select>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label">Discount Value</label>
                    <input
                      type="number"
                      step="0.01"
                      required
                      className="form-control"
                      placeholder="e.g. 20"
                      value={discountForm.value}
                      onChange={(e) => setDiscountForm({ ...discountForm, value: e.target.value })}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Max Allowed Uses (Blank = Unlimited)</label>
                    <input
                      type="number"
                      className="form-control"
                      placeholder="e.g. 100"
                      value={discountForm.maxUses}
                      onChange={(e) => setDiscountForm({ ...discountForm, maxUses: e.target.value })}
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label">Minimum Purchase ($)</label>
                    <input
                      type="number"
                      step="0.01"
                      className="form-control"
                      value={discountForm.minPurchase}
                      onChange={(e) => setDiscountForm({ ...discountForm, minPurchase: e.target.value })}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Minimum Seat/Item Quantity</label>
                    <input
                      type="number"
                      className="form-control"
                      value={discountForm.minQuantity}
                      onChange={(e) => setDiscountForm({ ...discountForm, minQuantity: e.target.value })}
                    />
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label">Start Date</label>
                    <input
                      type="date"
                      required
                      className="form-control"
                      value={discountForm.startDate}
                      onChange={(e) => setDiscountForm({ ...discountForm, startDate: e.target.value })}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">End Date</label>
                    <input
                      type="date"
                      required
                      className="form-control"
                      value={discountForm.endDate}
                      onChange={(e) => setDiscountForm({ ...discountForm, endDate: e.target.value })}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Scope</label>
                  <select
                    className="form-control"
                    value={discountForm.targetScope}
                    onChange={(e) => setDiscountForm({ ...discountForm, targetScope: e.target.value })}
                  >
                    <option value="subscription">Whole Subscription</option>
                    <option value="product">Specific Product</option>
                  </select>
                </div>

                {discountForm.targetScope === 'product' && (
                  <div className="form-group">
                    <label className="form-label">Target Product</label>
                    <select
                      className="form-control"
                      value={discountForm.targetProductId}
                      onChange={(e) => setDiscountForm({ ...discountForm, targetProductId: e.target.value })}
                    >
                      <option value="">Select target product</option>
                      {products.map((p) => (
                        <option key={p.id} value={p.id}>{p.name}</option>
                      ))}
                    </select>
                  </div>
                )}
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowDiscountModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Discount Rule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Tax Modal */}
      {showTaxModal && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">{editingTax ? 'Edit Tax Rule' : 'Create Tax Rule'}</div>
              <button onClick={() => setShowTaxModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSaveTax}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Tax Name</label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    placeholder="e.g. GST (18%)"
                    value={taxForm.name}
                    onChange={(e) => setTaxForm({ ...taxForm, name: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Tax Rate (%)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    className="form-control"
                    placeholder="e.g. 18.00"
                    value={taxForm.rate}
                    onChange={(e) => setTaxForm({ ...taxForm, rate: e.target.value })}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowTaxModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Tax Rule
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default DiscountsTaxes;
