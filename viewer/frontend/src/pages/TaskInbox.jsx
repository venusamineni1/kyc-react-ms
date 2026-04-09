import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { caseService } from '../services/caseService';
import Button from '../components/Button';

/* Truncate long text for display */
const truncate = (str, max = 60) =>
    str && str.length > max ? str.slice(0, max).trimEnd() + '…' : str;

/* Case status → colour mapping */
const STATUS_COLORS = {
    IN_REVIEW:       { bg: 'rgba(59,130,246,0.15)',  color: '#60a5fa' },
    PENDING_REVIEW:  { bg: 'rgba(245,158,11,0.15)',  color: '#fbbf24' },
    APPROVED:        { bg: 'rgba(16,185,129,0.15)',  color: '#34d399' },
    REJECTED:        { bg: 'rgba(239,68,68,0.15)',   color: '#f87171' },
    OPEN:            { bg: 'rgba(245,158,11,0.15)',  color: '#fbbf24' },
    IN_PROGRESS:     { bg: 'rgba(59,130,246,0.15)',  color: '#60a5fa' },
    CLOSED:          { bg: 'rgba(100,116,139,0.15)', color: '#94a3b8' },
};
const statusStyle = (s) =>
    STATUS_COLORS[s?.toUpperCase()] ?? { bg: 'rgba(255,255,255,0.07)', color: 'var(--text-secondary)' };

