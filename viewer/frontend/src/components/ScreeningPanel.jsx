import React, { useState, useEffect } from 'react';
import { screeningService } from '../services/screeningService';
import { useNotification } from '../contexts/NotificationContext';
import { FiUser, FiGlobe, FiAlertTriangle, FiShield, FiClock, FiPlay, FiSearch } from 'react-icons/fi';

const ScreeningCard = ({ title, context, result, onRun }) => {
    const isHit = result.status === 'HIT';
    const isNoHit = result.status === 'NO_HIT';
    const isInProgress = result.status === 'IN_PROGRESS';

    const getIcon = () => {
        switch (context) {
            case 'PEP': return <FiUser />;
            case 'ADM': return <FiAlertTriangle />;
            case 'INT': return <FiGlobe />;
            case 'SAN': return <FiShield />;
            default: return <FiSearch />;
        }
    };

    const getStatusLabel = () => {
        if (isInProgress) return 'Scanning...';
        if (isHit) return 'ALERT FOUND';
        if (isNoHit) return 'CLEAR';
        return 'NOT RUN';
    };

    const getStatusColor = () => {
        if (isInProgress) return 'var(--warning-color, #faad14)';
        if (isHit) return 'var(--error-color, #ff4d4f)';
        if (isNoHit) return 'var(--success-color, #52c41a)';
        return 'var(--text-muted, #8c8c8c)';
    };

    return (
        <div style={{
            background: 'rgba(255, 255, 255, 0.03)',
            border: `1px solid ${isHit ? 'rgba(255, 77, 79, 0.3)' : 'var(--glass-border, rgba(255, 255, 255, 0.1))'}`,
            borderRadius: '12px',
            padding: '1.2rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
            transition: 'all 0.3s ease',
            position: 'relative',
            overflow: 'hidden',
            boxShadow: isHit ? '0 0 15px rgba(255, 77, 79, 0.1)' : 'none'
        }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '8px',
                    background: 'rgba(255, 255, 255, 0.05)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '1.1rem',
                    color: getStatusColor()
                }}>
                    {getIcon()}
                </div>
                <div style={{ flex: 1 }}>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: '500' }}>{context}</div>
                    <div style={{ fontSize: '0.95rem', fontWeight: 'bold', color: 'var(--text-primary)' }}>{title}</div>
                </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '4px' }}>
                <div style={{
                    fontSize: '0.7rem',
                    fontWeight: 'bold',
                    padding: '4px 10px',
                    borderRadius: '20px',
                    background: `${getStatusColor()}20`,
                    color: getStatusColor(),
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                }}>
                    {getStatusLabel()}
                </div>
                {isInProgress && (
                    <div className="spinning" style={{ color: getStatusColor(), fontSize: '0.9rem' }}>
                        <FiSearch />
                    </div>
                )}
            </div>

            {isHit && result.alertMessage && (
                <div style={{
                    fontSize: '0.75rem',
                    color: 'var(--error-color, #ff4d4f)',
                    background: 'rgba(255, 77, 79, 0.05)',
                    padding: '8px',
                    borderRadius: '6px',
                    marginTop: '8px',
                    border: '1px solid rgba(255, 77, 79, 0.1)'
                }}>
                    {result.alertMessage}
                </div>
            )}
        </div>
    );
};

