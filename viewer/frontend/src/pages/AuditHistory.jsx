import React, { useState, useEffect } from 'react';
import { auditService } from '../services/auditService';
import Button from '../components/Button';

const AuditHistory = () => {
    const [audits, setAudits] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchAudits = async () => {
            setLoading(true);
            try {
                const data = await auditService.getAudits();
                setAudits(data);
            } catch (err) {
                console.error("Failed to load audits:", err);
                setError(err.message + (err.message.includes('403') ? " (Access Denied)" : ""));
            } finally {
                setLoading(false);
            }
        };

        fetchAudits();
    }, []);

    const getActionBadgeStyle = (action) => {
        const a = action.toUpperCase();
        if (a.includes('LOGIN') || a.includes('AUTH')) return { backgroundColor: 'rgba(59, 130, 246, 0.1)', color: '#60a5fa', border: '1px solid rgba(59, 130, 246, 0.2)' };
        if (a.includes('CREATE') || a.includes('ADD')) return { backgroundColor: 'rgba(16, 185, 129, 0.1)', color: '#34d399', border: '1px solid rgba(16, 185, 129, 0.2)' };
        if (a.includes('UPDATE') || a.includes('EDIT')) return { backgroundColor: 'rgba(245, 158, 11, 0.1)', color: '#fbbf24', border: '1px solid rgba(245, 158, 11, 0.2)' };
        if (a.includes('DELETE') || a.includes('REMOVE')) return { backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#f87171', border: '1px solid rgba(239, 68, 68, 0.2)' };
        if (a.includes('TRANSITION') || a.includes('WORKFLOW')) return { backgroundColor: 'rgba(139, 92, 246, 0.1)', color: '#a78bfa', border: '1px solid rgba(139, 92, 246, 0.2)' };
        return { backgroundColor: 'rgba(255, 255, 255, 0.05)', color: '#94a3b8', border: '1px solid rgba(255, 255, 255, 0.1)' };
    };

    const getStats = () => {
        return {
            total: audits.length,
            security: audits.filter(a => a.action.toUpperCase().includes('LOGIN')).length,
            operations: audits.filter(a => a.action.toUpperCase().includes('CASE') || a.action.toUpperCase().includes('TRANSITION')).length,
            admin: audits.filter(a => a.action.toUpperCase().includes('USER') || a.action.toUpperCase().includes('PERM') || a.action.toUpperCase().includes('CONFIG')).length
        };
    };

    const stats = getStats();

    return (
        <div className="audit-history-page">
            <div className="page-header" style={{ marginBottom: '2.5rem' }}>
                <h1 style={{ fontSize: '2rem', margin: '0 0 0.5rem 0', background: 'linear-gradient(135deg, #fff 0%, #94a3b8 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                    System Audit Trail
                </h1>
                <p style={{ color: '#94a3b8', margin: 0 }}>Immutable record of all security and operational system events</p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.5rem', marginBottom: '2.5rem' }}>
                {[
                    { label: 'Total Events', value: stats.total, color: '#3b82f6', icon: '📄' },
                    { label: 'Auth Events', value: stats.security, color: '#10b981', icon: '🔑' },
                    { label: 'Case Ops', value: stats.operations, color: '#8b5cf6', icon: '📊' },
                    { label: 'Config Changes', value: stats.admin, color: '#f59e0b', icon: '⚙️' }
                ].map((s, idx) => (
                    <div key={idx} className="glass-section" style={{ padding: '1.25rem', marginBottom: 0, borderLeft: `4px solid ${s.color}`, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <div>
                            <div style={{ fontSize: '0.75rem', color: '#64748b', marginBottom: '0.25rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{s.label}</div>
                            <div style={{ fontSize: '1.5rem', fontWeight: '700', color: '#f8fafc' }}>{s.value}</div>
                        </div>
                        <div style={{ fontSize: '1.5rem', opacity: 0.5 }}>{s.icon}</div>
                    </div>
                ))}
            </div>

            <div className="glass-section" style={{ padding: 0, overflow: 'hidden' }}>
                <div style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid rgba(255,255,255,0.08)', background: 'rgba(255,255,255,0.02)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h3 style={{ margin: 0, fontSize: '1.1rem' }}>Historical Event Log</h3>
                    <div style={{ color: '#64748b', fontSize: '0.875rem' }}>Showing latest active records</div>
                </div>

                <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                            <tr style={{ textAlign: 'left', background: 'rgba(0, 0, 0, 0.2)' }}>
                                <th style={{ padding: '1rem 1.5rem', color: '#94a3b8', fontWeight: '500', fontSize: '0.875rem' }}>Timestamp</th>
                                <th style={{ padding: '1rem 1.5rem', color: '#94a3b8', fontWeight: '500', fontSize: '0.875rem' }}>Security Principal</th>
                                <th style={{ padding: '1rem 1.5rem', color: '#94a3b8', fontWeight: '500', fontSize: '0.875rem' }}>Action</th>
                                <th style={{ padding: '1rem 1.5rem', color: '#94a3b8', fontWeight: '500', fontSize: '0.875rem' }}>Event Details</th>
                                <th style={{ padding: '1rem 1.5rem', color: '#94a3b8', fontWeight: '500', fontSize: '0.875rem' }}>Source IP</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan="5" style={{ padding: '4rem', textAlign: 'center' }}>
                                        <div className="loading-spinner">Decrypting audit logs...</div>
                                    </td>
                                </tr>
                            ) : audits.length === 0 ? (
                                <tr>
                                    <td colSpan="5" style={{ padding: '4rem', textAlign: 'center', color: '#64748b' }}>
                                        No audit events found in current cycle.
                                    </td>
                                </tr>
                            ) : (
                                audits.map(audit => (
                                    <tr key={audit.auditID} className="table-row-hover" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', transition: 'background 0.2s' }}>
                                        <td style={{ padding: '1rem 1.5rem', whiteSpace: 'nowrap' }}>
                                            <div style={{ color: '#cbd5e1', fontSize: '0.9rem' }}>{new Date(audit.timestamp).toLocaleDateString()}</div>
                                            <div style={{ color: '#64748b', fontSize: '0.75rem' }}>{new Date(audit.timestamp).toLocaleTimeString()}</div>
                                        </td>
                                        <td style={{ padding: '1rem 1.5rem' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                                <div style={{ width: '24px', height: '24px', borderRadius: '50%', background: 'rgba(59, 130, 246, 0.1)', color: '#3b82f6', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.7rem', fontWeight: 'bold' }}>
                                                    {(audit.username || 'SYS').substring(0, 1).toUpperCase()}
                                                </div>
                                                <span style={{ fontWeight: '500', color: '#e2e8f0' }}>{audit.username || 'System'}</span>
                                            </div>
                                        </td>
                                        <td style={{ padding: '1rem 1.5rem' }}>
                                            <span style={{
                                                padding: '0.2rem 0.6rem',
                                                borderRadius: '4px',
                                                fontSize: '0.75rem',
                                                fontWeight: '600',
                                                textTransform: 'uppercase',
                                                letterSpacing: '0.025em',
                                                ...getActionBadgeStyle(audit.action)
                                            }}>
                                                {audit.action.split('_').join(' ')}
                                            </span>
                                        </td>
                                        <td style={{ padding: '1rem 1.5rem', color: '#94a3b8', fontSize: '0.875rem' }}>{audit.details}</td>
                                        <td style={{ padding: '1rem 1.5rem', fontFamily: 'monospace', fontSize: '0.8rem', color: '#64748b' }}>{audit.ipAddress || 'Internal'}</td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
            {error && (
                <div style={{ marginTop: '1.5rem', padding: '1rem', borderRadius: '8px', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.2)', color: '#f87171', fontSize: '0.875rem' }}>
                    ⚠️ {error}
                </div>
            )}
        </div>
    );
};

export default AuditHistory;
