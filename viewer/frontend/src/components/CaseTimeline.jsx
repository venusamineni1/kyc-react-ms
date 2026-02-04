import React from 'react';

const CaseTimeline = ({ items }) => {
    if (!items || items.length === 0) {
        return <p style={{ fontStyle: 'italic', color: 'var(--text-secondary)' }}>No timeline data available for this case.</p>;
    }

    const getStatusIcon = (status) => {
        switch (status) {
            case 'completed':
            case 'terminated':
                return '✅';
            case 'active':
                return '🔄';
            case 'available':
            case 'enabled':
                return '⭕';
            default:
                return '•';
        }
    };

    const getStatusClass = (status) => {
        switch (status) {
            case 'completed': return 'status-completed';
            case 'active': return 'status-active';
            case 'available':
            case 'enabled': return 'status-available';
            default: return '';
        }
    };

    return (
        <div className="case-timeline">
            {items.map((item, index) => (
                <div key={index} className="timeline-item">
                    <div className="timeline-marker">
                        <span className={`marker-icon ${getStatusClass(item.status)}`}>
                            {getStatusIcon(item.status)}
                        </span>
                        {index !== items.length - 1 && <div className="timeline-connector"></div>}
                    </div>
                    <div className="timeline-content glass-section">
                        <div className="timeline-header">
                            <h4 style={{ margin: 0 }}>{item.name}</h4>
                            <span className="timeline-badge">{item.itemType}</span>
                        </div>
                        <div className="timeline-details">
                            <span className={`status-text ${getStatusClass(item.status)} uppercase`}>
                                {item.status}
                            </span>
                            <div className="timeline-times">
                                <div>Started: {new Date(item.startTime).toLocaleString()}</div>
                                {item.endTime && (
                                    <div>Ended: {new Date(item.endTime).toLocaleString()}</div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            ))}

            <style>{`
                .case-timeline {
                    padding: 1rem 0;
                    display: flex;
                    flex-direction: column;
                    gap: 1rem;
                }
                .timeline-item {
                    display: flex;
                    gap: 2rem;
                }
                .timeline-marker {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    width: 40px;
                }
                .marker-icon {
                    width: 32px;
                    height: 32px;
                    border-radius: 50%;
                    background: rgba(255,255,255,0.05);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 1.2rem;
                    border: 1px solid var(--glass-border);
                    z-index: 1;
                }
                .timeline-connector {
                    flex: 1;
                    width: 2px;
                    background: var(--glass-border);
                    margin: 4px 0;
                }
                .timeline-content {
                    flex: 1;
                    padding: 1rem !important;
                    margin-top: 0 !important;
                }
                .timeline-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 0.5rem;
                }
                .timeline-badge {
                    font-size: 0.7rem;
                    background: rgba(255,255,255,0.1);
                    padding: 2px 6px;
                    border-radius: 4px;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .timeline-details {
                    display: flex;
                    justify-content: space-between;
                    align-items: flex-end;
                    font-size: 0.85rem;
                }
                .timeline-times {
                    color: var(--text-secondary);
                    text-align: right;
                }
                .status-text {
                    font-weight: 600;
                    font-size: 0.75rem;
                }
                .status-completed { color: #52c41a; }
                .status-active { color: #1890ff; }
                .status-available { color: #faad14; }
                .uppercase { text-transform: uppercase; }
            `}</style>
        </div>
    );
};

export default CaseTimeline;