const ScreeningPanel = ({ clientId, hasPermission }) => {
    const { notify } = useNotification();
    const [status, setStatus] = useState('NOT_RUN'); // NOT_RUN, IN_PROGRESS, HIT, NO_HIT
    const [results, setResults] = useState([]); // [{contextType, status, alertMessage}]
    const [currentRequestId, setCurrentRequestId] = useState(null);
    const [historyOpen, setHistoryOpen] = useState(false);
    const [history, setHistory] = useState([]);

    // Auto-polling effect
    useEffect(() => {
        let intervalId;

        if (status === 'IN_PROGRESS' && currentRequestId) {
            intervalId = setInterval(async () => {
                await checkStatus(currentRequestId);
            }, 2000); // Poll every 2 seconds
        }

        return () => {
            if (intervalId) clearInterval(intervalId);
        };
    }, [status, currentRequestId]);

    // Initial load effect
    useEffect(() => {
        const loadInitialStatus = async () => {
            try {
                const log = await screeningService.getHistory(clientId);
                setHistory(log);

                if (log && log.length > 0) {
                    // Assuming API returns sorted desc by date, or we sort
                    // Sort just in case
                    const sorted = log.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
                    const latest = sorted[0];

                    if (latest.overallStatus === 'COMPLETED' || latest.overallStatus === 'HIT' || latest.overallStatus === 'NO_HIT' || latest.overallStatus === 'CLEAR') {
                        // We don't have the granular results here without calling status again
                        // So we should fetch the status details if we have an ID
                        if (latest.externalRequestID) {
                            await checkStatus(latest.externalRequestID, true);
                        }
                        // Fallback if checkStatus fails or returns nothing?
                        // checkStatus sets 'results' and 'status' state.
                    }
                }
            } catch (e) {
                console.error("Failed to load initial screening status", e);
            }
        };
        if (clientId) {
            loadInitialStatus();
        }
    }, [clientId]);

    const fetchHistory = async () => {
        try {
            const log = await screeningService.getHistory(clientId);
            setHistory(log);
        } catch (e) {
            console.error("Failed to load history", e);
        }
    };

    const runScreening = async () => {
        if (!hasPermission) return;
        try {
            const res = await screeningService.initiateScreening(clientId);
            setCurrentRequestId(res.requestId);
            setStatus('IN_PROGRESS');
            setResults([
                { contextType: 'PEP', status: 'IN_PROGRESS' },
                { contextType: 'ADM', status: 'IN_PROGRESS' },
                { contextType: 'INT', status: 'IN_PROGRESS' },
                { contextType: 'SAN', status: 'IN_PROGRESS' }
            ]);
            notify('Screening Initiated', 'info');
        } catch (e) {
            notify('Failed to start screening: ' + e.message, 'error');
            setStatus('NOT_RUN');
        }
    };

    const checkStatus = async (requestId, silent = false) => {
        try {
            const res = await screeningService.getScreeningStatus(requestId);
            setResults(res.results);

            // Determine overall status
            const anyInProgress = res.results.some(r => r.status === 'IN_PROGRESS');
            const anyHit = res.results.some(r => r.status === 'HIT');

            if (anyInProgress) {
                // Keep polling
            } else if (anyHit) {
                setStatus('HIT');
                if (!silent) notify('Screening Completed: Alert Found', 'warning');
            } else {
                setStatus('NO_HIT');
                if (!silent) notify('Screening Completed: No Hits', 'success');
            }
        } catch (e) {
            console.error('Error checking status', e);
            // Don't stop polling immediately on one error, maybe transient network issue?
            // But if 404/500, maybe stop. For now, strict.
        }
    };

    const getIndicatorColor = (resStatus) => {
        switch (resStatus) {
            case 'HIT': return '#ff4d4f'; // Red
            case 'NO_HIT': return '#52c41a'; // Green
            case 'IN_PROGRESS': return '#faad14'; // Yellow
            default: return '#555'; // Grey
        }
    };

    return (
        <div style={{
            flex: 1,
            background: 'rgba(255,255,255,0.03)',
            border: '1px solid var(--glass-border)',
            borderRadius: '8px',
            padding: '1.5rem',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            position: 'relative'
        }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', marginBottom: '1.5rem' }}>
                <h4 style={{ margin: 0, fontSize: '1.1rem', color: 'var(--text-primary)' }}>Screening Intelligence</h4>
                <div style={{ display: 'flex', gap: '8px' }}>
                    {hasPermission && (
                        <>
                            <button
                                onClick={() => { setHistoryOpen(true); fetchHistory(); }}
                                title="View History"
                                className="btn-icon"
                                style={{
                                    background: 'rgba(255,255,255,0.05)',
                                    border: '1px solid var(--glass-border)',
                                    borderRadius: '8px',
                                    color: 'var(--text-primary)',
                                    cursor: 'pointer',
                                    padding: '8px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    fontSize: '1.1rem',
                                    transition: 'all 0.2s ease'
                                }}
                            >
                                <FiClock />
                            </button>
                            <button
                                onClick={runScreening}
                                disabled={status === 'IN_PROGRESS'}
                                title="Run Screening"
                                className="btn-icon"
                                style={{
                                    background: 'var(--accent-primary)',
                                    border: 'none',
                                    borderRadius: '8px',
                                    color: 'black',
                                    cursor: 'pointer',
                                    padding: '8px 16px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '8px',
                                    fontSize: '0.9rem',
                                    fontWeight: '600',
                                    opacity: (status === 'IN_PROGRESS') ? 0.5 : 1,
                                    transition: 'all 0.2s ease'
                                }}
                            >
                                <FiPlay /> Run All
                            </button>
                        </>
                    )}
                </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', width: '100%' }}>
                <ScreeningCard
                    title="Politically Exposed Person"
                    context="PEP"
                    result={(results || []).find(r => r.contextType === 'PEP') || { status: 'NOT_RUN' }}
                />
                <ScreeningCard
                    title="Adverse Media"
                    context="ADM"
                    result={(results || []).find(r => r.contextType === 'ADM') || { status: 'NOT_RUN' }}
                />
                <ScreeningCard
                    title="International Sanctions"
                    context="SAN"
                    result={(results || []).find(r => r.contextType === 'SAN') || { status: 'NOT_RUN' }}
                />
                <ScreeningCard
                    title="Internal Watchlist"
                    context="INT"
                    result={(results || []).find(r => r.contextType === 'INT') || { status: 'NOT_RUN' }}
                />
            </div>

            {/* History Modal */}
            {
                historyOpen && (
                    <div style={{
                        position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                        background: 'rgba(0,0,0,0.8)', zIndex: 1000,
                        display: 'flex', justifyContent: 'center', alignItems: 'center'
                    }}>
                        <div style={{
                            background: '#1f1f1f', padding: '20px', borderRadius: '8px',
                            width: '500px', maxHeight: '80vh', overflowY: 'auto',
                            border: '1px solid var(--glass-border)'
                        }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '15px' }}>
                                <h3>Screening History</h3>
                                <button onClick={() => setHistoryOpen(false)} style={{ background: 'none', border: 'none', color: 'white', fontSize: '1.5rem', cursor: 'pointer' }}>×</button>
                            </div>
                            {history.map((h, idx) => (
                                <div key={idx} style={{ borderBottom: '1px solid #333', padding: '10px 0' }}>
                                    <div><strong>Date:</strong> {new Date(h.createdAt || Date.now()).toLocaleString()}</div>
                                    <div style={{ color: h.overallStatus === 'COMPLETED' ? '#52c41a' : '#faad14' }}>
                                        <strong>Status:</strong> {h.overallStatus}
                                    </div>
                                    <div style={{ fontSize: '0.8rem', color: '#aaa' }}>{h.externalRequestID}</div>
                                </div>
                            ))}
                        </div>
                    </div>
                )
            }
        </div >
    );
};

export default ScreeningPanel;
