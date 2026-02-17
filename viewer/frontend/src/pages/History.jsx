import React, { useState, useEffect } from 'react';
import { batchService } from '../services/batchService';
import { useNotification } from '../contexts/NotificationContext';
import { FaLayerGroup, FaFilter, FaCalendarAlt, FaSync, FaSearch } from 'react-icons/fa';
import './History.css'; // Assuming we might want specific styles

const History = () => {
    const { notify } = useNotification();
    const [history, setHistory] = useState([]);
    const [filteredHistory, setFilteredHistory] = useState([]);
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
                            <th onClick={() => handleSort('createdAt')} className="cursor-pointer hover:text-white transition-colors">
                                Created At {getSortIndicator('createdAt')}
                            </th>
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
                                <td className="text-muted">
                                    {new Date(run.createdAt).toLocaleString()}
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
        </div>
    );
};

export default History;
