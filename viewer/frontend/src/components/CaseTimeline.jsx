import React, { useState } from 'react';

const CaseTimeline = ({ items }) => {
    const [isExpanded, setIsExpanded] = useState(false);

    if (!items || items.length === 0) {
        return <p style={{ fontStyle: 'italic', color: 'var(--text-secondary)' }}>No timeline data available for this case.</p>;
    }

    // Sort items: Active first, then by time? Or just chronological?
    // Usually chronological is best for timeline.
    // Let's just limit the view if there are too many items.
    const displayedItems = isExpanded ? items : items.slice(-5); // Show last 5 by default if sorted by time, or first 5?
    // Timeline usually goes top-down. If customized sort (newest first), showing "first 5" is correct.
    // The service sorts by Start Time ASC. So bottom is newest.
    // We probably want to see the *active* ones.

    // Let's just make the list compact first.

    const getStatusIcon = (status) => {
        switch (status) {
            case 'completed': return '✅';
            case 'terminated': return '❌';
            case 'active': return '▶️';
            case 'available': return '○';
            case 'enabled': return '⚪';
            default: return '•';
        }
    };

    const getStatusColor = (status) => {
        switch (status) {
            case 'completed': return '#52c41a';
            case 'active': return '#1890ff';
            case 'terminated': return '#ff4d4f';
            case 'available': return '#faad14';
            default: return 'var(--text-secondary)';
        }
    };

    return (
        <div className="case-timeline-compact">
            <div className="timeline-header-controls" style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '0.5rem' }}>
                {items.length > 5 && (
                    <button
                        onClick={() => setIsExpanded(!isExpanded)}
                        style={{ background: 'none', border: 'none', color: 'var(--primary-color)', cursor: 'pointer', fontSize: '0.8rem' }}
                    >
                        {isExpanded ? 'Show Less' : `Show All (${items.length})`}
                    </button>
                )}
            </div>

            <div className="timeline-list">
                {displayedItems.map((item, index) => (
                    <div key={index} className="timeline-row" style={{ opacity: item.status === 'completed' || item.status === 'terminated' ? 0.7 : 1 }}>
                        <div className="t-time" style={{ width: '130px', fontSize: '0.75rem', textAlign: 'right', color: 'var(--text-secondary)', paddingRight: '1rem' }}>
                            {new Date(item.startTime).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                        </div>
                        <div className="t-marker">
                            <div className="t-dot" style={{ borderColor: getStatusColor(item.status) }}></div>
                            {index !== displayedItems.length - 1 && <div className="t-line"></div>}
                        </div>
                        <div className="t-content">
                            <div className="t-title">
                                <span style={{ fontWeight: item.status === 'active' ? 'bold' : 'normal', color: item.status === 'active' ? 'white' : 'inherit' }}>
                                    {item.name}
                                </span>
                                <span className="t-status" style={{ color: getStatusColor(item.status), marginLeft: '0.5rem', fontSize: '0.7rem', textTransform: 'uppercase', border: `1px solid ${getStatusColor(item.status)}`, padding: '0px 4px', borderRadius: '4px' }}>
                                    {item.status}
                                </span>
                            </div>
                            {item.endTime && <div className="t-sub" style={{ fontSize: '0.7rem', color: 'var(--text-secondary)' }}>Ended: {new Date(item.endTime).toLocaleTimeString()}</div>}
                        </div>
                    </div>
                ))}
            </div>

            <style>{`
                .case-timeline-compact {
                    position: relative;
                }
                .timeline-list {
                    display: flex;
                    flex-direction: column;
                }
                .timeline-row {
                    display: flex;
                    align-items: stretch; /* stretch to fill height for line */
                    min-height: 2.5rem;
                }
                .t-marker {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    width: 20px;
                    position: relative;
                    margin-right: 1rem;
                }
                .t-dot {
                    width: 10px;
                    height: 10px;
                    border-radius: 50%;
                    border: 2px solid var(--text-secondary);
                    background: var(--glass-bg);
                    z-index: 1;
                    margin-top: 4px; /* Align with text roughly */
                }
                .t-line {
                    width: 1px;
                    background: rgba(255,255,255,0.1);
                    flex: 1;
                    margin-top: 2px;
                }
                .t-content {
                    flex: 1;
                    padding-bottom: 1rem;
                }
                .t-title {
                    font-size: 0.9rem;
                    display: flex;
                    align-items: center;
                }
            `}</style>
        </div>
    );
};

export default CaseTimeline;
