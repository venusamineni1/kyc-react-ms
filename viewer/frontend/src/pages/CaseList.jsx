import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { caseService } from '../services/caseService';
import Pagination from '../components/Pagination';
import { useNotification } from '../contexts/NotificationContext';

const ALL_STATUSES = [
    'PROCESSING', 'KYC_ANALYST', 'REVIEWER_REVIEW', 'AFC_REVIEW', 'ACO_REVIEW', 'APPROVED', 'REJECTED'
];

const CaseList = () => {
    const { notify } = useNotification();
    const [cases, setCases] = useState([]);
    const [allCases, setAllCases] = useState([]);
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [stats, setStats] = useState({ total: 0, pending: 0, review: 0 });

    // Filters
    const [filters, setFilters] = useState({ status: '', assignee: '', search: '' });
    const [showFilters, setShowFilters] = useState(false);

    // Bulk selection
    const [selectedCases, setSelectedCases] = useState(new Set());
    const [bulkAssignUser, setBulkAssignUser] = useState('');
    const [showBulkModal, setShowBulkModal] = useState(false);
    const [users, setUsers] = useState([]);
    const [bulkLoading, setBulkLoading] = useState(false);

    const loadCases = async () => {
        setLoading(true);
        try {
            const result = await caseService.getCases(0);
            setAllCases(result.content);
            setData(result);
            if (result.content) {
                const pending = result.content.filter(c => c.status === 'PROCESSING' || c.status === 'KYC_ANALYST').length;
                const review = result.content.filter(c => c.status.includes('REVIEW')).length;
                setStats({ total: result.totalElements || result.content.length, pending, review });
            }
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadCases(); }, []);

    // Apply filters client-side
    const filteredCases = useMemo(() => {
        return allCases.filter(c => {
            if (filters.status && c.status !== filters.status) return false;
            if (filters.assignee && !(c.assignedTo || '').toLowerCase().includes(filters.assignee.toLowerCase())) return false;
            if (filters.search) {
                const q = filters.search.toLowerCase();
                if (!(c.clientName || '').toLowerCase().includes(q) && !String(c.caseID).includes(q)) return false;
            }
            return true;
        });
    }, [allCases, filters]);

    useEffect(() => { setCases(filteredCases); }, [filteredCases]);

    const getStatusColor = (status) => {
        switch (status) {
            case 'APPROVED': return '#52c41a';
            case 'REJECTED': return '#ff4d4f';
            case 'PROCESSING':
            case 'KYC_ANALYST': return '#1890ff';
            case 'REVIEWER_REVIEW':
            case 'AFC_REVIEW':
            case 'ACO_REVIEW': return '#faad14';
            default: return 'var(--text-secondary)';
        }
    };

    const toggleSelect = (id) => {
        setSelectedCases(prev => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    };

    const toggleSelectAll = () => {
        if (selectedCases.size === cases.length) {
            setSelectedCases(new Set());
        } else {
            setSelectedCases(new Set(cases.map(c => c.caseID)));
        }
    };

    const exportCSV = (rows) => {
        const header = 'CaseID,ClientName,Status,CreatedDate,AssignedTo';
        const lines = rows.map(c =>
            `${c.caseID},"${c.clientName || ''}",${c.status},${new Date(c.createdDate).toLocaleDateString()},"${c.assignedTo || ''}"`
        );
        const blob = new Blob([header + '\n' + lines.join('\n')], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'cases.csv';
        a.click();
        URL.revokeObjectURL(url);
    };

    const openBulkAssign = async () => {
        try {
            const allUsers = await caseService.getAllUsers();
            setUsers(allUsers);
        } catch {
            setUsers([]);
        }
        setShowBulkModal(true);
    };

    const handleBulkAssign = async () => {
        if (!bulkAssignUser) return;
        setBulkLoading(true);
        try {
            await Promise.all([...selectedCases].map(id => caseService.assignCase(id, bulkAssignUser)));
            notify(`${selectedCases.size} case(s) assigned to ${bulkAssignUser}`, 'success');
            setSelectedCases(new Set());
            setShowBulkModal(false);
            setBulkAssignUser('');
            await loadCases();
        } catch (err) {
            notify('Bulk assign failed: ' + err.message, 'error');
        } finally {
            setBulkLoading(false);
        }
    };

    return (
        <div className="case-management-container">
            <header className="page-header" style={{ marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <h1 style={{ margin: 0, fontSize: '2rem', color: '#fff' }}>Case Management</h1>
                    <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>Monitor and manage ongoing KYC due diligence cases.</p>
                </div>
                <div style={{ display: 'flex', gap: '1rem' }}>
                    <div className="stat-pill">
                        <span className="stat-label">Total Cases</span>
                        <span className="stat-value">{stats.total}</span>
                    </div>
                    <div className="stat-pill" style={{ borderColor: '#1890ff' }}>
                        <span className="stat-label" style={{ color: '#1890ff' }}>Pending</span>
                        <span className="stat-value">{stats.pending}</span>
                    </div>
                    <div className="stat-pill" style={{ borderColor: '#faad14' }}>
                        <span className="stat-label" style={{ color: '#faad14' }}>In Review</span>
                        <span className="stat-value">{stats.review}</span>
                    </div>
                </div>
            </header>

            <div className="glass-section" style={{ padding: '0' }}>
                <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
                    <h3 style={{ margin: 0 }}>Active Cases</h3>
                    <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
                        <button className="btn-icon" title="Toggle Filters" onClick={() => setShowFilters(f => !f)}
                            style={{ background: showFilters ? 'rgba(79,172,254,0.15)' : 'none', borderColor: showFilters ? '#4facfe' : undefined, color: showFilters ? '#4facfe' : undefined }}>
                            Filter {filters.status || filters.assignee || filters.search ? '●' : ''}
                        </button>
                        <button className="btn-icon" title="Export All" onClick={() => exportCSV(cases)}>Export</button>
                    </div>
                </div>

                {/* Filter bar */}
                {showFilters && (
                    <div style={{ padding: '1rem 1.5rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', gap: '1rem', flexWrap: 'wrap', background: 'rgba(255,255,255,0.02)' }}>
                        <input
                            placeholder="Search by name or ID..."
                            value={filters.search}
                            onChange={e => setFilters(f => ({ ...f, search: e.target.value }))}
                            style={{ flex: '1', minWidth: '160px', padding: '0.5rem 0.75rem', background: 'rgba(255,255,255,0.07)', border: '1px solid var(--glass-border)', borderRadius: '8px', color: '#fff' }}
                        />
                        <select
                            value={filters.status}
                            onChange={e => setFilters(f => ({ ...f, status: e.target.value }))}
                            style={{ padding: '0.5rem 0.75rem', background: 'rgba(255,255,255,0.07)', border: '1px solid var(--glass-border)', borderRadius: '8px', color: '#fff' }}
                        >
                            <option value="">All Statuses</option>
                            {ALL_STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
                        </select>
                        <input
                            placeholder="Filter by assignee..."
                            value={filters.assignee}
                            onChange={e => setFilters(f => ({ ...f, assignee: e.target.value }))}
                            style={{ width: '160px', padding: '0.5rem 0.75rem', background: 'rgba(255,255,255,0.07)', border: '1px solid var(--glass-border)', borderRadius: '8px', color: '#fff' }}
                        />
                        <button onClick={() => setFilters({ status: '', assignee: '', search: '' })}
                            style={{ padding: '0.5rem 0.75rem', background: 'none', border: '1px solid var(--glass-border)', borderRadius: '8px', color: 'var(--text-secondary)', cursor: 'pointer' }}>
                            Clear
                        </button>
                    </div>
                )}

                {/* Bulk action bar */}
                {selectedCases.size > 0 && (
                    <div style={{ padding: '0.75rem 1.5rem', borderBottom: '1px solid var(--glass-border)', background: 'rgba(79,172,254,0.08)', display: 'flex', alignItems: 'center', gap: '1rem' }}>
                        <span style={{ color: '#4facfe', fontWeight: 600 }}>{selectedCases.size} selected</span>
                        <button onClick={openBulkAssign} style={{ padding: '0.4rem 1rem', background: '#4facfe', color: '#000', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600 }}>
                            Assign To...
                        </button>
                        <button onClick={() => exportCSV(cases.filter(c => selectedCases.has(c.caseID)))}
                            style={{ padding: '0.4rem 1rem', background: 'none', border: '1px solid var(--glass-border)', color: '#fff', borderRadius: '6px', cursor: 'pointer' }}>
                            Export Selected
                        </button>
                        <button onClick={() => setSelectedCases(new Set())}
                            style={{ marginLeft: 'auto', background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}>
                            Clear selection
                        </button>
                    </div>
                )}

                {loading && !cases.length ? (
                    <div style={{ padding: '3rem', textAlign: 'center' }}>
                        <p className="loading">Fetching cases...</p>
                    </div>
                ) : error ? (
                    <div style={{ padding: '3rem', textAlign: 'center' }}>
                        <p className="error">{error}</p>
                    </div>
                ) : (
                    <>
                        <div className="responsive-table-container">
                            <table className="modern-table">
                                <thead>
                                    <tr>
                                        <th style={{ width: '40px' }}>
                                            <input type="checkbox"
                                                checked={cases.length > 0 && selectedCases.size === cases.length}
                                                onChange={toggleSelectAll}
                                                style={{ cursor: 'pointer' }}
                                            />
                                        </th>
                                        <th>Case ID</th>
                                        <th>Client Name</th>
                                        <th>Status</th>
                                        <th>Created Date</th>
                                        <th>Assigned To</th>
                                        <th style={{ textAlign: 'right' }}>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {cases.length === 0 ? (
                                        <tr><td colSpan="7" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>No cases match the current filters.</td></tr>
                                    ) : cases.map(kycCase => (
                                        <tr key={kycCase.caseID} className="table-row-hover"
                                            style={{ background: selectedCases.has(kycCase.caseID) ? 'rgba(79,172,254,0.07)' : undefined }}>
                                            <td>
                                                <input type="checkbox"
                                                    checked={selectedCases.has(kycCase.caseID)}
                                                    onChange={() => toggleSelect(kycCase.caseID)}
                                                    style={{ cursor: 'pointer' }}
                                                />
                                            </td>
                                            <td style={{ fontWeight: 'bold', color: 'var(--primary-color)' }}>
                                                #{kycCase.caseID}
                                            </td>
                                            <td>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                                    <div className="initials-avatar-sm">{kycCase.clientName?.[0]}</div>
                                                    {kycCase.clientName}
                                                </div>
                                            </td>
                                            <td>
                                                <span className="status-badge-modern" style={{
                                                    backgroundColor: `${getStatusColor(kycCase.status)}20`,
                                                    color: getStatusColor(kycCase.status),
                                                    border: `1px solid ${getStatusColor(kycCase.status)}40`
                                                }}>
                                                    <span className="status-dot" style={{ backgroundColor: getStatusColor(kycCase.status) }}></span>
                                                    {kycCase.status?.replace(/_/g, ' ')}
                                                </span>
                                            </td>
                                            <td style={{ color: 'var(--text-secondary)' }}>
                                                {new Date(kycCase.createdDate).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}
                                            </td>
                                            <td>
                                                {kycCase.assignedTo ? (
                                                    <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                                                        <span style={{ fontSize: '0.8rem', opacity: 0.7 }}>👤</span>
                                                        {kycCase.assignedTo}
                                                    </div>
                                                ) : (
                                                    <span style={{ color: '#ff4d4f', fontSize: '0.85rem', fontWeight: '500' }}>Unassigned</span>
                                                )}
                                            </td>
                                            <td style={{ textAlign: 'right' }}>
                                                <Link to={`/cases/${kycCase.caseID}`} className="btn-view-modern">View Details</Link>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <div style={{ padding: '1rem', borderTop: '1px solid var(--glass-border)' }}>
                            <Pagination data={data} onPageChange={() => {}} />
                        </div>
                    </>
                )}
            </div>

            {/* Bulk assign modal */}
            {showBulkModal && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
                    <div className="glass-section" style={{ width: '380px', padding: '2rem' }}>
                        <h3 style={{ marginTop: 0 }}>Assign {selectedCases.size} Case(s)</h3>
                        <select
                            value={bulkAssignUser}
                            onChange={e => setBulkAssignUser(e.target.value)}
                            style={{ width: '100%', padding: '0.6rem', marginBottom: '1.5rem', background: 'rgba(255,255,255,0.07)', border: '1px solid var(--glass-border)', borderRadius: '8px', color: '#fff' }}
                        >
                            <option value="">Select user...</option>
                            {users.map(u => <option key={u.username} value={u.username}>{u.username}</option>)}
                        </select>
                        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                            <button onClick={() => { setShowBulkModal(false); setBulkAssignUser(''); }}
                                style={{ padding: '0.5rem 1rem', background: 'none', border: '1px solid var(--glass-border)', borderRadius: '8px', color: '#fff', cursor: 'pointer' }}>
                                Cancel
                            </button>
                            <button onClick={handleBulkAssign} disabled={!bulkAssignUser || bulkLoading}
                                style={{ padding: '0.5rem 1rem', background: '#4facfe', border: 'none', borderRadius: '8px', color: '#000', fontWeight: 600, cursor: 'pointer' }}>
                                {bulkLoading ? 'Assigning...' : 'Assign'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <style>{`
                .stat-pill { background: var(--glass-bg); border: 1px solid var(--glass-border); padding: 0.75rem 1.25rem; border-radius: 12px; display: flex; flex-direction: column; min-width: 120px; }
                .stat-label { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-secondary); }
                .stat-value { font-size: 1.5rem; font-weight: 700; color: #fff; }
                .modern-table { width: 100%; border-collapse: collapse; }
                .modern-table th { text-align: left; padding: 1rem 1.5rem; font-size: 0.85rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-secondary); background: rgba(255,255,255,0.02); }
                .modern-table td { padding: 1.25rem 1.5rem; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 0.95rem; }
                .table-row-hover:hover { background: rgba(255,255,255,0.03); }
                .status-badge-modern { display: inline-flex; align-items: center; gap: 8px; padding: 4px 12px; border-radius: 20px; font-size: 0.8rem; font-weight: 600; text-transform: uppercase; }
                .status-dot { width: 6px; height: 6px; border-radius: 50%; }
                .initials-avatar-sm { width: 32px; height: 32px; background: var(--primary-color); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 0.8rem; }
                .btn-view-modern { color: var(--primary-color); text-decoration: none; font-weight: 600; font-size: 0.9rem; transition: all 0.2s; }
                .btn-icon { background: none; border: 1px solid var(--glass-border); color: var(--text-secondary); padding: 8px 12px; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
                .btn-icon:hover { background: var(--glass-bg); color: #fff; border-color: #fff; }
            `}</style>
        </div>
    );
};

export default CaseList;
