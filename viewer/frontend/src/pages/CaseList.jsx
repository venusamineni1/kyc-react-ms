import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { caseService } from '../services/caseService';
import Pagination from '../components/Pagination';
import Button from '../components/Button';
import { FiUser } from 'react-icons/fi';
import { useNotification } from '../contexts/NotificationContext';
import { getAgingInfo, AGING_BADGE_VARIANT } from '../utils/caseAging';

const ALL_STATUSES = [
    'PROCESSING', 'KYC_ANALYST', 'REVIEWER_REVIEW', 'AFC_REVIEW', 'ACO_REVIEW', 'APPROVED', 'REJECTED'
];

const PAGE_SIZE = 20;

const CaseList = () => {
    const { notify } = useNotification();
    const [allCases, setAllCases] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [stats, setStats] = useState({ total: 0, pending: 0, review: 0 });

    // Filters
    const [filters, setFilters] = useState({ status: '', assignee: '', search: '' });
    const [showFilters, setShowFilters] = useState(false);

    // Pagination (client-side - the backend currently returns the full case list in one page)
    const [page, setPage] = useState(0);

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

    // Reset to page 1 whenever the filtered result set changes shape
    useEffect(() => { setPage(0); }, [filters]);

    const totalPages = Math.max(1, Math.ceil(filteredCases.length / PAGE_SIZE));
    const currentPage = Math.min(page, totalPages - 1);
    const cases = useMemo(
        () => filteredCases.slice(currentPage * PAGE_SIZE, currentPage * PAGE_SIZE + PAGE_SIZE),
        [filteredCases, currentPage]
    );
    const paginationData = { currentPage, totalPages, totalElements: filteredCases.length };

    const getStatusVariant = (status) => {
        switch (status) {
            case 'APPROVED': return 'active';
            case 'REJECTED': return 'rejected';
            case 'PROCESSING':
            case 'KYC_ANALYST': return 'info';
            case 'REVIEWER_REVIEW':
            case 'AFC_REVIEW':
            case 'ACO_REVIEW': return 'pending';
            default: return 'info';
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
                    <div className="stat-pill" style={{ borderColor: 'var(--info-color)' }}>
                        <span className="stat-label" style={{ color: 'var(--info-color)' }}>Pending</span>
                        <span className="stat-value">{stats.pending}</span>
                    </div>
                    <div className="stat-pill" style={{ borderColor: 'var(--warning-color)' }}>
                        <span className="stat-label" style={{ color: 'var(--warning-color)' }}>In Review</span>
                        <span className="stat-value">{stats.review}</span>
                    </div>
                </div>
            </header>

            <div className="glass-section" style={{ padding: '0' }}>
                <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
                    <h3 style={{ margin: 0 }}>Active Cases</h3>
                    <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
                        <button className="btn-icon" title="Toggle Filters" aria-pressed={showFilters} onClick={() => setShowFilters(f => !f)}
                            style={{ background: showFilters ? 'var(--hover-bg)' : 'none', borderColor: showFilters ? 'var(--primary-color)' : undefined, color: showFilters ? 'var(--primary-color)' : undefined }}>
                            Filter {filters.status || filters.assignee || filters.search ? '●' : ''}
                        </button>
                        <button className="btn-icon" title="Export All" onClick={() => exportCSV(cases)}>Export</button>
                    </div>
                </div>

                {/* Filter bar */}
                {showFilters && (
                    <div style={{ padding: '1rem 1.5rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', gap: '1rem', flexWrap: 'wrap', background: 'rgba(255,255,255,0.02)' }}>
                        <input
                            aria-label="Search by name or ID"
                            placeholder="Search by name or ID..."
                            value={filters.search}
                            onChange={e => setFilters(f => ({ ...f, search: e.target.value }))}
                            style={{ flex: '1', minWidth: '160px', padding: '0.5rem 0.75rem', background: 'rgba(255,255,255,0.07)', border: '1px solid var(--glass-border)', borderRadius: '8px', color: '#fff' }}
                        />
                        <select
                            aria-label="Filter by status"
                            value={filters.status}
                            onChange={e => setFilters(f => ({ ...f, status: e.target.value }))}
                            style={{ padding: '0.5rem 0.75rem', background: 'rgba(255,255,255,0.07)', border: '1px solid var(--glass-border)', borderRadius: '8px', color: '#fff' }}
                        >
                            <option value="">All Statuses</option>
                            {ALL_STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
                        </select>
                        <input
                            aria-label="Filter by assignee"
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
                    <div style={{ padding: '0.75rem 1.5rem', borderBottom: '1px solid var(--glass-border)', background: 'var(--hover-bg)', display: 'flex', alignItems: 'center', gap: '1rem' }}>
                        <span style={{ color: 'var(--primary-color)', fontWeight: 600 }}>{selectedCases.size} selected</span>
                        <Button onClick={openBulkAssign} style={{ fontSize: '0.9rem', padding: '0.4rem 1rem' }}>
                            Assign To...
                        </Button>
                        <Button variant="ghost" onClick={() => exportCSV(cases.filter(c => selectedCases.has(c.caseID)))} style={{ fontSize: '0.9rem', padding: '0.4rem 1rem' }}>
                            Export Selected
                        </Button>
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
                                                aria-label="Select all cases on this page"
                                                checked={cases.length > 0 && selectedCases.size === cases.length}
                                                onChange={toggleSelectAll}
                                                style={{ cursor: 'pointer' }}
                                            />
                                        </th>
                                        <th>Case ID</th>
                                        <th>Client Name</th>
                                        <th>Status</th>
                                        <th>Created Date</th>
                                        <th>Age</th>
                                        <th>Assigned To</th>
                                        <th style={{ textAlign: 'right' }}>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {cases.length === 0 ? (
                                        <tr><td colSpan="8" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>No cases match the current filters.</td></tr>
                                    ) : cases.map(kycCase => (
                                        <tr key={kycCase.caseID} className="table-row-hover"
                                            style={{ background: selectedCases.has(kycCase.caseID) ? 'var(--hover-bg)' : undefined }}>
                                            <td>
                                                <input type="checkbox"
                                                    aria-label={`Select case ${kycCase.caseID} (${kycCase.clientName || 'unnamed client'})`}
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
                                                <span className={`status-badge-modern ${getStatusVariant(kycCase.status)}`}>
                                                    <span className="status-dot"></span>
                                                    {kycCase.status?.replace(/_/g, ' ')}
                                                </span>
                                            </td>
                                            <td style={{ color: 'var(--text-secondary)' }}>
                                                {new Date(kycCase.createdDate).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}
                                            </td>
                                            <td>
                                                {(() => {
                                                    const aging = getAgingInfo(kycCase.createdDate, kycCase.status);
                                                    if (aging.days === null) return null;
                                                    return (
                                                        <span className={`status-badge-modern ${AGING_BADGE_VARIANT[aging.level]}`} title={`${aging.days} day(s) since case creation`}>
                                                            <span className="status-dot"></span>
                                                            {aging.days}d
                                                        </span>
                                                    );
                                                })()}
                                            </td>
                                            <td>
                                                {kycCase.assignedTo ? (
                                                    <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                                                        <FiUser size={13} style={{ opacity: 0.7 }} />
                                                        {kycCase.assignedTo}
                                                    </div>
                                                ) : (
                                                    <span style={{ color: 'var(--danger-color)', fontSize: '0.85rem', fontWeight: '500' }}>Unassigned</span>
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
                            <Pagination data={paginationData} onPageChange={setPage} />
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
                            aria-label="Select user to assign cases to"
                            value={bulkAssignUser}
                            onChange={e => setBulkAssignUser(e.target.value)}
                            style={{ width: '100%', padding: '0.6rem', marginBottom: '1.5rem', background: 'rgba(255,255,255,0.07)', border: '1px solid var(--glass-border)', borderRadius: '8px', color: '#fff' }}
                        >
                            <option value="">Select user...</option>
                            {users.map(u => <option key={u.username} value={u.username}>{u.username}</option>)}
                        </select>
                        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                            <Button variant="ghost" onClick={() => { setShowBulkModal(false); setBulkAssignUser(''); }}>
                                Cancel
                            </Button>
                            <Button onClick={handleBulkAssign} disabled={!bulkAssignUser || bulkLoading}>
                                {bulkLoading ? 'Assigning...' : 'Assign'}
                            </Button>
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
                .initials-avatar-sm { width: 32px; height: 32px; background: var(--primary-color); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 0.8rem; }
                .btn-view-modern { color: var(--primary-color); text-decoration: none; font-weight: 600; font-size: 0.9rem; transition: all 0.2s; }
                .btn-icon { background: none; border: 1px solid var(--glass-border); color: var(--text-secondary); padding: 8px 12px; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
                .btn-icon:hover { background: var(--glass-bg); color: #fff; border-color: #fff; }
            `}</style>
        </div>
    );
};

export default CaseList;
