import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard,
  Package,
  Users,
  FileSpreadsheet,
  Repeat,
  Receipt,
  CreditCard,
  Percent,
  History,
  ShieldCheck,
  LifeBuoy
} from 'lucide-react';

const Sidebar = () => {
  const { user } = useAuth();
  const isCustomer = user?.role === 'customer';

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="brand-logo">B</div>
        <div className="brand-text">
          <h1>Brightloop</h1>
          <p>Subscription OS</p>
        </div>
      </div>

      <nav className="sidebar-nav">
        {!isCustomer ? (
          <>
            <div className="nav-section-title">Core Management</div>
            <NavLink to="/" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <LayoutDashboard size={18} />
              <span>Finance Dashboard</span>
            </NavLink>

            <NavLink to="/products-plans" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Package size={18} />
              <span>Products & Plans</span>
            </NavLink>

            <div className="nav-section-title">Sales & Lifecycle</div>
            <NavLink to="/customers" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Users size={18} />
              <span>Customers & 360</span>
            </NavLink>

            <NavLink to="/quotations" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <FileSpreadsheet size={18} />
              <span>Quotations</span>
            </NavLink>

            <NavLink to="/subscriptions" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Repeat size={18} />
              <span>Subscriptions</span>
            </NavLink>

            <div className="nav-section-title">Finance & Billing</div>
            <NavLink to="/invoices" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Receipt size={18} />
              <span>Invoices & Billing</span>
            </NavLink>

            <NavLink to="/payment-simulator" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <CreditCard size={18} />
              <span>Payment Simulator</span>
            </NavLink>

            <NavLink to="/discounts-taxes" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Percent size={18} />
              <span>Taxes & Discounts</span>
            </NavLink>

            <div className="nav-section-title">Compliance & Security</div>
            <NavLink to="/audit-logs" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <History size={18} />
              <span>Audit Trail Logs</span>
            </NavLink>
          </>
        ) : (
          <>
            <div className="nav-section-title">Customer Portal</div>
            <NavLink to="/" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <LayoutDashboard size={18} />
              <span>My Portal Overview</span>
            </NavLink>

            <NavLink to="/quotations" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <FileSpreadsheet size={18} />
              <span>Quotation Offers</span>
            </NavLink>

            <NavLink to="/subscriptions" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Repeat size={18} />
              <span>My Subscriptions</span>
            </NavLink>

            <NavLink to="/invoices" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Receipt size={18} />
              <span>My Invoices & Receipts</span>
            </NavLink>

            <NavLink to="/payment-simulator" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <CreditCard size={18} />
              <span>Pay Online (Demo)</span>
            </NavLink>
          </>
        )}
      </nav>

      <div style={{ padding: '16px 20px', borderTop: '1px solid rgba(255, 255, 255, 0.08)', fontSize: '0.75rem', color: '#64748b' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <ShieldCheck size={16} color="#10b981" />
          <span>PostgreSQL 14+ JDBC SLA &lt; 2s</span>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
