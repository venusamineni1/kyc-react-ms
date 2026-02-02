import React, { useState, useEffect } from 'react';
import { auditService } from '../services/auditService';

const AuditHistory = () => {
    const [audits, setAudits] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchAudits = async () => {
            try {
                const data = await auditService.getAudits();
                setAudits(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchAudits();
    }, []);

    if (loading) return <div style={{ padding: '2rem', color: 'white' }}>Loading audit logs...</div>;
    if (error) return <div style={{ padding: '2rem', color: '#ff4d4f' }}>Error: {error}</div>;

    return (
        <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
            <h2 style={{ color: 'white', marginBottom: '2rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '1rem' }}>
                User Audit History
            </h2>

            <div style={{
                background: 'rgba(255, 255, 255, 0.05)',
                borderRadius: '8px',
                overflow: 'hidden',
                border: '1px solid rgba(255, 255, 255, 0.1)'
            }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', color: 'var(--text-primary)' }}>
                    <thead>
                        <tr style={{ background: 'rgba(0, 0, 0, 0.2)', textAlign: 'left' }}>
                            <th style={{ padding: '1rem', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>Time</th>
                            <th style={{ padding: '1rem', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>User</th>
                            <th style={{ padding: '1rem', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>Action</th>
                            <th style={{ padding: '1rem', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>Details</th>
                            <th style={{ padding: '1rem', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>IP</th>
                        </tr>
                    </thead>
                    <tbody>
                        {audits.map(audit => (
                            <tr key={audit.auditID} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                                <td style={{ padding: '1rem' }}>{new Date(audit.timestamp).toLocaleString()}</td>
                                <td style={{ padding: '1rem', fontWeight: 'bold', color: '#69c0ff' }}>{audit.username || 'System'}</td>
                                <td style={{ padding: '1rem' }}>
                                    <span style={{
                                        background: 'rgba(255,255,255,0.1)',
                                        padding: '2px 8px',
                                        borderRadius: '4px',
                                        fontSize: '0.9rem'
                                    }}>
                                        {audit.action}
                                    </span>
                                </td>
                                <td style={{ padding: '1rem', color: '#aaa' }}>{audit.details}</td>
                                <td style={{ padding: '1rem', fontFamily: 'monospace', fontSize: '0.9rem' }}>{audit.ipAddress || '-'}</td>
                            </tr>
                        ))}
                        {audits.length === 0 && (
                            <tr>
                                <td colSpan="5" style={{ padding: '2rem', textAlign: 'center', color: '#666' }}>
                                    No audit logs found.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default AuditHistory;
