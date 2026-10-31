import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import api from '../api';
import { Lock, KeyRound, Check, X, AlertCircle, CheckCircle2 } from 'lucide-react';

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const urlToken = searchParams.get('token');
    if (urlToken) {
      setToken(urlToken);
    }
  }, [searchParams]);

  const hasLength = newPassword.length > 8;
  const hasUpper = /[A-Z]/.test(newPassword);
  const hasLower = /[a-z]/.test(newPassword);
  const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(newPassword);
  const isPasswordValid = hasLength && hasUpper && hasLower && hasSpecial;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!token) {
      setError('A valid reset token is required.');
      return;
    }
    if (!isPasswordValid) {
      setError('New password must fulfill all security criteria.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setError('');
    setLoading(true);
    try {
      await api.post('/auth/reset-password', {
        token,
        newPassword,
      });
      setSuccess(true);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to reset password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <div className="brand-logo" style={{ margin: '0 auto 16px', width: '48px', height: '48px', fontSize: '1.5rem' }}>
            <KeyRound size={24} />
          </div>
          <h2>Create New Password</h2>
          <p>Choose a secure password for your Brightloop account</p>
        </div>

        {error && (
          <div style={{ background: '#fef2f2', border: '1px solid #fecaca', padding: '12px', borderRadius: '8px', color: '#991b1b', fontSize: '0.85rem', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        {success ? (
          <div>
            <div style={{ background: '#ecfdf5', border: '1px solid #a7f3d0', padding: '18px', borderRadius: '10px', color: '#065f46', fontSize: '0.9rem', marginBottom: '24px', textAlign: 'center' }}>
              <CheckCircle2 size={32} color="#10b981" style={{ margin: '0 auto 10px' }} />
              <div style={{ fontWeight: '700', fontSize: '1.05rem', marginBottom: '4px' }}>Password Successfully Updated</div>
              <p style={{ margin: 0, color: '#047857', fontSize: '0.82rem' }}>
                Your account password has been reset. You can now log in with your new credentials.
              </p>
            </div>

            <Link to="/login" className="btn btn-primary" style={{ width: '100%', padding: '12px' }}>
              Proceed to Sign In
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Reset Security Token</label>
              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  required
                  className="form-control"
                  style={{ paddingLeft: '38px', fontFamily: 'monospace', fontSize: '0.82rem' }}
                  placeholder="Paste reset token here"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                />
                <KeyRound size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '13px' }} />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">New Password</label>
              <div style={{ position: 'relative' }}>
                <input
                  type="password"
                  required
                  className="form-control"
                  style={{ paddingLeft: '38px' }}
                  placeholder="Enter new password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
                <Lock size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '13px' }} />
              </div>

              {/* Password checklist */}
              <div style={{ marginTop: '10px', background: '#f8fafc', padding: '10px 14px', borderRadius: '8px', border: '1px solid #e2e8f0', fontSize: '0.78rem' }}>
                <div style={{ fontWeight: '600', color: '#475569', marginBottom: '6px' }}>Security Checklist:</div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: hasLength ? '#10b981' : '#94a3b8' }}>
                    {hasLength ? <Check size={14} color="#10b981" /> : <X size={14} color="#cbd5e1" />}
                    <span>&gt; 8 characters</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: hasUpper ? '#10b981' : '#94a3b8' }}>
                    {hasUpper ? <Check size={14} color="#10b981" /> : <X size={14} color="#cbd5e1" />}
                    <span>1 uppercase</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: hasLower ? '#10b981' : '#94a3b8' }}>
                    {hasLower ? <Check size={14} color="#10b981" /> : <X size={14} color="#cbd5e1" />}
                    <span>1 lowercase</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: hasSpecial ? '#10b981' : '#94a3b8' }}>
                    {hasSpecial ? <Check size={14} color="#10b981" /> : <X size={14} color="#cbd5e1" />}
                    <span>1 special symbol</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Confirm New Password</label>
              <div style={{ position: 'relative' }}>
                <input
                  type="password"
                  required
                  className="form-control"
                  style={{ paddingLeft: '38px' }}
                  placeholder="Repeat new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
                <Lock size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '13px' }} />
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', marginTop: '10px' }}
              disabled={loading}
            >
              {loading ? 'Updating Password...' : 'Save & Reset Password'}
            </button>

            <div style={{ textAlign: 'center', marginTop: '20px' }}>
              <Link to="/login" style={{ fontSize: '0.85rem', color: '#64748b' }}>
                Cancel and return to Sign In
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

export default ResetPassword;
