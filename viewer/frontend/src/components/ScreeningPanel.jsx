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
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            padding: '0.45rem 0.75rem',
            borderRadius: '8px',
            border: `1px solid ${isHit ? 'rgba(255, 77, 79, 0.3)' : 'var(--glass-border, rgba(255, 255, 255, 0.1))'}`,
            background: isHit ? 'rgba(255, 77, 79, 0.04)' : 'rgba(255, 255, 255, 0.02)',
            boxShadow: isHit ? '0 0 10px rgba(255, 77, 79, 0.08)' : 'none',
            transition: 'all 0.2s ease',
        }}>
            {/* Icon */}
            <div style={{ color: getStatusColor(), fontSize: '1rem', flexShrink: 0, opacity: 0.9 }}>
                {getIcon()}
            </div>

            {/* Context tag + title */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flex: 1, minWidth: 0 }}>
                <span style={{
                    fontSize: '0.65rem', fontWeight: 700, color: 'var(--text-secondary)',
                    background: 'rgba(255,255,255,0.07)', padding: '1px 5px', borderRadius: '4px',
                    flexShrink: 0,
                }}>
                    {context}
                </span>
                <span style={{
                    fontSize: '0.83rem', color: 'var(--text-primary)',
                    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                }}>
                    {title}
                </span>
            </div>

            {/* Spinner when in progress */}
            {isInProgress && (
                <div className="spinning" style={{ color: getStatusColor(), fontSize: '0.85rem', flexShrink: 0 }}>
                    <FiSearch />
                </div>
            )}

            {/* Status badge */}
            <span style={{
                fontSize: '0.65rem', fontWeight: 700,
                padding: '2px 8px', borderRadius: '10px',
                background: `${getStatusColor()}20`, color: getStatusColor(),
                textTransform: 'uppercase', letterSpacing: '0.4px',
                flexShrink: 0, whiteSpace: 'nowrap',
            }}>
                {getStatusLabel()}
            </span>

            {/* Alert message inline if hit */}
            {isHit && result.alertMessage && (
                <span title={result.alertMessage} style={{
                    fontSize: '0.7rem', color: 'var(--error-color, #ff4d4f)',
                    maxWidth: '120px', overflow: 'hidden', textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap', flexShrink: 0, cursor: 'help',
                }}>
                    ⚠ {result.alertMessage}
                </span>
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
    
    // Detailed Analysis State
    const [analyzeModalOpen, setAnalyzeModalOpen] = useState(false);
    const [analyzeResult, setAnalyzeResult] = useState(null);
    const [loadingAnalyze, setLoadingAnalyze] = useState(false);

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

                    const reqId = latest.externalRequestID || latest.externalRequestId;
                    if (latest.overallStatus === 'COMPLETED' || latest.overallStatus === 'HIT' || latest.overallStatus === 'NO_HIT' || latest.overallStatus === 'CLEAR') {
                        if (reqId) {
                            await checkStatus(reqId, true);
                        }
                    } else if (latest.overallStatus === 'IN_PROGRESS') {
                        if (reqId) {
                            setCurrentRequestId(reqId);
                            setStatus('IN_PROGRESS');
                            await checkStatus(reqId, true);
                        }
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

    const handleAnalyze = async (reqId) => {
        if (!reqId) return;
        setAnalyzeModalOpen(true);
        setLoadingAnalyze(true);
        try {
            const res = await screeningService.getScreeningStatus(reqId);
            setAnalyzeResult(res);
        } catch (e) {
            notify('Failed to load screening analysis details', 'error');
            setAnalyzeModalOpen(false);
        } finally {
            setLoadingAnalyze(false);
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
            background: 'rgba(255,255,255,0.02)',
            border: '1px solid var(--glass-border)',
            borderRadius: '8px',
            padding: '0.85rem',
            display: 'flex',
            flexDirection: 'column',
            position: 'relative'
        }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', marginBottom: '0.75rem' }}>
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

            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', width: '100%' }}>
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
                            width: '550px', maxHeight: '80vh', overflowY: 'auto',
                            border: '1px solid var(--glass-border)'
                        }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '15px' }}>
                                <h3>Screening History</h3>
                                <button onClick={() => setHistoryOpen(false)} style={{ background: 'none', border: 'none', color: 'white', fontSize: '1.5rem', cursor: 'pointer' }}>×</button>
                            </div>
                            {history.length === 0 ? <p style={{ color: 'var(--text-secondary)' }}>No screening history recorded.</p> : history.map((h, idx) => (
                                <div key={idx} style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', padding: '12px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div>
                                        <div style={{ fontSize: '0.9rem' }}><strong>Date:</strong> {new Date(h.createdAt || Date.now()).toLocaleString()}</div>
                                        <div style={{ color: h.overallStatus === 'COMPLETED' || h.overallStatus === 'CLEAR' || h.overallStatus === 'NO_HIT' ? '#52c41a' : '#faad14', margin: '4px 0', fontWeight: '500' }}>
                                            <strong>Status:</strong> {h.overallStatus}
                                        </div>
                                        <div style={{ fontSize: '0.75rem', color: '#888' }}>ID: {h.externalRequestID || h.externalRequestId}</div>
                                    </div>
                                    <div>
                                        <button 
                                            onClick={() => handleAnalyze(h.externalRequestID || h.externalRequestId)} 
                                            style={{
                                                background: 'var(--glass-bg)', border: '1px solid var(--glass-border)',
                                                color: '#fff', padding: '6px 12px', borderRadius: '6px', cursor: 'pointer', fontSize: '0.8rem'
                                            }}
                                        >
                                            Analyze
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )
            }

            {/* Analysis Breakdown Modal */}
            {
                analyzeModalOpen && (
                    <div style={{
                        position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                        background: 'rgba(0,0,0,0.85)', zIndex: 2000,
                        display: 'flex', justifyContent: 'center', alignItems: 'center'
                    }}>
                        <div style={{
                            background: '#1f1f1f', padding: '25px', borderRadius: '12px',
                            width: '650px', maxHeight: '85vh', overflowY: 'auto',
                            border: '1px solid var(--accent-primary)',
                            boxShadow: '0 0 30px rgba(0, 242, 254, 0.1)'
                        }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '10px' }}>
                                <h3 style={{ margin: 0, color: 'var(--accent-primary)' }}>Screening Matrix Analysis</h3>
                                <button onClick={() => { setAnalyzeModalOpen(false); setAnalyzeResult(null); }} style={{ background: 'none', border: 'none', color: 'white', fontSize: '1.5rem', cursor: 'pointer' }}>×</button>
                            </div>
                            
                            {loadingAnalyze ? (
                                <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>Decrypting response matrix...</div>
                            ) : analyzeResult ? (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                    <div style={{ background: 'rgba(255,255,255,0.05)', padding: '1rem', borderRadius: '8px', fontSize: '0.9rem' }}>
                                        <strong>Engine Status:</strong> {analyzeResult.overallStatus || 'No Hits'} <br/>
                                        <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '4px' }}>Transaction UUID: {analyzeResult.requestId || 'Unknown'}</div>
                                    </div>

                                    <h4 style={{ margin: '10px 0 0 0' }}>Watchlist Hits & Warnings</h4>
                                    {(analyzeResult.results || []).filter(r => r.status === 'HIT' || r.alertMessage).length > 0 ? (
                                        (analyzeResult.results || []).filter(r => r.status === 'HIT' || r.alertMessage).map((match, i) => (
                                            <div key={i} style={{ padding: '15px', background: 'rgba(255, 77, 79, 0.05)', border: '1px solid rgba(255, 77, 79, 0.2)', borderRadius: '8px' }}>
                                                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                                    <span style={{ fontWeight: 'bold', color: '#ff4d4f' }}>{match.contextType} Watchlist</span>
                                                    <span style={{ background: 'rgba(255,77,79,0.2)', padding: '2px 8px', borderRadius: '12px', fontSize: '0.7rem', color: '#ff4d4f' }}>HIT</span>
                                                </div>
                                                <div style={{ marginTop: '8px', fontSize: '0.9rem', color: 'var(--text-secondary)', background: 'rgba(0,0,0,0.3)', padding: '10px', borderRadius: '4px' }}>
                                                    {match.alertMessage}
                                                </div>
                                            </div>
                                        ))
                                    ) : (
                                        <div style={{ padding: '2rem', textAlign: 'center', background: 'rgba(82, 196, 26, 0.05)', border: '1px solid rgba(82, 196, 26, 0.2)', borderRadius: '8px', color: '#52c41a' }}>
                                            No active hits were detected across standard risk domains for this request.
                                        </div>
                                    )}

                                    <h4 style={{ margin: '15px 0 5px 0' }}>Cleared Domains</h4>
                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                                        {(analyzeResult.results || []).filter(r => r.status === 'NO_HIT' || r.status === 'CLEAR').map((match, i) => (
                                            <div key={i} style={{ padding: '10px', background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: '6px', fontSize: '0.85rem' }}>
                                                ✅ {match.contextType} Screened: Clear
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            ) : <p style={{ color: 'var(--text-secondary)' }}>Failed to parse analysis blob.</p>}
                        </div>
                    </div>
                )
            }
        </div >
    );
};

export default ScreeningPanel;
