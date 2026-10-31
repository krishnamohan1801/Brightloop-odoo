import React, { useState, useEffect } from 'react';
import api from '../api';
import { useAuth } from '../context/AuthContext';
import {
  Users,
  Plus,
  Edit2,
  Mail,
  Phone,
  Shield,
  Eye,
  X,
  Repeat,
  Receipt,
  UserPlus
} from 'lucide-react';

const Customers = () => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'admin';
  const isAdminOrStaff = isAdmin || user?.role === 'staff';

  const [activeTab, setActiveTab] = useState('customers');
  const [customers, setCustomers] = useState([]);
  const [staffList, setStaffList] = useState([]);
  const [loading, setLoading] = useState(true);

  // 360 Drawer
  const [selectedCustomer360, setSelectedCustomer360] = useState(null);
  const [loading360, setLoading360] = useState(false);

  // Add/Edit Customer Modal
  const [showCustomerModal, setShowCustomerModal] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState(null);
  const [customerForm, setCustomerForm] = useState({
    fullName: '',
    email: '',
    phone: '',
    passwordHash: '',
  });

  // Add Staff Modal (Admin only)
  const [showStaffModal, setShowStaffModal] = useState(false);
  const [staffForm, setStaffForm] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
  });

  const fetchData = async () => {
    try {
      setLoading(true);
      const custRes = await api.get('/users/customers');
      setCustomers(custRes.data);
      if (isAdmin) {
        const staffRes = await api.get('/users/staff');
        setStaffList(staffRes.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [isAdmin]);

  const handleOpen360 = async (customerId) => {
    try {
      setLoading360(true);
      const res = await api.get(`/users/customers/${customerId}/360`);
      setSelectedCustomer360(res.data);
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    } finally {
      setLoading360(false);
    }
  };

  const handleSaveCustomer = async (e) => {
    e.preventDefault();
    try {
      if (editingCustomer) {
        await api.put(`/users/customers/${editingCustomer.id}`, customerForm);
      } else {
        await api.post('/users/customers', customerForm);
      }
      setShowCustomerModal(false);
      setEditingCustomer(null);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    }
  };

  const handleCreateStaff = async (e) => {
    e.preventDefault();
    try {
      await api.post('/users/staff', staffForm);
      setShowStaffModal(false);
      setStaffForm({ fullName: '', email: '', phone: '', password: '' });
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
            Customer Management &amp; 360 View
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
            Part 2: Manage customer profiles, track subscriptions, view unpaid balances, and manage internal staff.
          </p>
        </div>

        {isAdminOrStaff && (
          <div style={{ display: 'flex', gap: '12px' }}>
            {isAdmin && activeTab === 'staff' ? (
              <button
                className="btn btn-primary"
                onClick={() => {
                  setStaffForm({ fullName: '', email: '', phone: '', password: '' });
                  setShowStaffModal(true);
                }}
              >
                <UserPlus size={16} />
                <span>Add Staff Member</span>
              </button>
            ) : (
              <button
                className="btn btn-primary"
                onClick={() => {
                  setEditingCustomer(null);
                  setCustomerForm({ fullName: '', email: '', phone: '', passwordHash: 'Customer@1234' });
                  setShowCustomerModal(true);
                }}
              >
                <Plus size={16} />
                <span>New Customer</span>
              </button>
            )}
          </div>
        )}
      </div>

      {/* Tabs */}
      {isAdmin && (
        <div style={{ display: 'flex', gap: '8px', borderBottom: '1px solid #e2e8f0', marginBottom: '24px' }}>
          <button
            onClick={() => setActiveTab('customers')}
            style={{
              padding: '10px 20px',
              background: 'none',
              border: 'none',
              borderBottom: activeTab === 'customers' ? '2px solid #4f46e5' : '2px solid transparent',
              color: activeTab === 'customers' ? '#4f46e5' : '#64748b',
              fontWeight: activeTab === 'customers' ? '700' : '500',
              cursor: 'pointer',
              fontSize: '0.92rem',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <Users size={18} />
            <span>Customers ({customers.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('staff')}
            style={{
              padding: '10px 20px',
              background: 'none',
              border: 'none',
              borderBottom: activeTab === 'staff' ? '2px solid #4f46e5' : '2px solid transparent',
              color: activeTab === 'staff' ? '#4f46e5' : '#64748b',
              fontWeight: activeTab === 'staff' ? '700' : '500',
              cursor: 'pointer',
              fontSize: '0.92rem',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <Shield size={18} />
            <span>Internal Staff ({staffList.length})</span>
          </button>
        </div>
      )}

      {/* Customer List */}
      {activeTab === 'customers' && (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Customer Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Joined Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {customers.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                      No customers registered yet.
                    </td>
                  </tr>
                ) : (
                  customers.map((c) => (
                    <tr key={c.id}>
                      <td style={{ fontWeight: '700', color: '#0f172a' }}>{c.fullName}</td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#475569' }}>
                          <Mail size={14} color="#94a3b8" />
                          <span>{c.email}</span>
                        </div>
                      </td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#64748b' }}>
                          <Phone size={14} color="#94a3b8" />
                          <span>{c.phone || '—'}</span>
                        </div>
                      </td>
                      <td>
                        <span className="badge badge-secondary">Customer</span>
                      </td>
                      <td style={{ fontSize: '0.8rem', color: '#64748b' }}>
                        {c.createdAt ? c.createdAt.substring(0, 10) : '—'}
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button
                            className="btn btn-outline btn-sm"
                            onClick={() => handleOpen360(c.id)}
                            style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
                          >
                            <Eye size={13} color="#4f46e5" />
                            <span>Customer 360</span>
                          </button>
                          {isAdminOrStaff && (
                            <button
                              className="btn btn-outline btn-sm"
                              onClick={() => {
                                setEditingCustomer(c);
                                setCustomerForm({
                                  fullName: c.fullName,
                                  email: c.email,
                                  phone: c.phone || '',
                                  passwordHash: '',
                                });
                                setShowCustomerModal(true);
                              }}
                            >
                              <Edit2 size={13} />
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
      )}

      {/* Staff List (Admin Only) */}
      {activeTab === 'staff' && (
        <div className="card">
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Staff Member</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {staffList.map((s) => (
                  <tr key={s.id}>
                    <td style={{ fontWeight: '700', color: '#0f172a' }}>{s.fullName}</td>
                    <td>{s.email}</td>
                    <td>{s.phone || '—'}</td>
                    <td>
                      <span className="badge badge-confirmed">Staff Member</span>
                    </td>
                    <td>
                      <span className="badge badge-active">Active</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Customer 360 Drawer / Modal */}
      {selectedCustomer360 && (
        <div className="modal-backdrop">
          <div className="modal-content" style={{ maxWidth: '800px' }}>
            <div className="modal-header">
              <div>
                <div className="modal-title">Customer 360° Profile</div>
                <div style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '2px' }}>
                  {selectedCustomer360.customer?.fullName} ({selectedCustomer360.customer?.email})
                </div>
              </div>
              <button onClick={() => setSelectedCustomer360(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <div className="modal-body">
              {/* Profile Bar */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px', background: '#f8fafc', padding: '16px', borderRadius: '10px', marginBottom: '24px' }}>
                <div>
                  <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: '700' }}>Phone</div>
                  <div style={{ fontWeight: '600', marginTop: '2px' }}>{selectedCustomer360.customer?.phone || 'Not provided'}</div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: '700' }}>Active Subscriptions</div>
                  <div style={{ fontWeight: '700', color: '#4f46e5', marginTop: '2px' }}>
                    {selectedCustomer360.subscriptions?.filter((s) => s.status === 'active').length || 0}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: '#64748b', fontWeight: '700' }}>Total Invoices</div>
                  <div style={{ fontWeight: '700', color: '#0f172a', marginTop: '2px' }}>
                    {selectedCustomer360.invoices?.length || 0}
                  </div>
                </div>
              </div>

              {/* Subscriptions section */}
              <div style={{ marginBottom: '24px' }}>
                <h3 style={{ fontSize: '1rem', fontWeight: '700', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Repeat size={16} color="#4f46e5" />
                  <span>Subscriptions &amp; Quotations ({selectedCustomer360.subscriptions?.length || 0})</span>
                </h3>
                <div className="table-container">
                  <table>
                    <thead>
                      <tr>
                        <th>Subscription #</th>
                        <th>Plan</th>
                        <th>Interval</th>
                        <th>Next Billing</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedCustomer360.subscriptions?.length === 0 ? (
                        <tr><td colSpan={5} style={{ textAlign: 'center', color: '#94a3b8' }}>No subscriptions found.</td></tr>
                      ) : (
                        selectedCustomer360.subscriptions?.map((sub) => (
                          <tr key={sub.id}>
                            <td style={{ fontFamily: 'monospace', fontWeight: '600' }}>{sub.subscriptionNumber}</td>
                            <td>{sub.planName}</td>
                            <td style={{ textTransform: 'capitalize' }}>{sub.billingInterval}</td>
                            <td>{sub.nextBillingDate}</td>
                            <td>
                              <span className={`badge badge-${sub.status === 'active' ? 'active' : sub.status === 'quotation' ? 'warning' : 'draft'}`}>
                                {sub.status}
                              </span>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Invoices section */}
              <div>
                <h3 style={{ fontSize: '1rem', fontWeight: '700', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Receipt size={16} color="#4f46e5" />
                  <span>Invoices &amp; Balances ({selectedCustomer360.invoices?.length || 0})</span>
                </h3>
                <div className="table-container">
                  <table>
                    <thead>
                      <tr>
                        <th>Invoice #</th>
                        <th>Due Date</th>
                        <th>Total</th>
                        <th>Paid</th>
                        <th>Balance</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedCustomer360.invoices?.length === 0 ? (
                        <tr><td colSpan={6} style={{ textAlign: 'center', color: '#94a3b8' }}>No invoices issued yet.</td></tr>
                      ) : (
                        selectedCustomer360.invoices?.map((inv) => (
                          <tr key={inv.id}>
                            <td style={{ fontFamily: 'monospace', fontWeight: '600' }}>{inv.invoiceNumber}</td>
                            <td>{inv.dueDate}</td>
                            <td style={{ fontWeight: '700' }}>${inv.totalAmount}</td>
                            <td style={{ color: '#10b981' }}>${inv.amountPaid || '0.00'}</td>
                            <td style={{ fontWeight: '700', color: inv.balanceDue > 0 ? '#ef4444' : '#10b981' }}>
                              ${inv.balanceDue || '0.00'}
                            </td>
                            <td>
                              <span className={`badge badge-${inv.status === 'paid' ? 'paid' : inv.isOverdue ? 'overdue' : 'confirmed'}`}>
                                {inv.isOverdue ? 'Overdue' : inv.status}
                              </span>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setSelectedCustomer360(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add/Edit Customer Modal */}
      {showCustomerModal && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">{editingCustomer ? 'Edit Customer' : 'Add New Customer'}</div>
              <button onClick={() => setShowCustomerModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSaveCustomer}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Full Name / Company Name</label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    placeholder="e.g. Acme Corp (John Doe)"
                    value={customerForm.fullName}
                    onChange={(e) => setCustomerForm({ ...customerForm, fullName: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Email Address</label>
                  <input
                    type="email"
                    required
                    className="form-control"
                    placeholder="contact@acme.com"
                    value={customerForm.email}
                    onChange={(e) => setCustomerForm({ ...customerForm, email: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Phone Number</label>
                  <input
                    type="tel"
                    className="form-control"
                    placeholder="+1 555-0199"
                    value={customerForm.phone}
                    onChange={(e) => setCustomerForm({ ...customerForm, phone: e.target.value })}
                  />
                </div>

                {!editingCustomer && (
                  <div className="form-group">
                    <label className="form-label">Initial Password</label>
                    <input
                      type="text"
                      className="form-control"
                      value={customerForm.passwordHash}
                      onChange={(e) => setCustomerForm({ ...customerForm, passwordHash: e.target.value })}
                    />
                    <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '4px' }}>
                      Defaults to Customer@1234. Customer can change password via email reset.
                    </div>
                  </div>
                )}
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowCustomerModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Customer
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Staff Modal (Admin only) */}
      {showStaffModal && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <div className="modal-header">
              <div className="modal-title">Add Internal Staff Member</div>
              <button onClick={() => setShowStaffModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleCreateStaff}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Staff Member Full Name</label>
                  <input
                    type="text"
                    required
                    className="form-control"
                    placeholder="e.g. Sarah Jenkins"
                    value={staffForm.fullName}
                    onChange={(e) => setStaffForm({ ...staffForm, fullName: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Work Email</label>
                  <input
                    type="email"
                    required
                    className="form-control"
                    placeholder="sarah@brightloop.com"
                    value={staffForm.email}
                    onChange={(e) => setStaffForm({ ...staffForm, email: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Phone</label>
                  <input
                    type="tel"
                    className="form-control"
                    placeholder="+1 555-0182"
                    value={staffForm.phone}
                    onChange={(e) => setStaffForm({ ...staffForm, phone: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Staff Account Password</label>
                  <input
                    type="password"
                    required
                    className="form-control"
                    placeholder="Password (>8 chars, 1 uppercase, 1 lowercase, 1 special)"
                    value={staffForm.password}
                    onChange={(e) => setStaffForm({ ...staffForm, password: e.target.value })}
                  />
                  <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '4px' }}>
                    Password must be &gt;8 characters with uppercase, lowercase, and special character.
                  </div>
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowStaffModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Create Staff Account
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Customers;
