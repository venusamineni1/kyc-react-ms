import React, { useState, useEffect } from 'react';
import { adHocTaskService } from '../services/adHocTaskService';
import { useAuth } from '../contexts/AuthContext';
import Button from '../components/Button';
import Modal from '../components/Modal';
import { useNotification } from '../contexts/NotificationContext';
import { caseService } from '../services/caseService';
import { clientService } from '../services/clientService';

const AdHocTaskList = () => {
    const { user } = useAuth();
    const { notify } = useNotification();
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeTab, setActiveTab] = useState('inbox'); // 'inbox' or 'sent'

    // Create Modal
    const [isCreateOpen, setIsCreateOpen] = useState(false);
    const [newTask, setNewTask] = useState({ assignee: '', requestText: '', clientID: '' });
    const [users, setUsers] = useState([]);
    const [clients, setClients] = useState([]);
    const [creating, setCreating] = useState(false);

    // Detail/Respond Modal
    const [selectedTask, setSelectedTask] = useState(null);
    const [responseText, setResponseText] = useState('');
    const [responding, setResponding] = useState(false);

    const loadTasks = async () => {
        setLoading(true);
        try {
            const data = await adHocTaskService.getMyTasks();
            setTasks(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const loadData = async () => {
        try {
            const [usersData, clientsData] = await Promise.all([
                caseService.getAllUsers(),
                clientService.getClients(0, '')
            ]);
            setUsers(usersData);
            setClients(clientsData.content || (Array.isArray(clientsData) ? clientsData : []));
        } catch (err) {
            console.warn("Failed to load users or clients", err);
        }
    };

    useEffect(() => {
        loadTasks();
        loadData();
    }, []);

    const handleCreate = async () => {
        if (!newTask.assignee || !newTask.requestText) return notify("Assignee and Request Text are required", 'warning');
        setCreating(true);
        try {
            await adHocTaskService.createTask(newTask);
            setIsCreateOpen(false);
            setNewTask({ assignee: '', requestText: '', clientID: '' });
            loadTasks();
            notify('Task created and assigned successfully', 'success');
        } catch (err) {
            notify(err.message, 'error');
        } finally {
            setCreating(false);
        }
    };

    const handleRespond = async () => {
        if (!responseText) return notify("Response is required", 'warning');
        setResponding(true);
        try {
            await adHocTaskService.respondTask(selectedTask.id, responseText);
            setSelectedTask(null);
            setResponseText('');
            loadTasks();
            notify('Response sent successfully', 'success');
        } catch (err) {
            notify(err.message, 'error');
        } finally {
            setResponding(false);
        }
    };

    const handleComplete = async () => {
        setResponding(true);
        try {
            await adHocTaskService.completeTask(selectedTask.id);
            setSelectedTask(null);
            loadTasks();
            notify('Task marked as completed', 'success');
        } catch (err) {
            notify(err.message, 'error');
        } finally {
            setResponding(false);
        }
    };

    const filteredTasks = tasks.filter(t => {
        if (activeTab === 'inbox') {
            return t.assignee === user.username;
        } else {
            return t.owner === user.username && t.assignee !== user.username;
        }
    });

    return (
        <div className="adhoc-tasks-page">
            <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2.5rem' }}>
                <div>
                    <h1 style={{ fontSize: '2.25rem', margin: '0 0 0.5rem 0', background: 'var(--header-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                        Ad-Hoc Tasks
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', margin: 0 }}>Create and manage direct compliance requests and internal tasks</p>
                </div>
                <Button onClick={() => setIsCreateOpen(true)} style={{ background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)', border: 'none', color: 'white' }}>
                    <span style={{ marginRight: '0.6rem' }}>+</span> New Direct Task
                </Button>
            </header>

            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '2rem', background: 'rgba(255,255,255,0.03)', padding: '0.4rem', borderRadius: '12px', width: 'fit-content', border: '1px solid rgba(255,255,255,0.05)' }}>
                {[
                    { id: 'inbox', label: 'Inbox', count: tasks.filter(t => t.assignee === user.username).length },
                    { id: 'sent', label: 'Sent / Pending', count: tasks.filter(t => t.owner === user.username && t.assignee !== user.username).length }
                ].map(tab => (
                    <button
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        style={{
                            padding: '0.6rem 1.2rem',
                            borderRadius: '8px',
                            border: 'none',
                            background: activeTab === tab.id ? 'rgba(59, 130, 246, 0.15)' : 'transparent',
                            color: activeTab === tab.id ? 'var(--primary-color)' : 'var(--text-secondary)',
                            fontSize: '0.9rem',
                            fontWeight: '600',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '0.5rem',
                            transition: 'all 0.2s ease'
                        }}
                    >
                        {tab.label}
                        <span style={{ fontSize: '0.75rem', opacity: 0.6, background: activeTab === tab.id ? 'rgba(59, 130, 246, 0.2)' : 'rgba(255,255,255,0.05)', padding: '0.1rem 0.5rem', borderRadius: '10px' }}>
                            {tab.count}
                        </span>
                    </button>
                ))}
            </div>

            <div className="glass-section" style={{ padding: '0', minHeight: '400px' }}>
                {loading ? (
                    <div style={{ padding: '6rem', textAlign: 'center' }}>
                        <div className="loading-spinner">Retrieving tasks...</div>
                    </div>
                ) : filteredTasks.length > 0 ? (
                    <div style={{ overflowX: 'auto' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead>
                                <tr style={{ background: 'var(--hover-bg)' }}>
                                    <th style={{ padding: '1.25rem 1.5rem', color: 'var(--text-secondary)', fontWeight: '500' }}>Reference</th>
                                    <th style={{ padding: '1.25rem 1.5rem', color: 'var(--text-secondary)', fontWeight: '500' }}>Subject</th>
                                    <th style={{ padding: '1.25rem 1.5rem', color: 'var(--text-secondary)', fontWeight: '500' }}>{activeTab === 'inbox' ? 'Requestor' : 'Assignee'}</th>
                                    <th style={{ padding: '1.25rem 1.5rem', color: 'var(--text-secondary)', fontWeight: '500' }}>Status</th>
                                    <th style={{ padding: '1.25rem 1.5rem', color: 'var(--text-secondary)', fontWeight: '500' }}>Timeline</th>
                                    <th style={{ padding: '1.25rem 1.5rem', textAlign: 'right', color: 'var(--text-secondary)', fontWeight: '500' }}>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredTasks.map(t => (
                                    <tr key={t.id} className="table-row-hover" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            <span style={{ color: 'var(--primary-color)', fontWeight: '700', fontSize: '0.85rem' }}>#{t.id}</span>
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            <div style={{ maxWidth: '250px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', fontWeight: '500' }}>
                                                {t.requestText}
                                            </div>
                                            {t.clientID && <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>Client ID: {t.clientID}</div>}
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                                                <div style={{ width: '24px', height: '24px', borderRadius: '50%', background: 'rgba(255,255,255,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.7rem' }}>👤</div>
                                                <span style={{ fontSize: '0.9rem' }}>{activeTab === 'inbox' ? t.owner : t.assignee}</span>
                                            </div>
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            <span style={{
                                                padding: '0.25rem 0.6rem',
                                                borderRadius: '20px',
                                                fontSize: '0.7rem',
                                                fontWeight: '700',
                                                letterSpacing: '0.025em',
                                                background: t.status === 'OPEN' ? 'rgba(245, 158, 11, 0.1)' : 'rgba(16, 185, 129, 0.1)',
                                                color: t.status === 'OPEN' ? '#f59e0b' : '#10b981',
                                                border: `1px solid ${t.status === 'OPEN' ? 'rgba(245, 158, 11, 0.2)' : 'rgba(16, 185, 129, 0.2)'}`
                                            }}>
                                                {t.status || 'OPEN'}
                                            </span>
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                                            {new Date(t.createTime).toLocaleDateString()}
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem', textAlign: 'right' }}>
                                            <Button variant="secondary" onClick={() => setSelectedTask(t)} style={{ padding: '0.4rem 0.9rem', fontSize: '0.85rem' }}>
                                                Open Details
                                            </Button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div style={{ padding: '6rem', textAlign: 'center', color: '#64748b' }}>
                        <div style={{ fontSize: '3rem', marginBottom: '1.5rem', opacity: 0.5 }}>📫</div>
                        <h3>No activity here</h3>
                        <p>Tasks categorized as {activeTab} will appear in this control panel.</p>
                    </div>
                )}
            </div>

            {/* Create Modal */}
            <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Initiate Ad-Hoc Task" maxWidth="500px">
                <div style={{ padding: '0.5rem' }}>
                    <div style={{ background: 'var(--hover-bg)', padding: '1rem', borderRadius: '12px', border: '1px solid var(--glass-border)', marginBottom: '1.5rem' }}>
                        <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                            <strong style={{ color: 'var(--primary-color)' }}>Info:</strong> Ad-hoc tasks are standalone requests used for manual investigations or internal coordination.
                        </p>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                        <div>
                            <label style={{ display: 'block', marginBottom: '0.6rem', fontSize: '0.875rem', fontWeight: '600', color: '#94a3b8' }}>Responsible Analyst</label>
                            <select
                                value={newTask.assignee}
                                onChange={(e) => setNewTask({ ...newTask, assignee: e.target.value })}
                                style={{ width: '100%', padding: '0.75rem', background: 'var(--input-bg)', color: 'var(--text-color)', border: '1px solid var(--input-border)', borderRadius: '8px', outline: 'none' }}
                            >
                                <option value="">Select Target User...</option>
                                {users.map(u => (
                                    <option key={u.username} value={u.username}>{u.username} ({u.role})</option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label style={{ display: 'block', marginBottom: '0.6rem', fontSize: '0.875rem', fontWeight: '600', color: '#94a3b8' }}>Associated Client (Optional)</label>
                            <select
                                value={newTask.clientID}
                                onChange={(e) => setNewTask({ ...newTask, clientID: e.target.value })}
                                style={{ width: '100%', padding: '0.75rem', background: 'var(--input-bg)', color: 'var(--text-color)', border: '1px solid var(--input-border)', borderRadius: '8px', outline: 'none' }}
                            >
                                <option value="">No Client Linked</option>
                                {clients.map(c => (
                                    <option key={c.clientID} value={c.clientID}>
                                        {c.firstName} {c.lastName} (ID: {c.clientID})
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label style={{ display: 'block', marginBottom: '0.6rem', fontSize: '0.875rem', fontWeight: '600', color: '#94a3b8' }}>Request Instructions</label>
                            <textarea
                                value={newTask.requestText}
                                onChange={(e) => setNewTask({ ...newTask, requestText: e.target.value })}
                                style={{ width: '100%', height: '120px', padding: '0.75rem', background: 'var(--input-bg)', color: 'var(--text-color)', border: '1px solid var(--input-border)', borderRadius: '8px', outline: 'none', resize: 'none' }}
                                placeholder="Detail the specific actions required..."
                            />
                        </div>

                        <Button onClick={handleCreate} disabled={creating} style={{ marginTop: '0.5rem', background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)', border: 'none', color: 'white', height: '48px' }}>
                            {creating ? 'Processing Dispatch...' : 'Dispatch Request'}
                        </Button>
                    </div>
                </div>
            </Modal>

            {/* View/Respond Modal */}
            <Modal isOpen={!!selectedTask} onClose={() => setSelectedTask(null)} title="Ad-Hoc Task Lifecycle" maxWidth="650px">
                {selectedTask && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem', background: 'var(--hover-bg)', padding: '1.25rem', borderRadius: '12px', border: '1px solid var(--glass-border)' }}>
                            <div className="info-item"><strong style={{ fontSize: '0.7rem' }}>Requestor</strong><span style={{ fontSize: '0.9rem' }}>{selectedTask.owner}</span></div>
                            <div className="info-item"><strong style={{ fontSize: '0.7rem' }}>Assignee</strong><span style={{ fontSize: '0.9rem' }}>{selectedTask.assignee}</span></div>
                            <div className="info-item"><strong style={{ fontSize: '0.7rem' }}>Status</strong>
                                <span style={{ fontSize: '0.85rem', color: selectedTask.status === 'OPEN' ? '#f59e0b' : '#10b981' }}>{selectedTask.status}</span>
                            </div>
                            <div className="info-item"><strong style={{ fontSize: '0.7rem' }}>Creation</strong><span style={{ fontSize: '0.85rem' }}>{new Date(selectedTask.createTime).toLocaleDateString()}</span></div>
                        </div>

                        <div className="glass-section" style={{ background: 'rgba(59, 130, 246, 0.03)', borderLeft: '4px solid #3b82f6', margin: 0, padding: '1.25rem' }}>
                            <h4 style={{ margin: '0 0 0.75rem 0', color: '#60a5fa', fontSize: '0.9rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Initial Request</h4>
                            <p style={{ margin: 0, fontSize: '1rem', lineHeight: '1.6', color: '#f8fafc' }}>{selectedTask.requestText}</p>
                        </div>

                        {selectedTask.responseText && (
                            <div className="glass-section" style={{ background: 'rgba(16, 185, 129, 0.03)', borderLeft: '4px solid #10b981', margin: 0, padding: '1.25rem' }}>
                                <h4 style={{ margin: '0 0 0.75rem 0', color: '#34d399', fontSize: '0.9rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Latest Resolution</h4>
                                <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.75rem', display: 'flex', justifyContent: 'space-between' }}>
                                    <span>Provided by: <strong>{selectedTask.responder || 'Unknown'}</strong></span>
                                </div>
                                <p style={{ margin: 0, fontSize: '1rem', lineHeight: '1.6', color: 'var(--text-color)' }}>{selectedTask.responseText}</p>
                            </div>
                        )}

                        {selectedTask.comments && selectedTask.comments.length > 0 && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                                <h4 style={{ margin: 0, fontSize: '0.85rem', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Communication Audit</h4>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                    {selectedTask.comments.map((c, i) => (
                                        <div key={i} style={{ display: 'flex', gap: '1rem' }}>
                                            <div style={{ width: '2px', background: 'rgba(255,255,255,0.1)', position: 'relative' }}>
                                                <div style={{ position: 'absolute', top: '8px', left: '-4px', width: '10px', height: '10px', borderRadius: '50%', background: '#3b82f6' }} />
                                            </div>
                                            <div style={{ flex: 1, color: '#f8fafc', fontSize: '0.95rem' }}>
                                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                                                    <strong style={{ fontSize: '0.8rem', color: '#64748b' }}>{c.author}</strong>
                                                    <span style={{ fontSize: '0.75rem', color: '#64748b' }}>{new Date(c.time).toLocaleTimeString()}</span>
                                                </div>
                                                <div style={{ background: 'rgba(255,255,255,0.02)', padding: '0.75rem', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.05)' }}>
                                                    {c.message}
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Lifecycle Actions */}
                        {selectedTask.assignee === user.username && (
                            <div style={{ marginTop: '0.5rem', borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: '1.5rem' }}>
                                {selectedTask.owner === user.username ? (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                        <div>
                                            <h4 style={{ margin: '0 0 0.5rem 0', fontSize: '0.95rem', color: '#f8fafc' }}>Final Approval</h4>
                                            <p style={{ margin: 0, fontSize: '0.875rem', color: '#94a3b8' }}>
                                                Review the provided resolution. Marking as complete will finalize the task and remove it from active workflows.
                                            </p>
                                        </div>
                                        <Button onClick={handleComplete} disabled={responding} style={{ background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)', border: 'none', color: 'white', height: '44px' }}>
                                            {responding ? 'Finalizing...' : 'Approve & Close Task'}
                                        </Button>
                                    </div>
                                ) : (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                        <h4 style={{ margin: '0', fontSize: '0.95rem', color: '#f8fafc' }}>Submit Resolution</h4>
                                        <textarea
                                            value={responseText}
                                            onChange={(e) => setResponseText(e.target.value)}
                                            placeholder="Provide detailed response to the requestor..."
                                            rows="4"
                                            style={{ width: '100%', padding: '0.75rem', background: 'var(--input-bg)', color: 'var(--text-color)', border: '1px solid var(--input-border)', borderRadius: '8px', outline: 'none' }}
                                        />
                                        <Button onClick={handleRespond} disabled={responding} style={{ background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)', border: 'none', color: 'white', height: '44px' }}>
                                            {responding ? 'Submitting...' : 'Send Resolution'}
                                        </Button>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                )}
            </Modal>
        </div>
    );
};

export default AdHocTaskList;
