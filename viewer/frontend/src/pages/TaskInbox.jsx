import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { caseService } from '../services/caseService';

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
        <div className="glass-section">
            <h2 style={{ marginBottom: '1.5rem' }}>My Task Inbox</h2>
            {loading ? (
                <p className="loading">Loading tasks...</p>
            ) : error ? (
                <p className="error">{error}</p>
            ) : tasks.length === 0 ? (
                <p>No tasks assigned to you or your groups.</p>
            ) : (
                <table>
                    <thead>
                        <tr>
                            <th>Task Name</th>
                            <th>Case ID</th>
                            <th>Client ID</th>
                            <th>Created</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        {tasks.map(task => (
                            <tr key={task.taskId}>
                                <td>{task.name}</td>
                                <td>
                                    {task.caseId ? (
                                        <Link to={`/cases/${task.caseId}`}>#{task.caseId}</Link>
                                    ) : (
                                        <span className="badge badge-info">Ad-Hoc</span>
                                    )}
                                </td>
                                <td>{task.clientID || '-'}</td>
                                <td>{new Date(task.createTime).toLocaleString()}</td>
                                <td>
                                    {task.caseId ? (
                                        <button className="btn btn-sm" onClick={() => handleProcessTask(task.caseId)}>
                                            Process Case
                                        </button>
                                    ) : (
                                        <Link to="/adhoc-tasks" className="btn btn-sm btn-secondary">
                                            View Task
                                        </Link>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </div>
    );
};

export default TaskInbox;
