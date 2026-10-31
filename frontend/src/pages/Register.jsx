import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Lock, Mail, User, Phone, Check, X, AlertCircle } from 'lucide-react';

const Register = () => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  // Password criteria per PDF
  const hasLength = password.length > 8;
  const hasUpper = /[A-Z]/.test(password);
  const hasLower = /[a-z]/.test(password);
  const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);
  const isPasswordValid = hasLength && hasUpper && hasLower && hasSpecial;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isPasswordValid) {
      setError('Please fulfill all password requirements below.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await register({ fullName, email, phone, password });
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <div className="brand-logo" style={{ margin: '0 auto 16px', width: '48px', height: '48px', fontSize: '1.5rem' }}>
            B
          </div>
          <h2>Join Brightloop</h2>
          <p>Create your subscription customer account</p>
        </div>

        {error && (
          <div style={{ background: '#fef2f2', border: '1px solid #fecaca', padding: '12px', borderRadius: '8px', color: '#991b1b', fontSize: '0.85rem', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Full Name / Business Name</label>
            <div style={{ position: 'relative' }}>
              <input
                type="text"
                required
                className="form-control"
                style={{ paddingLeft: '38px' }}
                placeholder="Acme Technologies Corp"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />
              <User size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '13px' }} />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Email Address</label>
            <div style={{ position: 'relative' }}>
              <input
                type="email"
                required
                className="form-control"
                style={{ paddingLeft: '38px' }}
                placeholder="contact@acme.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <Mail size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '13px' }} />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Phone Number (Optional)</label>
            <div style={{ position: 'relative' }}>
              <input
                type="tel"
                className="form-control"
                style={{ paddingLeft: '38px' }}
                placeholder="+1 555-0199"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
              <Phone size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '13px' }} />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <div style={{ position: 'relative' }}>
              <input
                type="password"
                required
                className="form-control"
                style={{ paddingLeft: '38px' }}
                placeholder="Create secure password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <Lock size={16} color="#94a3b8" style={{ position: 'absolute', left: '12px', top: '13px' }} />
            </div>

            {/* Password Requirements Checklist */}
            <div style={{ marginTop: '10px', background: '#f8fafc', padding: '10px 14px', borderRadius: '8px', border: '1px solid #e2e8f0', fontSize: '0.78rem' }}>
              <div style={{ fontWeight: '600', color: '#475569', marginBottom: '6px' }}>Password Requirements:</div>
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

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', padding: '12px', marginTop: '12px' }}
            disabled={loading}
          >
            {loading ? 'Creating Account...' : 'Register Account'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '0.85rem', color: '#64748b' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ fontWeight: '600' }}>
            Sign In
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Register;
