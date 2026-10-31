import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import { Mail, ArrowLeft, CheckCircle2, AlertCircle, KeyRound, ExternalLink } from 'lucide-react';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [resetData, setResetData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post('/auth/forgot-password', { email });
      setResetData(res.data);
      setSubmitted(true);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Request failed');
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
          <h2>Reset Password</h2>
          <p>We'll dispatch a secure recovery link to your registered email</p>
        </div>

        {error && (
          <div style={{ background: '#fef2f2', border: '1px solid #fecaca', padding: '12px', borderRadius: '8px', color: '#991b1b', fontSize: '0.85rem', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        {submitted ? (
          <div>
            <div style={{ background: '#ecfdf5', border: '1px solid #a7f3d0', padding: '16px', borderRadius: '10px', color: '#065f46', fontSize: '0.88rem', marginBottom: '20px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: '700', marginBottom: '6px' }}>
                <CheckCircle2 size={18} color="#10b981" />
                <span>Reset Link Dispatched</span>
              </div>
              <p style={{ margin: 0, lineHeight: 1.5 }}>
                If <strong>{email}</strong> is registered, password reset instructions have been sent via email.
              </p>
            </div>

            {resetData?.token && (
              <div style={{ background: '#f8fafc', border: '1px dashed #cbd5e1', padding: '14px', borderRadius: '8px', marginBottom: '20px', fontSize: '0.82rem' }}>
                <div style={{ fontWeight: '700', color: '#334155', marginBottom: '6px' }}>Direct Test Access:</div>
                <p style={{ color: '#64748b', marginBottom: '10px' }}>
                  For instant local testing, use this direct link:
                </p>
                <Link
                  to={`/reset-password?token=${resetData.token}`}
                  className="btn btn-secondary btn-sm"
                  style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
                >
                  <span>Proceed to Reset Password Page</span>
                  <ExternalLink size={14} />
                </Link>
              </div>
            )}

            <Link to="/login" className="btn btn-outline" style={{ width: '100%', padding: '10px' }}>
              Back to Sign In
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Registered Account Email</label>
              <div style={{ position: 'relative' }}>
                <input
                  type="email"
                  required
                  className="form-control"
                  style={{ paddingLeft: '38px' }}
                  placeholder="name@company.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
                <Mail size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '13px' }} />
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', marginTop: '8px' }}
              disabled={loading}
            >
              {loading ? 'Sending Recovery Link...' : 'Send Recovery Link'}
            </button>

            <div style={{ textAlign: 'center', marginTop: '20px' }}>
              <Link to="/login" style={{ fontSize: '0.85rem', color: '#64748b', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                <ArrowLeft size={14} />
                <span>Back to Sign In</span>
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

export default ForgotPassword;
