import React, { useState, useEffect } from 'react';
import Pagination from '../components/Pagination';
import apiClient from '../services/apiClient';

const MaterialChanges = () => {
    const [changes, setChanges] = useState([]);
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [page, setPage] = useState(0);

    const loadChanges = async (p = 0) => {
        setLoading(true);
        try {
            const result = await apiClient.get(`/clients/changes?page=${p}`);
            setChanges(result.content);
            setData(result);
            setPage(p);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadChanges();
    }, []);

    const handleAction = async (changeId, action) => {
        try {
            if (action === 'RISK') await clientService.triggerRisk(changeId);
            if (action === 'SCREENING') await clientService.triggerScreening(changeId);
            if (action === 'REVIEW') await clientService.reviewChange(changeId);
            loadChanges(page);
        } catch (err) {
            alert('Action failed: ' + err.message);
        }
    };

    return (
        <div className="glass-section">
            <h2 style={{ marginBottom: '1.5rem' }}>Material Changes Audit Log</h2>

            {loading && !changes.length ? (
                <p className="loading">Loading audit trail...</p>
            ) : error ? (
                <p className="error">{error}</p>
            ) : (
                <>
                    <table>
                        <thead>
                            <tr>
                                <th>Date/Time</th>
                                <th>Client</th>
                                <th>Category</th>
                                <th>Status</th>
                                <th>Field</th>
                                <th>Old Value</th>
                                <th>New Value</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {changes.map(change => (
                                <tr key={change.changeID}>
                                    <td style={{ fontSize: '0.8rem' }}>{new Date(change.changeDate).toLocaleString()}</td>
                                    <td>{change.clientName || change.clientID}</td>
                                    <td>
                                        <span className={`status-badge ${change.category === 'RISK' ? 'suspended' : change.category === 'SCREENING' ? 'pending' : 'active'}`} style={{ fontSize: '0.7rem' }}>
                                            {change.category}
                                        </span>
                                    </td>
                                    <td>
                                        <span className={`status-badge ${change.status === 'PENDING' ? 'rejected' : 'active'}`} style={{ fontSize: '0.7rem' }}>
                                            {change.status}
                                        </span>
                                    </td>
                                    <td><strong>{change.columnName}</strong></td>
                                    <td style={{ color: '#ff6b6b', fontSize: '0.85rem' }}>{change.oldValue || '-'}</td>
                                    <td style={{ color: '#51cf66', fontSize: '0.85rem' }}>{change.newValue || '-'}</td>
                                    <td>
                                        {change.status === 'PENDING' && (
                                            <div style={{ display: 'flex', gap: '5px' }}>
                                                {(change.category === 'RISK' || change.category === 'BOTH') && (
                                                    <button onClick={() => handleAction(change.changeID, 'RISK')} className="btn btn-secondary" style={{ padding: '2px 6px', fontSize: '0.7rem' }}>Risk</button>
                                                )}
                                                {(change.category === 'SCREENING' || change.category === 'BOTH') && (
                                                    <button onClick={() => handleAction(change.changeID, 'SCREENING')} className="btn btn-secondary" style={{ padding: '2px 6px', fontSize: '0.7rem' }}>Screen</button>
                                                )}
                                                <button onClick={() => handleAction(change.changeID, 'REVIEW')} className="btn btn-secondary" style={{ padding: '2px 6px', fontSize: '0.7rem', border: '1px solid #52c41a' }}>Done</button>
                                            </div>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <Pagination data={data} onPageChange={loadChanges} />
                </>
            )}
        </div>
    );
};

export default MaterialChanges;
