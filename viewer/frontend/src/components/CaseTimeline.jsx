import React, { useState } from 'react';

const CaseTimeline = ({ items }) => {
    const [isExpanded, setIsExpanded] = useState(false);

    if (!items || items.length === 0) {
        return <p style={{ fontStyle: 'italic', color: 'var(--text-secondary)', padding: '1rem' }}>No lifecycle data available for this case.</p>;
    }

    // Sort items: oldest first
    const sortedItems = [...items].sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
    const displayedItems = isExpanded ? sortedItems : sortedItems.slice(-6);

    const getStatusConfig = (status) => {
        switch (status?.toLowerCase()) {
            case 'completed': return { color: '#52c41a', bg: 'rgba(82, 196, 26, 0.1)', icon: '✓' };
            case 'active': return { color: '#1890ff', bg: 'rgba(24, 144, 255, 0.2)', icon: '▶', pulse: true };
            case 'available': return { color: '#faad14', bg: 'rgba(250, 173, 20, 0.1)', icon: '○' };
            case 'terminated': return { color: '#ff4d4f', bg: 'rgba(255, 77, 79, 0.1)', icon: '✕' };
            case 'enabled': return { color: '#722ed1', bg: 'rgba(114, 46, 209, 0.1)', icon: '•' };
            default: return { color: 'var(--text-secondary)', bg: 'rgba(255,255,255,0.05)', icon: '•' };
        }
    };

    const getTypeLabel = (type) => {
        if (type?.includes('Task')) return 'Task';
        if (type?.includes('Stage')) return 'Stage';
        if (type?.includes('EventListener')) return 'Signal';
        return type || 'Event';
    };

    return (
        <div className="modern-timeline">
            <div className="timeline-controls">
                {items.length > 6 && (
                    <button onClick={() => setIsExpanded(!isExpanded)} className="expand-link">
                        {isExpanded ? 'Collapse View' : `View Full History (${items.length} events)`}
                    </button>
                )}
            </div>

            <div className="timeline-rail">
                {displayedItems.map((item, index) => {
                    const config = getStatusConfig(item.status);
                    const isLast = index === displayedItems.length - 1;
                    const isActive = item.status === 'active';

                    return (
                        <div key={index} className={`timeline-entry ${isActive ? 'entry-active' : ''}`}>
                            <div className="entry-time">
                                <span className="date">{new Date(item.startTime).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</span>
                                <span className="time">{new Date(item.startTime).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}</span>
                            </div>

                            <div className="entry-rail-segment">
                                <div className={`rail-dot ${config.pulse ? 'pulse' : ''}`} style={{ backgroundColor: config.color, boxShadow: `0 0 10px ${config.color}` }}>
                                    <span className="dot-icon">{config.icon}</span>
                                </div>
                                {!isLast && <div className="rail-line"></div>}
                            </div>

                            <div className="entry-card shadow-lg">
                                <div className="card-header">
                                    <span className="type-chip">{getTypeLabel(item.itemType)}</span>
                                    <span className="status-badge" style={{ color: config.color, background: config.bg, borderColor: `${config.color}40` }}>
                                        {item.status}
                                    </span>
                                </div>
                                <div className="card-body">
                                    <h4 className="entry-name">{item.name}</h4>
                                    {item.endTime && (
                                        <div className="entry-meta">
                                            Completed at {new Date(item.endTime).toLocaleTimeString()}
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    );
                })}
            </div>

            <style>{`
                .modern-timeline {
                    padding: 1rem 0;
                    font-family: 'Inter', sans-serif;
                }
                .timeline-controls {
                    display: flex;
                    justify-content: flex-end;
                    margin-bottom: 2rem;
                }
                .expand-link {
                    background: none;
                    border: none;
                    color: var(--primary-color);
                    cursor: pointer;
                    font-weight: 600;
                    font-size: 0.85rem;
                    opacity: 0.8;
                    transition: opacity 0.2s;
                }
                .expand-link:hover { opacity: 1; }

                .timeline-rail {
                    display: flex;
                    flex-direction: column;
                    gap: 0.5rem;
                }

                .timeline-entry {
                    display: flex;
                    align-items: flex-start;
                    position: relative;
                }

                .entry-time {
                    width: 90px;
                    display: flex;
                    flex-direction: column;
                    align-items: flex-end;
                    padding-right: 1.5rem;
                    margin-top: 0.75rem;
                    flex-shrink: 0;
                }
                .entry-time .date { font-size: 0.75rem; font-weight: 700; color: #fff; }
                .entry-time .time { font-size: 0.7rem; color: var(--text-secondary); }

                .entry-rail-segment {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    width: 32px;
                    flex-shrink: 0;
                    position: relative;
                }
                .rail-dot {
                    width: 24px;
                    height: 24px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    z-index: 2;
                    margin-top: 0.75rem;
                }
                .dot-icon { color: white; font-size: 0.75rem; font-weight: bold; }
                .rail-line {
                    width: 2px;
                    background: linear-gradient(to bottom, var(--glass-border), transparent);
                    flex: 1;
                    height: 100%;
                    min-height: 40px;
                    position: absolute;
                    top: 2rem;
                    z-index: 1;
                }

                .entry-card {
                    flex: 1;
                    background: rgba(255, 255, 255, 0.03);
                    border: 1px solid var(--glass-border);
                    border-radius: 12px;
                    padding: 1rem;
                    margin-left: 1rem;
                    margin-bottom: 1.5rem;
                    transition: transform 0.2s, background 0.2s;
                }
                .entry-active .entry-card {
                    background: rgba(255, 255, 255, 0.07);
                    border-color: var(--primary-color);
                    box-shadow: 0 0 20px rgba(24, 144, 255, 0.1);
                    transform: scale(1.01);
                }

                .card-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 0.5rem;
                }
                .type-chip {
                    font-size: 0.65rem;
                    font-weight: 800;
                    text-transform: uppercase;
                    letter-spacing: 0.05em;
                    color: var(--primary-color);
                    background: rgba(24, 144, 255, 0.1);
                    padding: 2px 6px;
                    border-radius: 4px;
                }
                .status-badge {
                    font-size: 0.65rem;
                    font-weight: 700;
                    text-transform: uppercase;
                    padding: 2px 8px;
                    border-radius: 20px;
                    border: 1px solid transparent;
                }

                .entry-name {
                    margin: 0;
                    font-size: 0.95rem;
                    color: #fff;
                    font-weight: 600;
                }
                .entry-meta {
                    font-size: 0.75rem;
                    color: var(--text-secondary);
                    margin-top: 0.25rem;
                }

                .pulse {
                    animation: pulse-ring 2s infinite;
                }
                @keyframes pulse-ring {
                    0% { box-shadow: 0 0 0 0 rgba(24, 144, 255, 0.4); }
                    70% { box-shadow: 0 0 0 10px rgba(24, 144, 255, 0); }
                    100% { box-shadow: 0 0 0 0 rgba(24, 144, 255, 0); }
                }
            `}</style>
        </div>
    );
};

export default CaseTimeline;