const TaskInbox = () => {
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    // Reassign modal state
    const [reassignModal, setReassignModal] = useState({ open: false, taskId: null });
    const [reassignUser, setReassignUser] = useState('');
    const [allUsers, setAllUsers] = useState([]);
    const [reassigning, setReassigning] = useState(false);

    useEffect(() => { loadTasks(); }, []);

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

    const workflowTasks = tasks.filter(t => t.caseId);
    const adHocTasks    = tasks.filter(t => !t.caseId);

    const openReassign = async (taskId) => {
        try {
            const users = await caseService.getAllUsers();
            setAllUsers(users);
        } catch {
            setAllUsers([]);
        }
        setReassignUser('');
        setReassignModal({ open: true, taskId });
    };

    const handleReassign = async () => {
        if (!reassignUser) return;
        setReassigning(true);
        try {
            await caseService.reassignTask(reassignModal.taskId, reassignUser);
            setReassignModal({ open: false, taskId: null });
            setReassignUser('');
            loadTasks();
        } catch (err) {
            console.error('Reassign failed:', err);
        } finally {
            setReassigning(false);
        }
    };

    return (
        <div className="task-inbox-page">

            {/* ── Header ─────────────────────────────────────────────── */}
            <div style={{ marginBottom: '2rem' }}>
                <div className="page-header" style={{ marginBottom: '1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: 'none', paddingBottom: 0 }}>
                    <div>
                        <h1 style={{ fontSize: '2rem', margin: '0 0 0.5rem 0', background: 'var(--header-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                            Decision Center
                        </h1>
                        <p style={{ color: 'var(--text-secondary)', margin: 0 }}>
                            Manage your assigned workflow cases and ad-hoc requests
                        </p>
                    </div>
                </div>

                {/* Summary cards */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.5rem' }}>
                    {[
                        { label: 'Total Tasks',      value: tasks.length,          color: '#3b82f6', icon: '📋' },
                        { label: 'Workflow Cases',   value: workflowTasks.length,  color: '#10b981', icon: '⚡' },
                        { label: 'Ad-Hoc Requests',  value: adHocTasks.length,     color: '#f59e0b', icon: '✉️' },
                    ].map((s, i) => (
                        <div key={i} className="glass-section" style={{ padding: '1.25rem 1.5rem', marginBottom: 0, display: 'flex', alignItems: 'center', gap: '1rem', borderLeft: `4px solid ${s.color}` }}>
                            <div style={{ fontSize: '1.75rem', background: 'rgba(255,255,255,0.05)', width: '46px', height: '46px', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                                {s.icon}
                            </div>
                            <div>
                                <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{s.label}</div>
                                <div style={{ fontSize: '1.6rem', fontWeight: 700, color: 'var(--text-color)' }}>{s.value}</div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* ── Task table ─────────────────────────────────────────── */}
            <div className="glass-section" style={{ padding: 0 }}>
                <div style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h3 style={{ margin: 0 }}>Pending Actions</h3>
                    <button onClick={loadTasks} title="Refresh" style={{ background: 'none', border: '1px solid var(--glass-border)', borderRadius: '6px', color: 'var(--text-secondary)', padding: '4px 10px', cursor: 'pointer', fontSize: '0.8rem' }}>
                        ↻ Refresh
                    </button>
                </div>

                {loading ? (
                    <div style={{ padding: '4rem', textAlign: 'center' }}>
                        <div className="loading-spinner">Loading tasks…</div>
                    </div>
                ) : error ? (
                    <div style={{ padding: '2rem', textAlign: 'center', color: '#ef4444' }}>{error}</div>
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
                                    {['Task', 'Client', 'Case / Context', 'Type', 'Status', 'Created', 'Action'].map((h, i) => (
                                        <th key={i} style={{ padding: '0.9rem 1.25rem', textAlign: i === 6 ? 'right' : 'left', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.05em', whiteSpace: 'nowrap' }}>
                                            {h}
                                        </th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {tasks.map(task => {
                                    const isWorkflow = !!task.caseId;
                                    const ss = statusStyle(task.caseStatus || task.status);

                                    return (
                                        <tr key={task.taskId} className="table-row-hover" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>

                                            {/* ── Task name + description ── */}
                                            <td style={{ padding: '1rem 1.25rem', maxWidth: '220px' }}>
                                                <div style={{ fontWeight: 600, color: 'var(--text-color)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                                    {task.name}
                                                </div>
                                                {(task.description || task.requestText) && (
                                                    <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.2rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
                                                        title={task.description || task.requestText}>
                                                        {truncate(task.description || task.requestText, 55)}
                                                    </div>
                                                )}
                                                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', marginTop: '0.15rem', fontFamily: 'monospace' }}>
                                                    #{String(task.taskId ?? '').slice(0, 10)}
                                                </div>
                                            </td>

                                            {/* ── Client name ── */}
                                            <td style={{ padding: '1rem 1.25rem', whiteSpace: 'nowrap' }}>
                                                {task.clientName || task.clientID ? (
                                                    <div>
                                                        <div style={{ fontWeight: 500, color: 'var(--text-color)', fontSize: '0.875rem' }}>
                                                            {task.clientName || task.clientID}
                                                        </div>
                                                        {task.clientName && task.clientID && (
                                                            <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)' }}>
                                                                {task.clientID}
                                                            </div>
                                                        )}
                                                    </div>
                                                ) : (
                                                    <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>—</span>
                                                )}
                                            </td>

                                            {/* ── Case link / ad-hoc context ── */}
                                            <td style={{ padding: '1rem 1.25rem', maxWidth: '180px' }}>
                                                {isWorkflow ? (
                                                    <Link
                                                        to={`/cases/${task.caseId}`}
                                                        style={{ color: 'var(--primary-color)', textDecoration: 'none', fontWeight: 500, fontSize: '0.875rem', display: 'block', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
                                                        title={`Case ${task.caseId}`}
                                                    >
                                                        Case #{String(task.caseId ?? '').slice(0, 12)}
                                                    </Link>
                                                ) : (
                                                    <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', background: 'var(--hover-bg)', padding: '2px 7px', borderRadius: '4px', whiteSpace: 'nowrap' }}>
                                                        Ad-Hoc Request
                                                    </span>
                                                )}
                                            </td>

                                            {/* ── Workflow type badge ── */}
                                            <td style={{ padding: '1rem 1.25rem', whiteSpace: 'nowrap' }}>
                                                {isWorkflow ? (
                                                    <span style={{
                                                        padding: '2px 8px', borderRadius: '4px', fontSize: '0.68rem', fontWeight: 700,
                                                        background: task.workflowType === 'CMMN' ? 'rgba(59,130,246,0.12)' : 'rgba(168,85,247,0.12)',
                                                        color:      task.workflowType === 'CMMN' ? '#60a5fa'               : '#c084fc',
                                                        border: `1px solid ${task.workflowType === 'CMMN' ? 'rgba(59,130,246,0.25)' : 'rgba(168,85,247,0.25)'}`,
                                                    }}>
                                                        {task.workflowType || 'BPMN'}
                                                    </span>
                                                ) : (
                                                    <span style={{ padding: '2px 8px', borderRadius: '4px', fontSize: '0.68rem', fontWeight: 700, background: 'rgba(245,158,11,0.12)', color: '#fbbf24', border: '1px solid rgba(245,158,11,0.25)' }}>
                                                        AD-HOC
                                                    </span>
                                                )}
                                            </td>

                                            {/* ── Case / task status ── */}
                                            <td style={{ padding: '1rem 1.25rem', whiteSpace: 'nowrap' }}>
                                                {(task.caseStatus || task.status) ? (
                                                    <span style={{ padding: '2px 9px', borderRadius: '10px', fontSize: '0.68rem', fontWeight: 700, background: ss.bg, color: ss.color, textTransform: 'uppercase' }}>
                                                        {(task.caseStatus || task.status).replace(/_/g, ' ')}
                                                    </span>
                                                ) : (
                                                    <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>—</span>
                                                )}
                                            </td>

                                            {/* ── Created date ── */}
                                            <td style={{ padding: '1rem 1.25rem', color: 'var(--text-secondary)', fontSize: '0.82rem', whiteSpace: 'nowrap' }}>
                                                <div>{new Date(task.createTime).toLocaleDateString()}</div>
                                                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                                                    {new Date(task.createTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                                </div>
                                            </td>

                                            {/* ── Action ── */}
                                            <td style={{ padding: '1rem 1.25rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                                                <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                                                    {isWorkflow ? (
                                                        <>
                                                            <Button
                                                                variant="primary"
                                                                onClick={() => navigate(`/cases/${task.caseId}`)}
                                                                style={{ fontSize: '0.82rem', padding: '0.4rem 0.9rem' }}
                                                            >
                                                                Open Case
                                                            </Button>
                                                            <button
                                                                onClick={() => openReassign(task.taskId)}
                                                                style={{ fontSize: '0.8rem', padding: '0.4rem 0.75rem', background: 'rgba(255,255,255,0.07)', border: '1px solid var(--glass-border)', borderRadius: '6px', color: 'var(--text-secondary)', cursor: 'pointer' }}
                                                            >
                                                                Reassign
                                                            </button>
                                                        </>
                                                    ) : (
                                                        <Button
                                                            variant="secondary"
                                                            onClick={() => navigate(`/adhoc-tasks?taskId=${task.taskId}`)}
                                                            style={{ fontSize: '0.82rem', padding: '0.4rem 0.9rem' }}
                                                        >
                                                            View Task
                                                        </Button>
                                                    )}
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

        {/* Reassign modal */}
        {reassignModal.open && (
            <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
                <div className="glass-section" style={{ width: '360px', padding: '2rem' }}>
                    <h3 style={{ marginTop: 0 }}>Reassign Task</h3>
                    <select
                        value={reassignUser}
                        onChange={e => setReassignUser(e.target.value)}
                        style={{ width: '100%', padding: '0.6rem', marginBottom: '1.5rem', background: 'rgba(255,255,255,0.07)', border: '1px solid var(--glass-border)', borderRadius: '8px', color: '#fff' }}
                    >
                        <option value="">Select user...</option>
                        {allUsers.map(u => <option key={u.username} value={u.username}>{u.username}</option>)}
                    </select>
                    <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                        <button onClick={() => setReassignModal({ open: false, taskId: null })}
                            style={{ padding: '0.5rem 1rem', background: 'none', border: '1px solid var(--glass-border)', borderRadius: '8px', color: '#fff', cursor: 'pointer' }}>
                            Cancel
                        </button>
                        <button onClick={handleReassign} disabled={!reassignUser || reassigning}
                            style={{ padding: '0.5rem 1rem', background: '#4facfe', border: 'none', borderRadius: '8px', color: '#000', fontWeight: 600, cursor: 'pointer' }}>
                            {reassigning ? 'Reassigning...' : 'Reassign'}
                        </button>
                    </div>
                </div>
            </div>
        )}
    </div>
    );
};

export default TaskInbox;
