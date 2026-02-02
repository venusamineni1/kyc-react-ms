import React, { useState, useEffect } from 'react';
import { adHocTaskService } from '../services/adHocTaskService';
import { useAuth } from '../contexts/AuthContext';
import Button from '../components/Button';
import Modal from '../components/Modal';
import { useNotification } from '../contexts/NotificationContext';
import { caseService } from '../services/caseService'; // To get users list
import { clientService } from '../services/clientService'; // To get clients list

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
            console.log("Loading users/clients...");
            const [usersData, clientsData] = await Promise.all([
                caseService.getAllUsers(),
                clientService.getClients(0, '')
            ]);
            console.log("Users Data:", usersData);
            console.log("Clients Data:", clientsData);

            setUsers(usersData);
            setClients(clientsData.content || (__isArray(clientsData) ? clientsData : []));
        } catch (err) {
            console.warn("Failed to load users or clients", err);
        }
    };

    // Helper to check array (since clientService returns object page sometimes)
    const __isArray = (a) => Array.isArray(a);

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
        } catch (err) {
            alert(err.message);
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
        } catch (err) {
            alert(err.message);
        } finally {
            setResponding(false);
        }
    };

    const handleComplete = async () => {
        if (!confirm("Are you sure you want to complete this task?")) return;
        setResponding(true);
        try {
            await adHocTaskService.completeTask(selectedTask.id);
            setSelectedTask(null);
            loadTasks();
            notify('Task completed successfully', 'success');
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
            return t.owner === user.username && t.assignee !== user.username; // Created by me, assigned to someone else
        }
    });

    return (
        <div>
            <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
                <h1 style={{ margin: 0 }}>Ad-Hoc Tasks</h1>
                <div style={{ display: 'flex', gap: '1rem' }}>
                    <Button onClick={() => setIsCreateOpen(true)}>+ New Task</Button>
                </div>
            </header>

            {loading && <p className="loading">Loading tasks...</p>}
            {error && <p className="error">{error}</p>}

            {!loading && !error && (
                <>
                    <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem' }}>
                        <button
                            className={`tab-btn ${activeTab === 'inbox' ? 'active' : ''}`}
                            onClick={() => setActiveTab('inbox')}
                            style={{ background: 'none', border: 'none', color: activeTab === 'inbox' ? 'var(--primary-color)' : 'var(--text-secondary)', fontSize: '1.1rem', cursor: 'pointer', borderBottom: activeTab === 'inbox' ? '2px solid var(--primary-color)' : 'none', padding: '0.5rem 1rem' }}
                        >
                            Inbox ({tasks.filter(t => t.assignee === user.username).length})
                        </button>
                        <button
                            className={`tab-btn ${activeTab === 'sent' ? 'active' : ''}`}
                            onClick={() => setActiveTab('sent')}
                            style={{ background: 'none', border: 'none', color: activeTab === 'sent' ? 'var(--primary-color)' : 'var(--text-secondary)', fontSize: '1.1rem', cursor: 'pointer', borderBottom: activeTab === 'sent' ? '2px solid var(--primary-color)' : 'none', padding: '0.5rem 1rem' }}
                        >
                            Sent / Pending
                        </button>
                    </div>

                    <section className="glass-section">
                        {filteredTasks.length > 0 ? (
                            <table>
                                <thead>
                                    <tr>
                                        <th>Task ID</th>
                                        <th>Subject</th>
                                        <th>{activeTab === 'inbox' ? 'From' : 'To'}</th>
                                        <th>Status</th>
                                        <th>Created</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredTasks.map(t => (
                                        <tr key={t.id}>
                                            <td>{t.id}</td>
                                            <td>{t.requestText.substring(0, 30)}...</td>
                                            <td>{activeTab === 'inbox' ? t.owner : t.assignee}</td>
                                            <td><span className={`status-badge ${t.status === 'OPEN' ? 'pending' : 'active'}`}>{t.status || 'OPEN'}</span></td>
                                            <td>{new Date(t.createTime).toLocaleDateString()}</td>
                                            <td>
                                                <Button variant="secondary" onClick={() => setSelectedTask(t)}>View</Button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        ) : (
                            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                                No tasks found in {activeTab}.
                            </div>
                        )}
                    </section>
                </>
            )}

            {/* Create Modal */}
            <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="New Ad-Hoc Task" maxWidth="500px">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem' }}>Assign To</label>
                        <select
                            value={newTask.assignee}
                            onChange={(e) => setNewTask({ ...newTask, assignee: e.target.value })}
                            style={{ width: '100%', padding: '0.5rem', background: 'rgba(255,255,255,0.05)', color: 'white', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                        >
                            <option value="">Select User...</option>
                            {users.map(u => (
                                <option key={u.username} value={u.username}>{u.username} ({u.role})</option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem' }}>Client (Optional)</label>
                        <select
                            value={newTask.clientID}
                            onChange={(e) => setNewTask({ ...newTask, clientID: e.target.value })}
                            style={{ width: '100%', padding: '0.5rem', background: 'rgba(255,255,255,0.05)', color: 'white', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                        >
                            <option value="">Select Client...</option>
                            {clients.map(c => (
                                <option key={c.clientID} value={c.clientID}>
                                    {c.firstName} {c.lastName} (ID: {c.clientID})
                                </option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem' }}>Request</label>
                        <textarea
                            value={newTask.requestText}
                            onChange={(e) => setNewTask({ ...newTask, requestText: e.target.value })}
                            style={{ width: '100%', height: '100px', padding: '0.5rem', background: 'rgba(255,255,255,0.05)', color: 'white', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                            placeholder="What do you need?"
                        />
                    </div>
                    <Button onClick={handleCreate} disabled={creating}>{creating ? 'Creating...' : 'Send Task'}</Button>
                </div>
            </Modal>

            {/* View/Respond Modal */}
            <Modal isOpen={!!selectedTask} onClose={() => setSelectedTask(null)} title="Task Details" maxWidth="600px">
                {selectedTask && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <div className="case-info-grid" style={{ gridTemplateColumns: '1fr 1fr' }}>
                            <div className="info-item"><strong>From</strong><span>{selectedTask.owner}</span></div>
                            <div className="info-item"><strong>To</strong><span>{selectedTask.assignee}</span></div>
                            <div className="info-item"><strong>Status</strong><span>{selectedTask.status}</span></div>
                            <div className="info-item"><strong>Client ID</strong><span>{selectedTask.clientID || '-'}</span></div>
                        </div>

                        <div className="glass-section" style={{ background: 'rgba(255,255,255,0.03)' }}>
                            <h4>Request</h4>
                            <p>{selectedTask.requestText}</p>
                        </div>

                        {selectedTask.responseText && (
                            <div className="glass-section" style={{ background: 'rgba(255,255,255,0.03)', borderLeft: '3px solid #6ee7b7' }}>
                                <h4>Latest Response</h4>
                                <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
                                    By: <strong>{selectedTask.responder || 'Unknown'}</strong>
                                </p>
                                <p>{selectedTask.responseText}</p>
                            </div>
                        )}

                        {selectedTask.comments && selectedTask.comments.length > 0 && (
                            <div className="glass-section" style={{ background: 'rgba(255,255,255,0.03)' }}>
                                <h4>Activity Log</h4>
                                {selectedTask.comments.map((c, i) => (
                                    <div key={i} style={{ marginBottom: '1rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '0.5rem' }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                                            <span><strong>{c.author}</strong></span>
                                            <span>{new Date(c.time).toLocaleString()}</span>
                                        </div>
                                        <p style={{ marginTop: '0.25rem' }}>{c.message}</p>
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* Actions */}
                        {selectedTask.assignee === user.username && (
                            <div>
                                {selectedTask.owner === user.username ? (
                                    // I am the owner -> I should verify/complete
                                    <>
                                        <h4 style={{ marginBottom: '0.5rem' }}>Close Task</h4>
                                        <p style={{ fontSize: '0.9rem', marginBottom: '1rem', color: 'var(--text-secondary)' }}>
                                            Review the response above. If satisfied, complete the task.
                                        </p>
                                        <Button onClick={handleComplete} disabled={responding} style={{ backgroundColor: '#10b981' }}>
                                            {responding ? 'Processing...' : 'Complete Task'}
                                        </Button>
                                    </>
                                ) : (
                                    // I am the assignee (not owner) -> I should respond
                                    <>
                                        <h4 style={{ marginBottom: '0.5rem' }}>Add Response</h4>
                                        <textarea
                                            value={responseText}
                                            onChange={(e) => setResponseText(e.target.value)}
                                            placeholder="Type your response here..."
                                            rows="4"
                                            style={{ width: '100%', marginBottom: '1rem', padding: '0.5rem', background: 'rgba(255,255,255,0.05)', color: 'white', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                                        />
                                        <Button onClick={handleRespond} disabled={responding}>
                                            {responding ? 'Sending...' : 'Send Response'}
                                        </Button>
                                    </>
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
