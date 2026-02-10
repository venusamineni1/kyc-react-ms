import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { caseService } from '../services/caseService';
import Pagination from '../components/Pagination';

const CaseList = () => {
    const [cases, setCases] = useState([]);
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [stats, setStats] = useState({
        total: 0,
        pending: 0,
        review: 0,
        dueToday: 0
    });

    const loadCases = async (page = 0) => {
        setLoading(true);
        try {
            const result = await caseService.getCases(page);
            setCases(result.content);
            setData(result);

            // Calculate some basic stats for the header
            if (result.content) {
                const pending = result.content.filter(c => c.status === 'PROCESSING' || c.status === 'KYC_ANALYST').length;
                const review = result.content.filter(c => c.status.includes('REVIEW')).length;
                setStats({
                    total: result.totalElements || result.content.length,
                    pending: pending,
                    review: review,
                    dueToday: Math.floor(pending * 0.3) // Mock stat for UI flavor
                });
            }

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
                <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h3 style={{ margin: 0 }}>Active Cases</h3>
                    <div className="table-actions">
                        <button className="btn-icon" title="Filter"><i className="fas fa-filter"></i></button>
                        <button className="btn-icon" title="Export" style={{ marginLeft: '10px' }}><i className="fas fa-download"></i></button>
                    </div>
                </div>

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
                                        <th>Case ID</th>
                                        <th>Client Name</th>
                                        <th>Status</th>
                                        <th>Created Date</th>
                                        <th>Assigned To</th>
                                        <th style={{ textAlign: 'right' }}>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {cases.map(kycCase => (
                                        <tr key={kycCase.caseID} className="table-row-hover">
                                            <td style={{ fontWeight: 'bold', color: 'var(--primary-color)' }}>
                                                #{kycCase.caseID}
                                            </td>
                                            <td>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                                    <div className="initials-avatar-sm">
                                                        {kycCase.clientName?.[0]}
                                                    </div>
                                                    {kycCase.clientName}
                                                </div>
                                            </td>
                                            <td>
                                                <span
                                                    className="status-badge-modern"
                                                    style={{
                                                        backgroundColor: `${getStatusColor(kycCase.status)}20`,
                                                        color: getStatusColor(kycCase.status),
                                                        border: `1px solid ${getStatusColor(kycCase.status)}40`
                                                    }}
                                                >
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
                                                <Link to={`/cases/${kycCase.caseID}`} className="btn-view-modern">
                                                    View Details
                                                </Link>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <div style={{ padding: '1rem', borderTop: '1px solid var(--glass-border)' }}>
                            <Pagination data={data} onPageChange={loadCases} />
                        </div>
                    </>
                )}
            </div>

            <style>{`
                .stat-pill {
                    background: var(--glass-bg);
                    border: 1px solid var(--glass-border);
                    padding: 0.75rem 1.25rem;
                    border-radius: 12px;
                    display: flex;
                    flex-direction: column;
                    min-width: 120px;
                }
                .stat-label {
                    font-size: 0.75rem;
                    text-transform: uppercase;
                    letter-spacing: 0.05em;
                    color: var(--text-secondary);
                }
                .stat-value {
                    font-size: 1.5rem;
                    font-weight: 700;
                    color: #fff;
                }
                .modern-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .modern-table th {
                    text-align: left;
                    padding: 1rem 1.5rem;
                    font-size: 0.85rem;
                    text-transform: uppercase;
                    letter-spacing: 0.05em;
                    color: var(--text-secondary);
                    background: rgba(255, 255, 255, 0.02);
                }
                .modern-table td {
                    padding: 1.25rem 1.5rem;
                    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
                    font-size: 0.95rem;
                }
                .table-row-hover:hover {
                    background: rgba(255, 255, 255, 0.03);
                }
                .status-badge-modern {
                    display: inline-flex;
                    align-items: center;
                    gap: 8px;
                    padding: 4px 12px;
                    border-radius: 20px;
                    font-size: 0.8rem;
                    font-weight: 600;
                    text-transform: uppercase;
                }
                .status-dot {
                    width: 6px;
                    height: 6px;
                    border-radius: 50%;
                }
                .initials-avatar-sm {
                    width: 32px;
                    height: 32px;
                    background: var(--primary-color);
                    color: white;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-weight: bold;
                    font-size: 0.8rem;
                }
                .btn-view-modern {
                    color: var(--primary-color);
                    text-decoration: none;
                    font-weight: 600;
                    font-size: 0.9rem;
                    transition: all 0.2s;
                }
                .btn-icon {
                    background: none;
                    border: 1px solid var(--glass-border);
                    color: var(--text-secondary);
                    padding: 8px;
                    border-radius: 8px;
                    cursor: pointer;
                    transition: all 0.2s;
                }
                .btn-icon:hover {
                    background: var(--glass-bg);
                    color: #fff;
                    border-color: #fff;
                }
            `}</style>
        </div>
    );
};

export default CaseList;
