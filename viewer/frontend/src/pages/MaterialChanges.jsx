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
                                <th>ID</th>
                                <th>Date/Time</th>
                                <th>Client</th>
                                <th>Entity</th>
                                <th>Field</th>
                                <th>Operation</th>
                                <th>Old Value</th>
                                <th>New Value</th>
                            </tr>
                        </thead>
                        <tbody>
                            {changes.map(change => (
                                <tr key={change.changeID}>
                                    <td>#{change.changeID}</td>
                                    <td style={{ fontSize: '0.85rem' }}>{new Date(change.changeDate).toLocaleString()}</td>
                                    <td>{change.clientName || change.clientID}</td>
                                    <td>{change.entityName}</td>
                                    <td><strong>{change.columnName}</strong></td>
                                    <td><span className="status-badge" style={{ backgroundColor: 'rgba(255,255,255,0.1)' }}>{change.operationType}</span></td>
                                    <td style={{ color: '#ff6b6b', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }} title={change.oldValue}>{change.oldValue || '-'}</td>
                                    <td style={{ color: '#51cf66', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }} title={change.newValue}>{change.newValue || '-'}</td>
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
