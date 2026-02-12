import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { clientService } from '../services/clientService';
import Pagination from '../components/Pagination';
import Button from '../components/Button';
import { useNotification } from '../contexts/NotificationContext';

const ClientDirectory = () => {
    const { notify } = useNotification();
    const [clients, setClients] = useState([]);
    const [data, setData] = useState(null);
    const [query, setQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const loadClients = async (page = 0, searchQuery = '') => {
        setLoading(true);
        try {
            const result = await clientService.getClients(page, searchQuery);
            setClients(result.content);
            setData(result);
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        const timeout = setTimeout(() => {
            loadClients(0, query);
        }, 300);
        return () => clearTimeout(timeout);
    }, [query]);

    const handleExport = async () => {
        try {
            const data = await clientService.exportClients();
            if (!data || data.length === 0) {
                notify('No data to export', 'warning');
                return;
            }

            const headers = Object.keys(data[0]).join(',');
            const rows = data.map(obj =>
                Object.values(obj).map(val =>
                    typeof val === 'string' ? `"${val.replace(/"/g, '""')}"` : val
                ).join(',')
            );
            const csvContent = [headers, ...rows].join('\n');

            const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.setAttribute('href', url);
            link.setAttribute('download', `clients_export_${new Date().toISOString().split('T')[0]}.csv`);
            link.style.display = 'none';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            notify('Clients exported successfully', 'success');
        } catch (err) {
            notify('Export failed: ' + err.message, 'error');
        }
    };

    const getStatusBadgeStyle = (status) => {
        switch (status) {
            case 'ACTIVE': return { backgroundColor: 'rgba(16, 185, 129, 0.2)', color: '#10b981', border: '1px solid rgba(16, 185, 129, 0.3)' };
            case 'IN_REVIEW': return { backgroundColor: 'rgba(245, 158, 11, 0.2)', color: '#fbbf24', border: '1px solid rgba(245, 158, 11, 0.3)' };
            case 'SUSPENDED': return { backgroundColor: 'rgba(239, 68, 68, 0.2)', color: '#f87171', border: '1px solid rgba(239, 68, 68, 0.3)' };
            default: return { backgroundColor: 'rgba(107, 114, 128, 0.2)', color: '#9ca3af', border: '1px solid rgba(107, 114, 128, 0.3)' };
        }
    };

    return (
        <div className="client-directory-page">
            {/* Header & Stats */}
            <div style={{ marginBottom: '2rem' }}>
                <div className="page-header" style={{ alignItems: 'flex-start', borderBottom: 'none', paddingBottom: '1rem', marginBottom: '1.5rem' }}>
                    <div>
                        <h1 style={{ fontSize: '2rem', margin: '0 0 0.5rem 0', background: 'var(--header-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                            Client Directory
                        </h1>
                        <p style={{ color: 'var(--text-secondary)', margin: 0 }}>View and manage comprehensive client information</p>
                    </div>
                    <Button onClick={handleExport} variant="secondary">
                        <span style={{ marginRight: '0.5rem' }}>↓</span> Export Directory
                    </Button>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.5rem' }}>
                    {[
                        { label: 'Total Clients', value: data?.totalElements || 0, color: '#3b82f6' },
                        { label: 'Active', value: clients.filter(c => c.status === 'ACTIVE').length, color: '#10b981' },
                        { label: 'In Review', value: clients.filter(c => c.status === 'IN_REVIEW').length, color: '#f59e0b' },
                        { label: 'Suspended', value: clients.filter(c => c.status === 'SUSPENDED').length, color: '#ef4444' }
                    ].map((s, idx) => (
                        <div key={idx} className="glass-section" style={{ padding: '1.5rem', marginBottom: 0, textAlign: 'center', borderLeft: `4px solid ${s.color}` }}>
                            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{s.label}</div>
                            <div style={{ fontSize: '2rem', fontWeight: '700', color: 'var(--text-color)' }}>{s.value}</div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Main Content Area */}
            <div className="glass-section" style={{ padding: '0' }}>
                <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h3 style={{ margin: 0 }}>Client List</h3>
                    <div style={{ position: 'relative', width: '350px' }}>
                        <input
                            type="text"
                            placeholder="Search by name, ID or status..."
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            style={{
                                width: '100%',
                                padding: '0.6rem 1rem 0.6rem 2.5rem',
                                background: 'var(--input-bg)',
                                border: '1px solid var(--input-border)',
                                borderRadius: '8px',
                                color: 'var(--text-color)',
                                outline: 'none',
                                boxSizing: 'border-box'
                            }}
                        />
                        <span style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-secondary)' }}>🔍</span>
                    </div>
                </div>

                <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                            <tr style={{ background: 'var(--hover-bg)' }}>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>Client ID</th>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>Legal Name</th>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>Onboarding Date</th>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>Status</th>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'right', color: 'var(--text-secondary)', fontWeight: '500' }}>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading && !clients.length ? (
                                <tr>
                                    <td colSpan="5" style={{ padding: '4rem', textAlign: 'center' }}>
                                        <div className="loading-spinner">Loading client records...</div>
                                    </td>
                                </tr>
                            ) : error ? (
                                <tr>
                                    <td colSpan="5" style={{ padding: '4rem', textAlign: 'center', color: '#ef4444' }}>
                                        <div style={{ marginBottom: '1rem' }}>⚠️ Error loading clients</div>
                                        <div>{error}</div>
                                    </td>
                                </tr>
                            ) : clients.length === 0 ? (
                                <tr>
                                    <td colSpan="5" style={{ padding: '4rem', textAlign: 'center', color: '#64748b' }}>
                                        No client records found.
                                    </td>
                                </tr>
                            ) : (
                                clients.map((client) => (
                                    <tr key={client.clientID} className="table-row-hover" style={{ borderBottom: '1px solid var(--glass-border)', transition: 'background 0.2s' }}>
                                        <td style={{ padding: '1.25rem 1.5rem', fontWeight: '600', color: '#3b82f6' }}>
                                            #{client.clientID}
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                                                <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: 'rgba(59, 130, 246, 0.1)', color: '#3b82f6', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold' }}>
                                                    {client.firstName.substring(0, 1)}
                                                </div>
                                                <div style={{ fontWeight: '500' }}>{client.firstName} {client.lastName}</div>
                                            </div>
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem', color: 'var(--text-secondary)' }}>
                                            {new Date(client.onboardingDate).toLocaleDateString()}
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            <span style={{
                                                padding: '0.25rem 0.75rem',
                                                borderRadius: '20px',
                                                fontSize: '0.75rem',
                                                fontWeight: '600',
                                                letterSpacing: '0.025em',
                                                ...getStatusBadgeStyle(client.status)
                                            }}>
                                                {client.status.replace('_', ' ')}
                                            </span>
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem', textAlign: 'right' }}>
                                            <Link to={`/clients/${client.clientID}`}>
                                                <Button variant="secondary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.875rem' }}>
                                                    View Details
                                                </Button>
                                            </Link>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
                {data && <div style={{ padding: '1.5rem', borderTop: '1px solid var(--glass-border)' }}>
                    <Pagination data={data} onPageChange={(p) => loadClients(p, query)} />
                </div>}
            </div>
        </div>
    );
};

export default ClientDirectory;
