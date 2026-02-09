import React, { useState, useEffect } from 'react';
import Button from '../components/Button';
import { caseService } from '../services/caseService';
import { useNotification } from '../contexts/NotificationContext';

const AdminWorkflowDashboard = () => {
    const { notify } = useNotification();
    const [activeTab, setActiveTab] = useState('tasks');
    const [tasks, setTasks] = useState([]);
    const [processes, setProcesses] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            if (activeTab === 'tasks') {
                const data = await caseService.getAdminTasks();
                setTasks(data);
            } else {
                const data = await caseService.getAdminProcesses();
                setProcesses(data);
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, [activeTab]);

    const handleTerminate = async (id) => {
        if (!window.confirm('Are you sure you want to terminate this process? This action cannot be undone.')) return;
        try {
            await caseService.terminateProcess(id);
            fetchData();
            notify('Process terminated successfully', 'success');
        } catch (err) {
            notify('Failed to terminate: ' + err.message, 'error');
        }
    };

    const handleDeleteAll = async () => {
        if (!window.confirm('DANGER: This will delete ALL active tasks and processes. Are you absolutely sure?')) return;
        try {
            await caseService.deleteAllTasks();
            fetchData();
            notify('All active tasks/processes deleted', 'success');
        } catch (err) {
            notify('Failed to delete all: ' + err.message, 'error');
        }
    };

    return (
        <div className="glass-section">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <h2 style={{ margin: 0 }}>Workflow Dashboard</h2>
                <Button variant="danger" onClick={handleDeleteAll}>Delete ALL Processes</Button>
            </div>

            <div className="tabs" style={{ marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', display: 'flex', gap: '1rem' }}>
                <button
                    className={`btn ${activeTab === 'tasks' ? '' : 'btn-secondary'}`}
                    onClick={() => setActiveTab('tasks')}
                    style={activeTab === 'tasks' ? { borderBottom: '2px solid var(--primary-color)', borderRadius: '0' } : { border: 'none' }}
                >
                    All Active Tasks
                </button>
                <button
                    className={`btn ${activeTab === 'processes' ? '' : 'btn-secondary'}`}
                    onClick={() => setActiveTab('processes')}
                    style={activeTab === 'processes' ? { borderBottom: '2px solid var(--primary-color)', borderRadius: '0' } : { border: 'none' }}
                >
                    Running Cases
                </button>
            </div>

            {loading ? <p className="loading">Loading...</p> : error ? <p className="error">{error}</p> : (
                <>
                    {activeTab === 'tasks' && (
                        tasks.length === 0 ? <p>No active tasks found.</p> : (
                            <table>
                                <thead>
                                    <tr>
                                        <th>Task Name</th>
                                        <th>Assignee</th>
                                        <th>Case ID</th>
                                        <th>Created</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {tasks.map(t => (
                                        <tr key={t.taskId}>
                                            <td>{t.name}</td>
                                            <td><span className="status-badge">{t.assignee || 'Unassigned'}</span></td>
                                            <td>{t.caseId}</td>
                                            <td>{new Date(t.createTime).toLocaleString()}</td>
                                            <td>
                                                <small>ID: {t.taskId}</small>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )
                    )}

                    {activeTab === 'processes' && (
                        processes.length === 0 ? <p>No running cases found.</p> : (
                            <table>
                                <thead>
                                    <tr>
                                        <th>Case Definition</th>
                                        <th>Started</th>
                                        <th>Initiator</th>
                                        <th>Case ID</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {processes.map(p => (
                                        <tr key={p.id}>
                                            <td>{p.definitionKey}</td>
                                            <td>{new Date(p.startTime).toLocaleString()}</td>
                                            <td>{p.initiator}</td>
                                            <td>{p.caseId}</td>
                                            <td>
                                                <Button variant="danger" onClick={() => handleTerminate(p.id)}>Terminate</Button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )
                    )}
                </>
            )}
        </div>
    );
};

export default AdminWorkflowDashboard;
