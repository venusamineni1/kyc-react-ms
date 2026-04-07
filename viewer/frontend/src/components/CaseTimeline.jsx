import React, { useState } from 'react';

const STATUS_CONFIG = {
    completed:  { color: '#52c41a', label: 'Completed' },
    active:     { color: '#1890ff', label: 'Active',    pulse: true },
    available:  { color: '#faad14', label: 'Available' },
    terminated: { color: '#ff4d4f', label: 'Terminated' },
    enabled:    { color: '#722ed1', label: 'Enabled' },
};

const getTypeLabel = (type) => {
    if (type?.includes('Task'))          return 'Task';
    if (type?.includes('Stage'))         return 'Stage';
    if (type?.includes('EventListener')) return 'Signal';
    return 'Event';
};

const formatTime = (iso) => {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleString(undefined, {
        day: '2-digit', month: 'short',
        hour: '2-digit', minute: '2-digit', hour12: false,
    });
};

const getDuration = (start, end) => {
    if (!start || !end) return null;
    const mins = Math.round((new Date(end) - new Date(start)) / 60000);
    if (mins < 1)   return '<1m';
    if (mins < 60)  return `${mins}m`;
    const h = Math.floor(mins / 60), m = mins % 60;
    return m ? `${h}h ${m}m` : `${h}h`;
};

const COLLAPSED_COUNT = 8;

const CaseTimeline = ({ items }) => {
    const [expanded, setExpanded] = useState(false);

    if (!items || items.length === 0) {
        return (
            <p style={{ fontStyle: 'italic', color: 'var(--text-secondary)', padding: '0.5rem' }}>
                No lifecycle data available for this case.
            </p>
        );
    }

    const sorted   = [...items].sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
    const hidden   = sorted.length - COLLAPSED_COUNT;
    const displayed = expanded ? sorted : sorted.slice(-COLLAPSED_COUNT);

    return (
        <div style={{ fontFamily: 'Inter, sans-serif', fontSize: '0.83rem' }}>

            {/* ── Column headers ───────────────────────────────────── */}
            <div style={{
                display: 'grid',
                gridTemplateColumns: '12px 1fr 56px 84px 130px',
                columnGap: '0.75rem',
                padding: '0 0.5rem 0.4rem',
                color: 'var(--text-secondary)',
                fontSize: '0.7rem',
                fontWeight: 700,
                textTransform: 'uppercase',
                letterSpacing: '0.06em',
                borderBottom: '1px solid var(--glass-border)',
                marginBottom: '0.25rem',
            }}>
                <span />
                <span>Event</span>
                <span>Type</span>
                <span>Status</span>
                <span>Started</span>
            </div>

            {/* ── Rows ─────────────────────────────────────────────── */}
            {displayed.map((item, idx) => {
                const cfg      = STATUS_CONFIG[item.status?.toLowerCase()] ?? STATUS_CONFIG.enabled;
                const typeLabel = getTypeLabel(item.itemType);
                const isStage  = typeLabel === 'Stage';
                const isActive = item.status?.toLowerCase() === 'active';
                const dur      = getDuration(item.startTime, item.endTime);

                return (
                    <div
                        key={idx}
                        style={{
                            display: 'grid',
                            gridTemplateColumns: '12px 1fr 56px 84px 130px',
                            columnGap: '0.75rem',
                            alignItems: 'center',
                            padding: '0.42rem 0.5rem',
                            borderRadius: '6px',
                            marginBottom: '2px',
                            borderLeft: `3px solid ${isActive ? cfg.color : 'transparent'}`,
                            background: isActive
                                ? 'rgba(24,144,255,0.06)'
                                : isStage
                                    ? 'rgba(255,255,255,0.025)'
                                    : 'transparent',
                            fontWeight: isStage ? 600 : 400,
                            transition: 'background 0.15s',
                        }}
                    >
                        {/* Status dot */}
                        <span style={{
                            width: 9, height: 9,
                            borderRadius: '50%',
                            background: cfg.color,
                            display: 'inline-block',
                            flexShrink: 0,
                            boxShadow: isActive ? `0 0 5px ${cfg.color}` : 'none',
                            animation: isActive ? 'tl-pulse 2s infinite' : 'none',
                        }} />

                        {/* Name + duration */}
                        <span style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', minWidth: 0 }}>
                            <span style={{
                                color: 'var(--text-color)',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                            }}>
                                {item.name}
                            </span>
                            {dur && (
                                <span style={{
                                    color: 'var(--text-secondary)',
                                    fontSize: '0.7rem',
                                    flexShrink: 0,
                                    opacity: 0.75,
                                }}>
                                    {dur}
                                </span>
                            )}
                        </span>

                        {/* Type chip */}
                        <span style={{
                            fontSize: '0.65rem',
                            fontWeight: 700,
                            padding: '2px 6px',
                            borderRadius: '4px',
                            textAlign: 'center',
                            whiteSpace: 'nowrap',
                            background: isStage
                                ? 'rgba(114,46,209,0.15)'
                                : 'rgba(255,255,255,0.07)',
                            color: isStage ? '#b37feb' : 'var(--text-secondary)',
                        }}>
                            {typeLabel}
                        </span>

                        {/* Status badge */}
                        <span style={{
                            fontSize: '0.65rem',
                            fontWeight: 700,
                            padding: '2px 7px',
                            borderRadius: '10px',
                            textAlign: 'center',
                            whiteSpace: 'nowrap',
                            background: `${cfg.color}22`,
                            color: cfg.color,
                        }}>
                            {cfg.label}
                        </span>

                        {/* Timestamp (hover shows end time) */}
                        <span
                            title={item.endTime ? `Ended: ${formatTime(item.endTime)}` : ''}
                            style={{
                                color: 'var(--text-secondary)',
                                fontSize: '0.75rem',
                                whiteSpace: 'nowrap',
                                cursor: item.endTime ? 'help' : 'default',
                            }}
                        >
                            {formatTime(item.startTime)}
                        </span>
                    </div>
                );
            })}

            {/* ── Expand / collapse ────────────────────────────────── */}
            {sorted.length > COLLAPSED_COUNT && (
                <button
                    onClick={() => setExpanded(e => !e)}
                    style={{
                        marginTop: '0.6rem',
                        width: '100%',
                        background: 'none',
                        border: '1px solid var(--glass-border)',
                        borderRadius: '6px',
                        color: 'var(--primary-color)',
                        padding: '0.3rem 1rem',
                        cursor: 'pointer',
                        fontSize: '0.78rem',
                        fontFamily: 'inherit',
                        opacity: 0.85,
                        transition: 'opacity 0.15s',
                    }}
                    onMouseEnter={e => e.target.style.opacity = 1}
                    onMouseLeave={e => e.target.style.opacity = 0.85}
                >
                    {expanded
                        ? '▲  Show fewer events'
                        : `▼  Show ${hidden} earlier event${hidden !== 1 ? 's' : ''}`}
                </button>
            )}

            <style>{`
                @keyframes tl-pulse {
                    0%   { box-shadow: 0 0 0 0   rgba(24,144,255,0.5); }
                    70%  { box-shadow: 0 0 0 6px rgba(24,144,255,0);   }
                    100% { box-shadow: 0 0 0 0   rgba(24,144,255,0);   }
                }
            `}</style>
        </div>
    );
};

export default CaseTimeline;
