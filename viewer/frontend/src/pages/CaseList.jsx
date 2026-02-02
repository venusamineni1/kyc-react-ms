import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { caseService } from '../services/caseService';
import Pagination from '../components/Pagination';

const CaseList = () => {
    const [cases, setCases] = useState([]);
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const loadCases = async (page = 0) => {
        setLoading(true);
        try {
            const result = await caseService.getCases(page);
            setCases(result.content);
            setData(result);
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCases();
    }, []);

    return (
        <div className="glass-section">
            <h2 style={{ marginBottom: '1.5rem' }}>Case Management</h2>

            {loading && !cases.length ? (
                <p className="loading">Loading cases...</p>
            ) : error ? (
                <p className="error">{error}</p>
            ) : (
                <>
                    <table>
                        <thead>
                            <tr>
                                <th>Case ID</th>
                                <th>Client Name</th>
                                <th>Status</th>
                                <th>Created Date</th>
                                <th>Assigned To</th>
                            </tr>
                        </thead>
                        <tbody>
                            {cases.map(kycCase => (
                                <tr key={kycCase.caseID}>
                                    <td>
                                        <Link to={`/cases/${kycCase.caseID}`}>
                                            #{kycCase.caseID}
                                        </Link>
                                    </td>
                                    <td>{kycCase.clientName}</td>
                                    <td><span className="status-badge">{kycCase.status}</span></td>
                                    <td>{new Date(kycCase.createdDate).toLocaleDateString()}</td>
                                    <td>{kycCase.assignedTo || 'Unassigned'}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <Pagination data={data} onPageChange={loadCases} />
                </>
            )}
        </div>
    );
};

export default CaseList;
