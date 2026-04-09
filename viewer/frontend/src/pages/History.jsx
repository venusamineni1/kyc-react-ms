import React, { useState, useEffect } from 'react';
import { batchService } from '../services/batchService';
import { useNotification } from '../contexts/NotificationContext';
import { FaLayerGroup, FaFilter, FaCalendarAlt, FaSync, FaSearch } from 'react-icons/fa';
import './History.css'; // Assuming we might want specific styles

const History = () => {
    const { notify } = useNotification();
    const [history, setHistory] = useState([]);
    const [filteredHistory, setFilteredHistory] = useState([]);
    const [snapshotModal, setSnapshotModal] = useState(null); // { snapshot, batchName }
    const [loadingSnapshot, setLoadingSnapshot] = useState(false);
    const [filters, setFilters] = useState({
        service: { risk: true, screening: true },
        status: 'ALL',
        startDate: '',
        endDate: ''
    });
    const [sortConfig, setSortConfig] = useState({ key: 'createdAt', direction: 'desc' });

    useEffect(() => {
        loadHistory();
    }, []);

    useEffect(() => {
        applyFiltersAndSort();
    }, [history, filters, sortConfig]);

    const loadHistory = async () => {
        try {
            const [riskData, screeningData] = await Promise.all([
                batchService.getRiskHistory(),
                batchService.getScreeningHistory()
            ]);

            const merged = [
                ...(riskData || []).map(item => ({ ...item, service: 'Risk' })),
                ...(screeningData || []).map(item => ({ ...item, service: 'Screening' }))
            ];
            // Initial sort will be handled by applyFiltersAndSort
            setHistory(merged);
        } catch (error) {
            notify('Failed to load history', 'error');
            console.error(error);
        }
    };

    const applyFiltersAndSort = () => {
        let result = [...history];

        // Service Filter
        result = result.filter(item =>
            (item.service === 'Risk' && filters.service.risk) ||
            (item.service === 'Screening' && filters.service.screening)
        );

        // Status Filter
        if (filters.status !== 'ALL') {
            result = result.filter(item => item.runStatus === filters.status);
        }

        // Date Filter
        if (filters.startDate) {
            result = result.filter(item => new Date(item.createdAt) >= new Date(filters.startDate));
        }
        if (filters.endDate) {
            const end = new Date(filters.endDate);
            end.setHours(23, 59, 59, 999);
            result = result.filter(item => new Date(item.createdAt) <= end);
        }

        // Sorting
        result.sort((a, b) => {
            if (a[sortConfig.key] < b[sortConfig.key]) {
                return sortConfig.direction === 'asc' ? -1 : 1;
            }
            if (a[sortConfig.key] > b[sortConfig.key]) {
                return sortConfig.direction === 'asc' ? 1 : -1;
            }
            return 0;
        });

        setFilteredHistory(result);
    };

    const handleSort = (key) => {
        let direction = 'asc';
        if (sortConfig.key === key && sortConfig.direction === 'asc') {
            direction = 'desc';
        }
        setSortConfig({ key, direction });
    };

    const getSortIndicator = (key) => {
        if (sortConfig.key !== key) return '↕';
        return sortConfig.direction === 'asc' ? '↑' : '↓';
    };

    const handleServiceChange = (e) => {
        setFilters({
            ...filters,
            service: { ...filters.service, [e.target.name]: e.target.checked }
        });
    };

    const viewConfig = async (run) => {
        if (!run.batchID || run.service !== 'Screening') return;
        setLoadingSnapshot(true);
        try {
            const snapshot = await batchService.getMappingSnapshot(run.batchID);
            if (snapshot && snapshot.configJson) {
                setSnapshotModal({ snapshot, batchName: run.batchName });
            } else {
                notify('No mapping snapshot linked to this batch', 'info');
            }
        } catch (e) {
            notify('Failed to load mapping config', 'error');
        } finally {
            setLoadingSnapshot(false);
        }
    };

    return (
        <div className="container">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold">Batch History</h1>
                {/* Duplicate Refresh Button Removed */}
            </div>

            {/* Filters - Professional Bar */}
            <div className="glass-section mb-6" style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: '1.25rem', padding: '1rem', overflowX: 'auto' }}>

                {/* Service Filter */}
                <div className="flex items-center gap-3" style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
                    <div className="text-gray-400" title="Service Type">
                        <FaLayerGroup size={14} />
                    </div>
                    <div className="flex gap-3" style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
                        <label className="flex items-center cursor-pointer text-sm font-medium hover:text-white transition-colors">
                            <input
                                type="checkbox"
                                name="risk"
                                checked={filters.service.risk}
                                onChange={handleServiceChange}
                                className="mr-2 accent-red-500 rounded"
                            />
                            Risk
                        </label>
                        <label className="flex items-center cursor-pointer text-sm font-medium hover:text-white transition-colors">
                            <input
                                type="checkbox"
                                name="screening"
                                checked={filters.service.screening}
                                onChange={handleServiceChange}
                                className="mr-2 accent-blue-500 rounded"
                            />
                            Screening
                        </label>
                    </div>
                </div>

                <div className="h-6 w-px bg-gray-700"></div>

                {/* Status Filter */}
                <div className="flex items-center gap-3" style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
                    <div className="text-gray-400" title="Status">
                        <FaFilter size={14} />
                    </div>
                    <select
                        value={filters.status}
                        onChange={(e) => setFilters({ ...filters, status: e.target.value })}
                        className="glass-input glass-select"
                        style={{ minWidth: '160px' }}
                    >
                        <option value="ALL">All Statuses</option>
                        <option value="INITIATED">Initiated</option>
                        <option value="UPLOADED">Uploaded</option>
                        <option value="PROCESSED">Processed</option>
                        <option value="FAILED">Failed</option>
                        <option value="CHECKSUM_GENERATED">Checksum Generated</option>
                        <option value="ZIPPED">Zipped</option>
                    </select>
                </div>

                <div className="h-6 w-px bg-gray-700"></div>

                {/* Date Filter */}
                <div className="flex items-center gap-3" style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
                    <div className="text-gray-400" title="Date Range">
                        <FaCalendarAlt size={14} />
                    </div>
                    <div className="flex items-center gap-2" style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
                        <input
                            type="date"
                            value={filters.startDate}
                            onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
                            className="glass-input"
                            placeholder="Start"
                        />
                        <span className="text-gray-500 text-sm">to</span>
                        <input
                            type="date"
                            value={filters.endDate}
                            onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
                            className="glass-input"
                            placeholder="End"
                        />
                    </div>
                </div>

                <div className="ml-auto pl-4 border-l border-gray-700">
                    <button onClick={loadHistory} className="btn-icon-only" title="Refresh Data">
                        <FaSync size={14} />
                    </button>
                </div>
            </div>

            {/* Table */}
            <div className="glass-section overflow-x-auto p-0">
                <table className="w-full">
                    <thead>
                        <tr>
                            <th onClick={() => handleSort('service')} className="cursor-pointer hover:text-white transition-colors">
                                Service {getSortIndicator('service')}
                            </th>
                            <th onClick={() => handleSort('batchName')} className="cursor-pointer hover:text-white transition-colors">
                                Batch Name {getSortIndicator('batchName')}
                            </th>
                            <th onClick={() => handleSort('runStatus')} className="cursor-pointer hover:text-white transition-colors">
                                Run Status {getSortIndicator('runStatus')}
                            </th>
                            <th onClick={() => handleSort('notificationStatus')} className="cursor-pointer hover:text-white transition-colors">
                                Notification {getSortIndicator('notificationStatus')}
                            </th>
                            <th onClick={() => handleSort('feedbackCount')} className="cursor-pointer hover:text-white transition-colors">
                                Feedback {getSortIndicator('feedbackCount')}
                            </th>
                            <th onClick={() => handleSort('clientCount')} className="cursor-pointer hover:text-white transition-colors">
                                Clients {getSortIndicator('clientCount')}
                            </th>
                            <th onClick={() => handleSort('createdAt')} className="cursor-pointer hover:text-white transition-colors">
                                Created At {getSortIndicator('createdAt')}
                            </th>
                            <th>Config</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredHistory.map((run, index) => (
                            <tr key={`${run.service}-${run.batchID || index}`}>
                                <td>
                                    <span className={`badge ${run.service === 'Risk' ? 'badge-risk' : 'badge-screening'}`}>
                                        {run.service}
                                    </span>
                                </td>
                                <td className="font-medium text-white">{run.batchName}</td>
                                <td>
                                    <span className={`badge ${run.runStatus === 'PROCESSED' ? 'badge-success' :
                                        run.runStatus === 'FAILED' ? 'badge-error' : 'badge-warning'
                                        }`}>
                                        {run.runStatus}
                                    </span>
                                </td>
                                <td className="text-muted">{run.notificationStatus || '-'}</td>
                                <td className="text-muted">{run.feedbackCount || 0}</td>
                                <td className="text-muted">{run.clientCount || '-'}</td>
                                <td className="text-muted">
                                    {new Date(run.createdAt).toLocaleString()}
                                </td>
                                <td>
                                    {run.service === 'Screening' && run.mappingSnapshotID ? (
                                        <button
                                            onClick={() => viewConfig(run)}
                                            disabled={loadingSnapshot}
                                            title={`View mapping config snapshot #${run.mappingSnapshotID} used for this batch`}
                                            style={{
                                                background: 'rgba(168,85,247,0.12)', color: '#c084fc',
                                                border: '1px solid rgba(168,85,247,0.3)', borderRadius: '6px',
                                                padding: '3px 10px', fontSize: '0.75rem', fontWeight: 600,
                                                cursor: 'pointer', whiteSpace: 'nowrap',
                                            }}
                                        >
                                            ⚙ Snapshot #{run.mappingSnapshotID}
                                        </button>
                                    ) : run.service === 'Screening' ? (
                                        <span style={{ color: 'var(--text-secondary)', fontSize: '0.75rem' }}>No snapshot</span>
                                    ) : (
                                        <span className="text-muted">-</span>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {filteredHistory.length === 0 && (
                    <div className="p-12 text-center text-muted flex flex-col items-center gap-3">
                        <FaSearch size={24} className="text-gray-600" />
                        <p>No records found matching your filters.</p>
                    </div>
                )}
            </div>

            {/* Mapping Config Snapshot Modal */}
            {snapshotModal && (
                <div style={{
                    position: 'fixed', inset: 0, zIndex: 1000,
                    background: 'rgba(0,0,0,0.65)', display: 'flex',
                    alignItems: 'center', justifyContent: 'center', padding: '2rem',
                }} onClick={() => setSnapshotModal(null)}>
                    <div className="glass-section" style={{
                        maxWidth: '720px', width: '100%', maxHeight: '80vh',
                        overflow: 'auto', padding: '1.5rem',
                    }} onClick={(e) => e.stopPropagation()}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                            <div>
                                <h2 style={{ margin: 0, fontSize: '1.1rem' }}>
                                    Mapping Configuration {snapshotModal.snapshot.versionLabel}
                                </h2>
                                <p style={{ margin: '0.25rem 0 0', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                                    Batch: {snapshotModal.batchName} &nbsp;·&nbsp;
                                    Created: {new Date(snapshotModal.snapshot.createdAt).toLocaleString()} &nbsp;·&nbsp;
                                    By: {snapshotModal.snapshot.createdBy} ({snapshotModal.snapshot.source})
                                </p>
                            </div>
                            <button onClick={() => setSnapshotModal(null)} style={{
                                background: 'none', border: 'none', color: 'var(--text-secondary)',
                                fontSize: '1.2rem', cursor: 'pointer', padding: '0.25rem',
                            }}>✕</button>
                        </div>

                        <div style={{ overflowX: 'auto' }}>
                            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.82rem' }}>
                                <thead>
                                    <tr style={{ background: 'var(--hover-bg)' }}>
                                        <th style={{ padding: '0.6rem 1rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Target Path</th>
                                        <th style={{ padding: '0.6rem 1rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Source Field</th>
                                        <th style={{ padding: '0.6rem 1rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Default</th>
                                        <th style={{ padding: '0.6rem 1rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Transform</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {(() => {
                                        try {
                                            const configs = JSON.parse(snapshotModal.snapshot.configJson);
                                            return configs.map((cfg, i) => (
                                                <tr key={i} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                                                    <td style={{ padding: '0.5rem 1rem', fontFamily: 'monospace', color: '#c084fc' }}>{cfg.targetPath}</td>
                                                    <td style={{ padding: '0.5rem 1rem', color: 'var(--text-color)' }}>{cfg.sourceField || '-'}</td>
                                                    <td style={{ padding: '0.5rem 1rem', color: 'var(--text-muted)' }}>{cfg.defaultValue || '-'}</td>
                                                    <td style={{ padding: '0.5rem 1rem', color: 'var(--text-muted)' }}>{cfg.transformation || '-'}</td>
                                                </tr>
                                            ));
                                        } catch {
                                            return (
                                                <tr>
                                                    <td colSpan={4} style={{ padding: '1rem', color: 'var(--text-muted)' }}>
                                                        <pre style={{ whiteSpace: 'pre-wrap', fontSize: '0.8rem' }}>{snapshotModal.snapshot.configJson}</pre>
                                                    </td>
                                                </tr>
                                            );
                                        }
                                    })()}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default History;
