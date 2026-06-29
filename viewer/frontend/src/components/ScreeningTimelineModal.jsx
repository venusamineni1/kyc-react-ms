import React, { useState, useEffect } from 'react';
import { FiUpload, FiRefreshCw, FiFileText, FiFile, FiX, FiChevronDown, FiChevronUp } from 'react-icons/fi';
import { screeningService } from '../services/screeningService';

const TYPE_META = {
    SUBMIT:          { icon: <FiUpload />,    label: 'Submitted to NRTS',        color: '#60a5fa' },
    STATUS_POLL:     { icon: <FiRefreshCw />, label: 'Status Poll',              color: '#fbbf24' },
    ALERT_DETAILS:   { icon: <FiFileText />,  label: 'Alert Details Fetched',    color: '#c084fc' },
    DOCUMENT_FETCH:  { icon: <FiFile />,      label: 'Document Fetched',         color: '#94a3b8' },
};

function TimelineRow({ interaction, index, visible }) {
    const [expanded, setExpanded] = useState(false);
    const meta = TYPE_META[interaction.interactionType] || { icon: <FiFile />, label: interaction.interactionType, color: '#94a3b8' };

    return (
        <div style={{
            display: 'flex', gap: '12px',
            opacity: visible ? 1 : 0,
            transform: visible ? 'translateY(0)' : 'translateY(8px)',
            transition: `opacity 0.35s ease ${index * 0.12}s, transform 0.35s ease ${index * 0.12}s`,
        }}>
            {/* Rail */}
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flexShrink: 0 }}>
                <div style={{
                    width: '30px', height: '30px', borderRadius: '50%',
                    background: `${meta.color}20`, border: `1px solid ${meta.color}50`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    color: meta.color, fontSize: '0.9rem',
                }}>
                    {meta.icon}
                </div>
                <div style={{ width: '2px', flex: 1, background: 'rgba(255,255,255,0.1)', marginTop: '4px' }} />
            </div>

            {/* Content */}
            <div style={{ flex: 1, paddingBottom: '18px', minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                    <span style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-primary)' }}>{meta.label}</span>
                    {interaction.isFinal && (
                        <span style={{
                            fontSize: '0.65rem', fontWeight: 700, color: '#10b981',
                            background: 'rgba(16,185,129,0.12)', border: '1px solid rgba(16,185,129,0.3)',
                            padding: '1px 6px', borderRadius: '10px', textTransform: 'uppercase',
                        }}>
                            Final
                        </span>
                    )}
                    {interaction.httpStatus && (
                        <span style={{ fontSize: '0.7rem', color: 'var(--text-secondary)' }}>HTTP {interaction.httpStatus}</span>
                    )}
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '2px' }}>
                    {interaction.createdAt ? new Date(interaction.createdAt).toLocaleString() : ''}
                </div>

                <button
                    onClick={() => setExpanded(e => !e)}
                    style={{
                        marginTop: '6px', background: 'none', border: 'none', cursor: 'pointer',
                        color: 'var(--accent-primary)', fontSize: '0.78rem', display: 'flex',
                        alignItems: 'center', gap: '4px', padding: 0,
                    }}
                >
                    {expanded ? <FiChevronUp /> : <FiChevronDown />} {expanded ? 'Hide' : 'View'} raw response
                </button>

                {expanded && (
                    <div style={{ marginTop: '8px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                        {interaction.requestPayload && (
                            <div>
                                <div style={{ fontSize: '0.68rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '2px' }}>Request</div>
                                <pre style={{
                                    margin: 0, fontSize: '0.72rem', color: '#cbd5e1', background: 'rgba(0,0,0,0.35)',
                                    border: '1px solid rgba(255,255,255,0.08)', borderRadius: '6px', padding: '8px',
                                    whiteSpace: 'pre-wrap', wordBreak: 'break-all', maxHeight: '160px', overflowY: 'auto',
                                }}>{interaction.requestPayload}</pre>
                            </div>
                        )}
                        {interaction.responsePayload && (
                            <div>
                                <div style={{ fontSize: '0.68rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '2px' }}>Response</div>
                                <pre style={{
                                    margin: 0, fontSize: '0.72rem', color: '#cbd5e1', background: 'rgba(0,0,0,0.35)',
                                    border: '1px solid rgba(255,255,255,0.08)', borderRadius: '6px', padding: '8px',
                                    whiteSpace: 'pre-wrap', wordBreak: 'break-all', maxHeight: '220px', overflowY: 'auto',
                                }}>{interaction.responsePayload}</pre>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

export default function ScreeningTimelineModal({ logId, onClose }) {
    const [interactions, setInteractions] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [revealCount, setRevealCount] = useState(0);

    useEffect(() => {
        setLoading(true);
        setError(null);
        setRevealCount(0);
        screeningService.getInteractionTimeline(logId)
            .then(data => setInteractions(data || []))
            .catch(() => setError('Failed to load NRTS interaction timeline.'))
            .finally(() => setLoading(false));
    }, [logId]);

    // Stagger the reveal so rows animate in one after another, not all at once.
    useEffect(() => {
        if (!interactions || interactions.length === 0) return;
        let i = 0;
        const id = setInterval(() => {
            i += 1;
            setRevealCount(i);
            if (i >= interactions.length) clearInterval(id);
        }, 140);
        return () => clearInterval(id);
    }, [interactions]);

    return (
        <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(0,0,0,0.85)', zIndex: 2100,
            display: 'flex', justifyContent: 'center', alignItems: 'center',
        }}>
            <div style={{
                background: '#1f1f1f', padding: '25px', borderRadius: '12px',
                width: '600px', maxHeight: '85vh', overflowY: 'auto',
                border: '1px solid var(--accent-primary)',
                boxShadow: '0 0 30px rgba(0, 242, 254, 0.1)',
            }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '10px' }}>
                    <h3 style={{ margin: 0, color: 'var(--accent-primary)' }}>NRTS Interaction Timeline</h3>
                    <button onClick={onClose} aria-label="Close" style={{ background: 'none', border: 'none', color: 'white', fontSize: '1.3rem', cursor: 'pointer' }}>
                        <FiX />
                    </button>
                </div>

                {loading && (
                    <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>Loading timeline…</div>
                )}
                {error && (
                    <div style={{ textAlign: 'center', padding: '2rem', color: '#ef4444' }}>{error}</div>
                )}
                {!loading && !error && interactions && interactions.length === 0 && (
                    <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>
                        No NRTS interactions recorded for this screening (likely a No-Hit result, or it predates audit-trail tracking).
                    </div>
                )}
                {!loading && !error && interactions && interactions.length > 0 && (
                    <div>
                        {interactions.map((interaction, idx) => (
                            <TimelineRow key={interaction.interactionID ?? idx} interaction={interaction} index={idx} visible={idx < revealCount} />
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
