import React, { useState, useEffect } from 'react';
import Button from '../components/Button';
import { useAuth } from '../contexts/AuthContext';
import { useNotification } from '../contexts/NotificationContext';
import apiClient from '../services/apiClient';

const AdminUserManagement = () => {
    const { hasPermission } = useAuth();
    const { notify } = useNotification();
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [newUser, setNewUser] = useState({ username: '', password: '', role: 'USER' });
    const [editingUser, setEditingUser] = useState(null);

    const fetchUsers = async () => {
        try {
            const data = await apiClient.get('/users');
            setUsers(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const openEditModal = (user) => {
        setEditingUser({ ...user });
    };

    const handleUpdateRole = async () => {
        if (!editingUser) return;
        try {
            await apiClient.put(`/users/${editingUser.username}/role`, { role: editingUser.role });
            setEditingUser(null);
            fetchUsers(); // Refresh list
            notify('Role updated successfully', 'success');
        } catch (err) {
            notify('Update failed: ' + err.message, 'error');
        }
    };

    const handleCreateUser = async () => {
        if (!newUser.username || !newUser.password) {
            notify("Username and password are required", 'warning');
            return;
        }
        try {
            await apiClient.post('/users', newUser);
            setIsCreateModalOpen(false);
            setNewUser({ username: '', password: '', role: 'USER' });
            fetchUsers();
            notify('User created successfully', 'success');
        } catch (err) {
            notify('Creation failed: ' + err.message, 'error');
        }
    };

    if (!hasPermission('MANAGE_USERS')) return <div className="error">Unauthorized</div>;

    return (
        <div className="glass-section">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <h2 style={{ margin: 0 }}>User Management</h2>
                <Button onClick={() => setIsCreateModalOpen(true)}>Create New User</Button>
            </div>

            {loading ? (
                <p className="loading">Loading users...</p>
            ) : error ? (
                <p className="error">{error}</p>
            ) : (
                <table>
                    <thead>
                        <tr>
                            <th>Username</th>
                            <th>Role</th>
                            <th>Last Login</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        {users.map(u => (
                            <tr key={u.username}>
                                <td>{u.username}</td>
                                <td><span className="status-badge">{u.role}</span></td>
                                <td>{u.lastLogin ? new Date(u.lastLogin).toLocaleString() : 'Never'}</td>
                                <td>
                                    <Button variant="secondary" onClick={() => openEditModal(u)}>Edit</Button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}

            {/* Edit User Modal */}
            {editingUser && (
                <div className="modal" style={{ display: 'block' }}>
                    <div className="modal-content">
                        <h3>Edit User: {editingUser.username}</h3>
                        <div className="form-group">
                            <label>Role</label>
                            <select
                                value={editingUser.role}
                                onChange={(e) => setEditingUser({ ...editingUser, role: e.target.value })}
                            >
                                <option value="ADMIN">ADMIN</option>
                                <option value="USER">USER</option>
                                <option value="KYC_ANALYST">KYC_ANALYST</option>
                                <option value="KYC_REVIEWER">KYC_REVIEWER</option>
                                <option value="AFC_REVIEWER">AFC_REVIEWER</option>
                                <option value="ACO_REVIEWER">ACO_REVIEWER</option>
                                <option value="AUDITOR">AUDITOR</option>
                            </select>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1.5rem' }}>
                            <Button variant="secondary" onClick={() => setEditingUser(null)}>Cancel</Button>
                            <Button onClick={handleUpdateRole}>Save Changes</Button>
                        </div>
                    </div>
                </div>
            )}

            {/* Create User Modal */}
            {isCreateModalOpen && (
                <div className="modal" style={{ display: 'block' }}>
                    <div className="modal-content">
                        <h3>Create New User</h3>
                        <form onSubmit={(e) => { e.preventDefault(); handleCreateUser(); }}>
                            <div className="form-group">
                                <label>Username</label>
                                <input
                                    type="text"
                                    value={newUser.username}
                                    onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
                                    style={{ width: '100%', padding: '0.5rem', marginTop: '0.5rem' }}
                                    autoFocus
                                />
                            </div>
                            <div className="form-group">
                                <label>Password</label>
                                <input
                                    type="password"
                                    value={newUser.password}
                                    onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
                                    style={{ width: '100%', padding: '0.5rem', marginTop: '0.5rem' }}
                                />
                            </div>
                            <div className="form-group">
                                <label>Role</label>
                                <select
                                    value={newUser.role}
                                    onChange={(e) => setNewUser({ ...newUser, role: e.target.value })}
                                    style={{ width: '100%', padding: '0.5rem', marginTop: '0.5rem' }}
                                >
                                    <option value="ADMIN">ADMIN</option>
                                    <option value="USER">USER</option>
                                    <option value="KYC_ANALYST">KYC_ANALYST</option>
                                    <option value="KYC_REVIEWER">KYC_REVIEWER</option>
                                    <option value="AFC_REVIEWER">AFC_REVIEWER</option>
                                    <option value="ACO_REVIEWER">ACO_REVIEWER</option>
                                    <option value="AUDITOR">AUDITOR</option>
                                </select>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1.5rem' }}>
                                <Button variant="secondary" onClick={() => setIsCreateModalOpen(false)} type="button">Cancel</Button>
                                <button type="submit" className="btn" style={{ backgroundColor: '#6366f1', color: 'white' }}>Create User</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminUserManagement;
