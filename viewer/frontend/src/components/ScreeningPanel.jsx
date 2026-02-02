import React, { useState, useEffect } from 'react';
import { screeningService } from '../services/screeningService';
import { useNotification } from '../contexts/NotificationContext';
// Icons: using emoji or simple svg for now if no icon lib
// Or assuming we have a Button component

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

    const checkStatus = async (requestId) => {
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
                notify('Screening Completed: Alert Found', 'warning');
            } else {
                setStatus('NO_HIT');
                notify('Screening Completed: No Hits', 'success');
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
            <style>
                {`
                @keyframes pulse-yellow {
                    0% { box-shadow: 0 0 0 0 rgba(250, 173, 20, 0.4); }
                    70% { box-shadow: 0 0 0 10px rgba(250, 173, 20, 0); }
                    100% { box-shadow: 0 0 0 0 rgba(250, 173, 20, 0); }
                }
                `}
            </style>
            <div style={{ position: 'absolute', top: '10px', right: '10px', display: 'flex', gap: '5px' }}>
                {hasPermission && (
                    <>
                        <button
                            onClick={() => { setHistoryOpen(true); fetchHistory(); }}
                            title="View History"
                            style={{
                                background: 'transparent',
                                border: '1px solid var(--glass-border)',
                                borderRadius: '4px',
                                color: 'var(--text-primary)',
                                cursor: 'pointer',
                                padding: '4px 8px',
                                fontSize: '1.2rem'
                            }}
                        >
                            🕒
                        </button>
                        <button
                            onClick={runScreening}
                            disabled={status === 'IN_PROGRESS'}
                            title="Run Screening"
                            style={{
                                background: 'transparent',
                                border: '1px solid var(--glass-border)',
                                borderRadius: '4px',
                                color: 'var(--text-primary)',
                                cursor: 'pointer',
                                padding: '4px 8px',
                                fontSize: '1.2rem',
                                opacity: (status === 'IN_PROGRESS') ? 0.5 : 1
                            }}
                        >
                            ▶️
                        </button>
                    </>
                )}
            </div>

            <h4 style={{ margin: '0 0 1rem 0', color: 'var(--text-secondary)' }}>Screening Status</h4>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', width: '100%' }}>
                {['PEP', 'ADM', 'INT', 'SAN'].map(ctx => {
                    const result = results.find(r => r.contextType === ctx) || { status: 'NOT_RUN' };
                    const isInProgress = result.status === 'IN_PROGRESS';

                    return (
                        <div key={ctx} title={result.alertMessage} style={{
                            background: getIndicatorColor(result.status),
                            color: 'white',
                            padding: '10px',
                            borderRadius: '4px',
                            textAlign: 'center',
                            fontWeight: 'bold',
                            fontSize: '0.9rem',
                            cursor: result.status === 'HIT' ? 'help' : 'default',
                            animation: isInProgress ? 'pulse-yellow 2s infinite' : 'none'
                        }}>
                            {ctx}
                            {isInProgress && <span style={{ display: 'block', fontSize: '0.7rem' }}>Scanning...</span>}
                            {result.status === 'HIT' && <span style={{ display: 'block', fontSize: '0.7rem' }}>HIT</span>}
                        </div>
                    );
                })}
            </div>

            {/* History Modal */}
            {historyOpen && (
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
            )}
        </div>
    );
};

export default ScreeningPanel;
