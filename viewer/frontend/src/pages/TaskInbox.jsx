import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { caseService } from '../services/caseService';
import Button from '../components/Button';

const TaskInbox = () => {
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        loadTasks();
    }, []);

    const loadTasks = async () => {
        setLoading(true);
        try {
            const data = await caseService.getUserTasks();
            setTasks(data);
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleProcessTask = (caseId) => {
        navigate(`/cases/${caseId}`);
    };

    return (
        <div className="task-inbox-page">
            <div style={{ marginBottom: '2rem' }}>
                <div className="page-header" style={{ marginBottom: '1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: 'none', paddingBottom: '0' }}>
                    <div>
                        <h1 style={{ fontSize: '2rem', margin: '0 0 0.5rem 0', background: 'var(--header-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                            Decision Center
                        </h1>
                        <p style={{ color: 'var(--text-secondary)', margin: 0 }}>Manage your assigned workflow cases and ad-hoc requests</p>
                    </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.5rem' }}>
                    {[
                        { label: 'Total Tasks', value: tasks.length, color: '#3b82f6', icon: '📋' },
                        { label: 'Workflow Cases', value: tasks.filter(t => t.caseId).length, color: '#10b981', icon: '⚡' },
                        { label: 'Ad-Hoc Requests', value: tasks.filter(t => !t.caseId).length, color: '#f59e0b', icon: '✉️' }
                    ].map((s, idx) => (
                        <div key={idx} className="glass-section" style={{ padding: '1.5rem', marginBottom: 0, display: 'flex', alignItems: 'center', gap: '1.2rem', borderLeft: `4px solid ${s.color}` }}>
                            <div style={{ fontSize: '2rem', background: 'rgba(255,255,255,0.05)', width: '50px', height: '50px', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                {s.icon}
                            </div>
                            <div>
                                <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{s.label}</div>
                                <div style={{ fontSize: '1.75rem', fontWeight: '700', color: 'var(--text-color)' }}>{s.value}</div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            <div className="glass-section" style={{ padding: '0' }}>
                <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h3 style={{ margin: 0 }}>Pending Actions</h3>
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Showing internal and workflow tasks</div>
                </div>

                {loading ? (
                    <div style={{ padding: '4rem', textAlign: 'center' }}>
                        <div className="loading-spinner">Initializing task engine...</div>
                    </div>
                ) : error ? (
                    <div style={{ padding: '2rem', textAlign: 'center', color: '#ef4444' }}>
                        <p>{error}</p>
                    </div>
                ) : tasks.length === 0 ? (
                    <div style={{ padding: '4rem', textAlign: 'center', color: '#64748b' }}>
                        <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🎉</div>
                        <h3>All Clear!</h3>
                        <p>No tasks assigned to you or your groups at this time.</p>
                    </div>
                ) : (
                    <div style={{ overflowX: 'auto' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                                <tr style={{ background: 'var(--hover-bg)' }}>
                                    <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>Task Name</th>
                                    <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>Type</th>
                                    <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>Context</th>
                                    <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>Created</th>
                                    <th style={{ padding: '1.25rem 1.5rem', textAlign: 'right', color: 'var(--text-secondary)', fontWeight: '500' }}>Action</th>
                                </tr>
                            </thead>
                            <tbody>
                                {tasks.map(task => (
                                    <tr key={task.taskId} className="table-row-hover" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            <div style={{ fontWeight: '600', color: 'var(--text-color)' }}>{task.name}</div>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>ID: {task.taskId}</div>
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            <span style={{
                                                padding: '0.25rem 0.6rem',
                                                borderRadius: '4px',
                                                fontSize: '0.7rem',
                                                fontWeight: '700',
                                                background: task.workflowType === 'CMMN' ? 'rgba(59, 130, 246, 0.1)' : 'rgba(168, 85, 247, 0.1)',
                                                color: task.workflowType === 'CMMN' ? '#60a5fa' : '#c084fc',
                                                border: `1px solid ${task.workflowType === 'CMMN' ? 'rgba(59, 130, 246, 0.2)' : 'rgba(168, 85, 247, 0.2)'}`
                                            }}>
                                                {task.workflowType || 'BPMN'}
                                            </span>
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            {task.caseId ? (
                                                <div style={{ display: 'flex', flexDirection: 'column' }}>
                                                    <Link to={`/cases/${task.caseId}`} style={{ color: 'var(--primary-color)', textDecoration: 'none', fontWeight: '500' }}>Case #{task.caseId}</Link>
                                                    <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Client: {task.clientID || 'N/A'}</span>
                                                </div>
                                            ) : (
                                                <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', padding: '0.2rem 0.5rem', background: 'var(--hover-bg)', borderRadius: '4px' }}>Ad-Hoc Request</span>
                                            )}
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                                            {new Date(task.createTime).toLocaleString()}
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem', textAlign: 'right' }}>
                                            {task.caseId ? (
                                                <Button variant="primary" onClick={() => handleProcessTask(task.caseId)} style={{ fontSize: '0.85rem', padding: '0.5rem 1rem' }}>
                                                    Process Case
                                                </Button>
                                            ) : (
                                                <Link to="/adhoc-tasks">
                                                    <Button variant="secondary" style={{ fontSize: '0.85rem', padding: '0.5rem 1rem' }}>
                                                        View Task
                                                    </Button>
                                                </Link>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
};

export default TaskInbox;
