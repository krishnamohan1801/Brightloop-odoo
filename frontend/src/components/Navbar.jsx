import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Bell, LogOut, CheckCircle2, AlertCircle, Info, RefreshCw } from 'lucide-react';
import api from '../api';

const Navbar = ({ title }) => {
  const { user, logout } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  const fetchNotifications = () => {
    api.get('/notifications')
      .then((res) => {
        setNotifications(res.data);
        setUnreadCount(res.data.filter((n) => !n.isRead).length);
      })
      .catch(() => {});
  };

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 15000);
    return () => clearInterval(interval);
  }, []);

  const handleMarkAllRead = () => {
    api.post('/notifications/mark-all-read').then(() => {
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    });
  };

  return (
    <header className="top-navbar">
      <div className="top-navbar-left">
        <h2 style={{ fontSize: '1.25rem', fontWeight: '700', letterSpacing: '-0.02em', color: '#0f172a' }}>
          {title || 'Brightloop Software'}
        </h2>
      </div>

      <div className="top-navbar-right">
        {/* Notifications Popover */}
        <div style={{ position: 'relative' }}>
          <button
            onClick={() => setShowNotifications(!showNotifications)}
            style={{
              position: 'relative',
              background: '#f1f5f9',
              border: 'none',
              borderRadius: '50%',
              width: '40px',
              height: '40px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              color: '#475569',
            }}
          >
            <Bell size={18} />
            {unreadCount > 0 && (
              <span
                style={{
                  position: 'absolute',
                  top: '2px',
                  right: '2px',
                  background: '#ef4444',
                  color: 'white',
                  fontSize: '0.65rem',
                  fontWeight: '700',
                  borderRadius: '50%',
                  width: '18px',
                  height: '18px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                {unreadCount}
              </span>
            )}
          </button>

          {showNotifications && (
            <div
              style={{
                position: 'absolute',
                top: '50px',
                right: '0',
                width: '360px',
                background: '#ffffff',
                borderRadius: '12px',
                boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.15)',
                border: '1px solid #e2e8f0',
                padding: '16px',
                zIndex: 100,
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <span style={{ fontWeight: '700', fontSize: '0.9rem' }}>System Alerts & Activity</span>
                {unreadCount > 0 && (
                  <button
                    onClick={handleMarkAllRead}
                    style={{ background: 'none', border: 'none', color: '#4f46e5', fontSize: '0.75rem', fontWeight: '600', cursor: 'pointer' }}
                  >
                    Mark all read
                  </button>
                )}
              </div>

              <div style={{ maxHeight: '280px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {notifications.length === 0 ? (
                  <p style={{ fontSize: '0.82rem', color: '#64748b', textAlign: 'center', padding: '16px 0' }}>No notifications yet.</p>
                ) : (
                  notifications.map((n) => (
                    <div
                      key={n.id}
                      style={{
                        padding: '10px 12px',
                        borderRadius: '8px',
                        background: n.isRead ? '#f8fafc' : '#eff6ff',
                        border: '1px solid',
                        borderColor: n.isRead ? '#e2e8f0' : '#bfdbfe',
                        fontSize: '0.8rem',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: '600', color: '#1e293b' }}>
                        {n.type === 'success' ? <CheckCircle2 size={14} color="#10b981" /> : n.type === 'alert' ? <AlertCircle size={14} color="#ef4444" /> : <Info size={14} color="#3b82f6" />}
                        {n.title}
                      </div>
                      <div style={{ color: '#475569', marginTop: '3px', fontSize: '0.76rem' }}>{n.message}</div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* User Badge & Profile */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.88rem', fontWeight: '700', color: '#0f172a' }}>{user?.fullName || user?.email}</div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '2px' }}>
              <span className={`badge badge-${user?.role === 'admin' ? 'paid' : user?.role === 'staff' ? 'confirmed' : 'draft'}`}>
                {user?.role}
              </span>
            </div>
          </div>

          <button
            onClick={logout}
            className="btn btn-outline btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#64748b' }}
            title="Sign out"
          >
            <LogOut size={15} />
            <span>Sign Out</span>
          </button>
        </div>
      </div>
    </header>
  );
};

export default Navbar;
