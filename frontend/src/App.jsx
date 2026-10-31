import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';

import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';

import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

import Dashboard from './pages/Dashboard';
import ProductsPlans from './pages/ProductsPlans';
import Customers from './pages/Customers';
import Quotations from './pages/Quotations';
import Subscriptions from './pages/Subscriptions';
import Invoices from './pages/Invoices';
import PaymentSimulator from './pages/PaymentSimulator';
import DiscountsTaxes from './pages/DiscountsTaxes';
import AuditLogs from './pages/AuditLogs';

const ProtectedLayout = ({ children, title }) => {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#64748b' }}>
        Loading Brightloop Software...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return (
    <div className="app-container">
      <Sidebar />
      <div className="main-content">
        <Navbar title={title} />
        <main className="page-body">
          {children}
        </main>
      </div>
    </div>
  );
};

const PublicRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return null;
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return children;
};

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* Public Auth Routes */}
          <Route path="/login" element={<PublicRoute><Login /></PublicRoute>} />
          <Route path="/register" element={<PublicRoute><Register /></PublicRoute>} />
          <Route path="/forgot-password" element={<PublicRoute><ForgotPassword /></PublicRoute>} />
          <Route path="/reset-password" element={<PublicRoute><ResetPassword /></PublicRoute>} />

          {/* Protected Application Routes */}
          <Route
            path="/"
            element={<ProtectedLayout title="Finance Dashboard"><Dashboard /></ProtectedLayout>}
          />
          <Route
            path="/products-plans"
            element={<ProtectedLayout title="Product Catalog & Billing Plans"><ProductsPlans /></ProtectedLayout>}
          />
          <Route
            path="/customers"
            element={<ProtectedLayout title="Customers & 360 Overview"><Customers /></ProtectedLayout>}
          />
          <Route
            path="/quotations"
            element={<ProtectedLayout title="Quotations & Sales Proposals"><Quotations /></ProtectedLayout>}
          />
          <Route
            path="/subscriptions"
            element={<ProtectedLayout title="Subscription Contracts"><Subscriptions /></ProtectedLayout>}
          />
          <Route
            path="/invoices"
            element={<ProtectedLayout title="Invoices & Recurring Billing"><Invoices /></ProtectedLayout>}
          />
          <Route
            path="/payment-simulator"
            element={<ProtectedLayout title="Dummy Payment Simulator & Refunds"><PaymentSimulator /></ProtectedLayout>}
          />
          <Route
            path="/discounts-taxes"
            element={<ProtectedLayout title="Discounts & Tax Configuration"><DiscountsTaxes /></ProtectedLayout>}
          />
          <Route
            path="/audit-logs"
            element={<ProtectedLayout title="Audit Trail Logs"><AuditLogs /></ProtectedLayout>}
          />

          {/* Catch-all */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
