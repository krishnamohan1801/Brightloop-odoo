import React, { useState, useEffect } from 'react';
import api from '../api';
import { History, Shield, RefreshCw } from 'lucide-react';

const AuditLogs = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchLogs = () => {
    setLoading(true);
    api.get('/dashboard/audit-logs?limit=50')
      .then((res) => setLogs(res.data))
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: '800', letterSpacing: '-0.02em', color: '#0f172a' }}>
            System Audit Trail &amp; Activity Log
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.85rem', marginTop: '4px' }}>
            Part 4: Immutable audit log tracking subscription status transitions, automated billing events, simulated payment receipts, and refund reversals.
          </p>
        </div>

        <button onClick={fetchLogs} className="btn btn-outline" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          <span>Refresh Audit Trail</span>
        </button>
      </div>

      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>User / Initiator</th>
                <th>Operation / Action</th>
                <th>Target Entity</th>
                <th>Details &amp; Audit Metadata</th>
              </tr>
            </thead>
            <tbody>
              {logs.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '24px', color: '#64748b' }}>
                    No audit records logged yet.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id}>
                    <td style={{ fontSize: '0.8rem', color: '#64748b', whiteSpace: 'nowrap' }}>
                      {log.createdAt ? log.createdAt.substring(0, 19).replace('T', ' ') : '—'}
                    </td>
                    <td>
                      <div style={{ fontWeight: '600', fontSize: '0.82rem' }}>
                        {log.userEmail || 'System Engine'}
                      </div>
                    </td>
                    <td>
                      <span className={`badge badge-${
                        log.action.includes('SUCCESS') || log.action.includes('CREATE') ? 'active' :
                        log.action.includes('REFUND') || log.action.includes('CANCEL') || log.action.includes('FAIL') ? 'danger' :
                        log.action.includes('PAUSE') ? 'warning' : 'info'
                      }`}>
                        {log.action}
                      </span>
                    </td>
                    <td>
                      <span style={{ fontFamily: 'monospace', fontSize: '0.78rem', color: '#475569' }}>
                        {log.entityType ? `${log.entityType}` : 'system'}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.82rem', color: '#1e293b' }}>
                      {log.details}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default AuditLogs;
