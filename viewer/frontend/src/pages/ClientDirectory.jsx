import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { clientService } from '../services/clientService';
import Pagination from '../components/Pagination';
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
            link.setAttribute('download', `client_changes_${new Date().toISOString().split('T')[0]}.csv`);
            link.style.visibility = 'hidden';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            notify('Clients exported successfully', 'success');
        } catch (err) {
            notify('Export failed: ' + err.message, 'error');
        }
    };

    return (
        <div className="glass-section">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <h2 style={{ margin: 0 }}>Client Directory</h2>
                <div style={{ display: 'flex', gap: '1rem' }}>
                    <input
                        type="text"
                        placeholder="Search clients..."
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                        style={{ padding: '0.5rem', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--glass-border)', color: 'white', borderRadius: '4px' }}
                    />
                    <button className="btn" onClick={handleExport}>Export CSV</button>
                </div>
            </div>

            {loading && !clients.length ? (
                <p className="loading">Loading clients...</p>
            ) : error ? (
                <p className="error">{error}</p>
            ) : (
                <>
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Onboarding Date</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            {clients.map(client => (
                                <tr key={client.clientID}>
                                    <td>{client.clientID}</td>
                                    <td>
                                        <Link to={`/clients/${client.clientID}`}>
                                            {client.firstName} {client.lastName}
                                        </Link>
                                    </td>
                                    <td>{client.onboardingDate}</td>
                                    <td><span className="status-badge">{client.status}</span></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <Pagination data={data} onPageChange={(p) => loadClients(p, query)} />
                </>
            )}
        </div>
    );
};

export default ClientDirectory;
