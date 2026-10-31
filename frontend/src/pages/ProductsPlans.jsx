import React, { useState, useEffect } from 'react';
import api from '../api';
import { useAuth } from '../context/AuthContext';
import {
  Package,
  Plus,
  Edit2,
  Archive,
  RotateCcw,
  Tag,
  Calendar,
  Layers,
  CheckCircle2,
  X,
  Trash2
} from 'lucide-react';

const ProductsPlans = () => {
  const { user } = useAuth();
  const isAdminOrStaff = user?.role === 'admin' || user?.role === 'staff';

  const [activeTab, setActiveTab] = useState('products');
  const [products, setProducts] = useState([]);
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modals state
  const [showProductModal, setShowProductModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [productForm, setProductForm] = useState({
    name: '',
    productType: 'software',
    salesPrice: '',
    costPrice: '',
    notes: '',
  });

  const [showVariantModal, setShowVariantModal] = useState(false);
  const [selectedProductForVariant, setSelectedProductForVariant] = useState(null);
  const [variantForm, setVariantForm] = useState({
    variantName: '',
    extraPrice: '',
    sku: '',
  });

  const [showPlanModal, setShowPlanModal] = useState(false);
  const [editingPlan, setEditingPlan] = useState(null);
  const [planForm, setPlanForm] = useState({
    name: '',
    billingInterval: 'monthly',
    price: '',
    minQuantity: 1,
    startDate: new Date().toISOString().split('T')[0],
    endDate: '',
    autoClose: false,
    canPause: true,
  });

  const fetchData = async () => {
    try {
      setLoading(true);
      const [prodRes, planRes] = await Promise.all([
        api.get('/products'),
        api.get('/plans'),
      ]);
      setProducts(prodRes.data);
      setPlans(planRes.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Product actions
  const handleSaveProduct = async (e) => {
    e.preventDefault();
    try {
      if (editingProduct) {
        await api.put(`/products/${editingProduct.id}`, productForm);
      } else {
        await api.post('/products', productForm);
      }
      setShowProductModal(false);
      setEditingProduct(null);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleToggleProductArchive = async (id, currentActive) => {
    try {
      await api.patch(`/products/${id}/archive?active=${!currentActive}`);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  // Variant actions
  const handleAddVariant = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/products/${selectedProductForVariant.id}/variants`, variantForm);
      setShowVariantModal(false);
      setVariantForm({ variantName: '', extraPrice: '', sku: '' });
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleDeleteVariant = async (variantId) => {
    if (!window.confirm('Delete this variant?')) return;
    try {
      await api.delete(`/products/variants/${variantId}`);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  // Plan actions
  const handleSavePlan = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...planForm,
        endDate: planForm.endDate || null,
      };
      if (editingPlan) {
        await api.put(`/plans/${editingPlan.id}`, payload);
      } else {
        await api.post('/plans', payload);
      }
      setShowPlanModal(false);
      setEditingPlan(null);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleTogglePlanArchive = async (id, currentActive) => {
    try {
      await api.patch(`/plans/${id}/archive?active=${!currentActive}`);
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
            Product Catalog &amp; Billing Plans
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
            Part 1: Define what Brightloop sells, product variants, and recurring billing frequency schedules.
          </p>
        </div>

        {isAdminOrStaff && (
          <div style={{ display: 'flex', gap: '12px' }}>
            {activeTab === 'products' ? (
              <button
                className="btn btn-primary"
                onClick={() => {
                  setEditingProduct(null);
                  setProductForm({ name: '', productType: 'software', salesPrice: '', costPrice: '', notes: '' });
                  setShowProductModal(true);
                }}
              >
                <Plus size={16} />
                <span>Add Product</span>
              </button>
            ) : (
              <button
                className="btn btn-primary"
                onClick={() => {
                  setEditingPlan(null);
                  setPlanForm({
                    name: '',
                    billingInterval: 'monthly',
                    price: '',
                    minQuantity: 1,
                    startDate: new Date().toISOString().split('T')[0],
                    endDate: '',
                    autoClose: false,
                    canPause: true,
                  });
                  setShowPlanModal(true);
                }}
              >
                <Plus size={16} />
                <span>Create Billing Plan</span>
              </button>
            )}
          </div>
        )}
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '8px', borderBottom: '1px solid #e2e8f0', marginBottom: '24px' }}>
        <button
          onClick={() => setActiveTab('products')}
          style={{
            padding: '10px 20px',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'products' ? '2px solid #4f46e5' : '2px solid transparent',
            color: activeTab === 'products' ? '#4f46e5' : '#64748b',
            fontWeight: activeTab === 'products' ? '700' : '500',
            cursor: 'pointer',
            fontSize: '0.92rem',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <Package size={18} />
          <span>Products &amp; Variants ({products.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('plans')}
          style={{
            padding: '10px 20px',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'plans' ? '2px solid #4f46e5' : '2px solid transparent',
            color: activeTab === 'plans' ? '#4f46e5' : '#64748b',
            fontWeight: activeTab === 'plans' ? '700' : '500',
            cursor: 'pointer',
            fontSize: '0.92rem',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <Layers size={18} />
          <span>Billing Plans ({plans.length})</span>
        </button>
      </div>

      {/* Products Tab Content */}
      {activeTab === 'products' && (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Product Name</th>
                  <th>Type</th>
                  <th>Sales Price</th>
                  <th>Cost Price</th>
                  <th>Variants</th>
                  <th>Status</th>
                  {isAdminOrStaff && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {products.length === 0 ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                      No products configured yet.
                    </td>
                  </tr>
                ) : (
                  products.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <div style={{ fontWeight: '700', color: '#0f172a' }}>{p.name}</div>
                        {p.notes && <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '2px' }}>{p.notes}</div>}
                      </td>
                      <td>
                        <span className={`badge badge-${p.productType === 'software' ? 'info' : p.productType === 'addon' ? 'warning' : 'draft'}`}>
                          {p.productType}
                        </span>
                      </td>
                      <td style={{ fontWeight: '700', color: '#0f172a' }}>${p.salesPrice}</td>
                      <td style={{ color: '#64748b' }}>${p.costPrice || '0.00'}</td>
                      <td>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', alignItems: 'center' }}>
                          {p.variants && p.variants.length > 0 ? (
                            p.variants.map((v) => (
                              <span
                                key={v.id}
                                className="badge badge-secondary"
                                style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', textTransform: 'none' }}
                              >
                                <span>{v.variantName} (+${v.extraPrice})</span>
                                {isAdminOrStaff && (
                                  <button
                                    onClick={() => handleDeleteVariant(v.id)}
                                    style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, display: 'flex' }}
                                    title="Delete variant"
                                  >
                                    <X size={12} color="#94a3b8" />
                                  </button>
                                )}
                              </span>
                            ))
                          ) : (
                            <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>None</span>
                          )}

                          {isAdminOrStaff && (
                            <button
                              onClick={() => {
                                setSelectedProductForVariant(p);
                                setVariantForm({ variantName: '', extraPrice: '', sku: '' });
                                setShowVariantModal(true);
                              }}
                              className="btn btn-outline btn-sm"
                              style={{ padding: '2px 6px', fontSize: '0.7rem' }}
                            >
                              + Add Variant
                            </button>
                          )}
                        </div>
                      </td>
                      <td>
                        <span className={`badge badge-${p.isActive ? 'active' : 'cancelled'}`}>
                          {p.isActive ? 'Active' : 'Archived'}
                        </span>
                      </td>
                      {isAdminOrStaff && (
                        <td>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => {
                                setEditingProduct(p);
                                setProductForm({
                                  name: p.name,
                                  productType: p.productType,
                                  salesPrice: p.salesPrice,
                                  costPrice: p.costPrice,
                                  notes: p.notes || '',
                                });
                                setShowProductModal(true);
                              }}
                            >
                              <Edit2 size={13} />
                              <span>Edit</span>
                            </button>
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => handleToggleProductArchive(p.id, p.isActive)}
                              title={p.isActive ? 'Archive Product' : 'Restore Product'}
                            >
                              {p.isActive ? <Archive size={13} /> : <RotateCcw size={13} />}
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

      {/* Plans Tab Content */}
      {activeTab === 'plans' && (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Plan Name</th>
                  <th>Billing Interval</th>
                  <th>Base Price</th>
                  <th>Min Qty</th>
                  <th>Availability</th>
                  <th>Lifecycle Rules</th>
                  <th>Status</th>
                  {isAdminOrStaff && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {plans.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                      No billing plans configured yet.
                    </td>
                  </tr>
                ) : (
                  plans.map((pl) => (
                    <tr key={pl.id}>
                      <td style={{ fontWeight: '700', color: '#0f172a' }}>{pl.name}</td>
                      <td>
                        <span className="badge badge-info" style={{ textTransform: 'uppercase' }}>
                          {pl.billingInterval}
                        </span>
                      </td>
                      <td style={{ fontWeight: '700', color: '#4f46e5' }}>${pl.price}</td>
                      <td>{pl.minQuantity} unit(s)</td>
                      <td style={{ fontSize: '0.8rem', color: '#64748b' }}>
                        From {pl.startDate} {pl.endDate ? `to ${pl.endDate}` : '(Ongoing)'}
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '6px' }}>
                          {pl.autoClose && <span className="badge badge-warning">Auto-Close</span>}
                          {pl.canPause ? <span className="badge badge-success">Pausable</span> : <span className="badge badge-draft">No Pause</span>}
                        </div>
                      </td>
                      <td>
                        <span className={`badge badge-${pl.isActive ? 'active' : 'cancelled'}`}>
                          {pl.isActive ? 'Active' : 'Archived'}
                        </span>
                      </td>
                      {isAdminOrStaff && (
                        <td>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => {
                                setEditingPlan(pl);
                                setPlanForm({
                                  name: pl.name,
                                  billingInterval: pl.billingInterval,
                                  price: pl.price,
                                  minQuantity: pl.minQuantity,
                                  startDate: pl.startDate,
                                  endDate: pl.endDate || '',
                                  autoClose: pl.autoClose,
                                  canPause: pl.canPause,
                                });
                                setShowPlanModal(true);
                              }}
                            >
                              <Edit2 size={13} />
                              <span>Edit</span>
                            </button>
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => handleTogglePlanArchive(pl.id, pl.isActive)}
                              title={pl.isActive ? 'Archive Plan' : 'Restore Plan'}
                            >
                              {pl.isActive ? <Archive size={13} /> : <RotateCcw size={13} />}
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

      {/* Product Modal */}
      {showProductModal && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">{editingProduct ? 'Edit Product' : 'Add New Product'}</div>
              <button onClick={() => setShowProductModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSaveProduct}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Product Name</label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    placeholder="e.g. Brightloop Business Suite"
                    value={productForm.name}
                    onChange={(e) => setProductForm({ ...productForm, name: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Product Type</label>
                  <select
                    className="form-control"
                    value={productForm.productType}
                    onChange={(e) => setProductForm({ ...productForm, productType: e.target.value })}
                  >
                    <option value="software">Software</option>
                    <option value="addon">Add-on</option>
                    <option value="service">Service</option>
                  </select>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label">Sales Price ($)</label>
                    <input
                      type="number"
                      step="0.01"
                      required
                      className="form-control"
                      placeholder="499.00"
                      value={productForm.salesPrice}
                      onChange={(e) => setProductForm({ ...productForm, salesPrice: e.target.value })}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Cost Price ($)</label>
                    <input
                      type="number"
                      step="0.01"
                      className="form-control"
                      placeholder="120.00"
                      value={productForm.costPrice}
                      onChange={(e) => setProductForm({ ...productForm, costPrice: e.target.value })}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Notes &amp; Description</label>
                  <textarea
                    rows={3}
                    className="form-control"
                    placeholder="Key specifications, SLA features, etc."
                    value={productForm.notes}
                    onChange={(e) => setProductForm({ ...productForm, notes: e.target.value })}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowProductModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Product
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Variant Modal */}
      {showVariantModal && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">Add Variant for {selectedProductForVariant?.name}</div>
              <button onClick={() => setShowVariantModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleAddVariant}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Variant Name (e.g. Odoo Brand, Enterprise Tier)</label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    placeholder="e.g. Odoo Brand Edition"
                    value={variantForm.variantName}
                    onChange={(e) => setVariantForm({ ...variantForm, variantName: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Additional Price ($) - Added to base product price</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    className="form-control"
                    placeholder="e.g. 560.00"
                    value={variantForm.extraPrice}
                    onChange={(e) => setVariantForm({ ...variantForm, extraPrice: e.target.value })}
                  />
                  <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '4px' }}>
                    Per PDF: Choosing the Odoo brand variant adds $560 to the base price.
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">SKU (Stock Keeping Unit)</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="BL-ODOO-560"
                    value={variantForm.sku}
                    onChange={(e) => setVariantForm({ ...variantForm, sku: e.target.value })}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowVariantModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Variant
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Billing Plan Modal */}
      {showPlanModal && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">{editingPlan ? 'Edit Billing Plan' : 'Create Billing Plan'}</div>
              <button onClick={() => setShowPlanModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSavePlan}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Plan Name</label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    placeholder="e.g. Monthly Cloud Standard"
                    value={planForm.name}
                    onChange={(e) => setPlanForm({ ...planForm, name: e.target.value })}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="form-group">
                    <label className="form-label">Billing Interval</label>
                    <select
                      className="form-control"
                      value={planForm.billingInterval}
                      onChange={(e) => setPlanForm({ ...planForm, billingInterval: e.target.value })}
                    >
                      <option value="daily">Daily</option>
                      <option value="weekly">Weekly</option>
                      <option value="monthly">Monthly</option>
                      <option value="yearly">Yearly</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Price ($)</label>
                    <input
                      type="number"
                      step="0.01"
                      required
                      className="form-control"
                      placeholder="499.00"
                      value={planForm.price}
                      onChange={(e) => setPlanForm({ ...planForm, price: e.target.value })}
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
                      value={planForm.startDate}
                      onChange={(e) => setPlanForm({ ...planForm, startDate: e.target.value })}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">End Date (Optional)</label>
                    <input
                      type="date"
                      className="form-control"
                      value={planForm.endDate}
                      onChange={(e) => setPlanForm({ ...planForm, endDate: e.target.value })}
                    />
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '24px', marginTop: '12px' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.85rem' }}>
                    <input
                      type="checkbox"
                      checked={planForm.canPause}
                      onChange={(e) => setPlanForm({ ...planForm, canPause: e.target.checked })}
                    />
                    <span>Customer can pause subscription</span>
                  </label>

                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.85rem' }}>
                    <input
                      type="checkbox"
                      checked={planForm.autoClose}
                      onChange={(e) => setPlanForm({ ...planForm, autoClose: e.target.checked })}
                    />
                    <span>Auto-close when plan ends</span>
                  </label>
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowPlanModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Plan
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductsPlans;
