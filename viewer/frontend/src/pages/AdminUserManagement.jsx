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
    const [searchTerm, setSearchTerm] = useState('');

    const fetchUsers = async () => {
        setLoading(true);
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
            fetchUsers();
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

    const getStats = () => {
        const stats = {
            total: users.length,
            admins: users.filter(u => u.role === 'ADMIN').length,
            analysts: users.filter(u => u.role.includes('ANALYST') || u.role.includes('REVIEWER')).length,
            others: users.filter(u => u.role === 'USER' || u.role === 'AUDITOR').length
        };
        return stats;
    };

    const filteredUsers = users.filter(u =>
        u.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
        u.role.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const getRoleBadgeStyle = (role) => {
        switch (role) {
            case 'ADMIN': return { backgroundColor: 'rgba(239, 68, 68, 0.2)', color: '#f87171', border: '1px solid rgba(239, 68, 68, 0.3)' };
            case 'KYC_ANALYST': return { backgroundColor: 'rgba(59, 130, 246, 0.2)', color: '#60a5fa', border: '1px solid rgba(59, 130, 246, 0.3)' };
            case 'KYC_REVIEWER': return { backgroundColor: 'rgba(139, 92, 246, 0.2)', color: '#a78bfa', border: '1px solid rgba(139, 92, 246, 0.3)' };
            case 'AUDITOR': return { backgroundColor: 'rgba(245, 158, 11, 0.2)', color: '#fbbf24', border: '1px solid rgba(245, 158, 11, 0.3)' };
            default: return { backgroundColor: 'rgba(107, 114, 128, 0.2)', color: '#9ca3af', border: '1px solid rgba(107, 114, 128, 0.3)' };
        }
    };

    if (!hasPermission('MANAGE_USERS')) return <div className="error-container glass-section"><h3>Unauthorized</h3><p>You do not have permission to manage users.</p></div>;

    const stats = getStats();

    return (
        <div className="admin-user-management">
            {/* Header & Stats Section */}
            <div style={{ marginBottom: '2rem' }}>
                <div className="page-header" style={{ marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: 'none', paddingBottom: '0' }}>
                    <div>
                        <h1 style={{ fontSize: '2rem', margin: '0 0 0.5rem 0', background: 'linear-gradient(135deg, #fff 0%, #94a3b8 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                            User Management
                        </h1>
                        <p style={{ color: '#94a3b8', margin: 0 }}>Create and manage system users and their roles</p>
                    </div>
                    <Button onClick={() => setIsCreateModalOpen(true)} style={{ padding: '0.75rem 1.5rem', fontSize: '1rem' }}>
                        + Create New User
                    </Button>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.5rem' }}>
                    {[
                        { label: 'Total Users', value: stats.total, color: '#3b82f6' },
                        { label: 'Administrators', value: stats.admins, color: '#ef4444' },
                        { label: 'Operational Staff', value: stats.analysts, color: '#8b5cf6' },
                        { label: 'Other Users', value: stats.others, color: '#10b981' }
                    ].map((s, idx) => (
                        <div key={idx} className="glass-section" style={{ padding: '1.5rem', marginBottom: 0, textAlign: 'center', borderLeft: `4px solid ${s.color}` }}>
                            <div style={{ fontSize: '0.875rem', color: '#94a3b8', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{s.label}</div>
                            <div style={{ fontSize: '2rem', fontWeight: '700', color: '#f8fafc' }}>{s.value}</div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Main Content Area */}
            <div className="glass-section" style={{ padding: '0' }}>
                <div style={{ padding: '1.5rem', borderBottom: '1px solid rgba(255,255,255,0.1)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h3 style={{ margin: 0 }}>Active System Users</h3>
                    <div style={{ position: 'relative', width: '300px' }}>
                        <input
                            type="text"
                            placeholder="Search by username or role..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            style={{
                                width: '100%',
                                padding: '0.6rem 1rem',
                                background: 'rgba(15, 23, 42, 0.6)',
                                border: '1px solid rgba(255,255,255,0.1)',
                                borderRadius: '8px',
                                color: 'white',
                                outline: 'none'
                            }}
                        />
                    </div>
                </div>

                <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                            <tr style={{ background: 'rgba(255,255,255,0.03)' }}>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: '#94a3b8', fontWeight: '500' }}>Username</th>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: '#94a3b8', fontWeight: '500' }}>Role</th>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: '#94a3b8', fontWeight: '500' }}>Last Activity</th>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'right', color: '#94a3b8', fontWeight: '500' }}>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan="4" style={{ padding: '3rem', textAlign: 'center' }}>
                                        <div className="loading-spinner">Loading system users...</div>
                                    </td>
                                </tr>
                            ) : filteredUsers.length === 0 ? (
                                <tr>
                                    <td colSpan="4" style={{ padding: '3rem', textAlign: 'center', color: '#94a3b8' }}>
                                        No users found matches your search.
                                    </td>
                                </tr>
                            ) : (
                                filteredUsers.map((u) => (
                                    <tr key={u.username} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', transition: 'background 0.2s' }} className="table-row-hover">
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                                                <div style={{ width: '32px', height: '32px', borderRadius: '50%', background: 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: '600', fontSize: '0.75rem' }}>
                                                    {u.username.substring(0, 2).toUpperCase()}
                                                </div>
                                                <span style={{ fontWeight: '500' }}>{u.username}</span>
                                            </div>
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            <span style={{
                                                padding: '0.25rem 0.75rem',
                                                borderRadius: '20px',
                                                fontSize: '0.75rem',
                                                fontWeight: '600',
                                                textTransform: 'uppercase',
                                                letterSpacing: '0.025em',
                                                ...getRoleBadgeStyle(u.role)
                                            }}>
                                                {u.role.replace('_', ' ')}
                                            </span>
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem', color: '#94a3b8', fontSize: '0.875rem' }}>
                                            {u.lastLogin ? new Date(u.lastLogin).toLocaleString() : 'Never'}
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem', textAlign: 'right' }}>
                                            <Button variant="secondary" onClick={() => openEditModal(u)} style={{ padding: '0.4rem 0.8rem', fontSize: '0.875rem' }}>
                                                Manage
                                            </Button>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Edit User Modal */}
            {editingUser && (
                <div className="modal" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.8)', backdropFilter: 'blur(4px)' }}>
                    <div className="modal-content glass-section" style={{ width: '100%', maxWidth: '450px', padding: '2rem', borderRadius: '16px', border: '1px solid rgba(255,255,255,0.1)' }}>
                        <div style={{ marginBottom: '1.5rem' }}>
                            <h3 style={{ margin: '0 0 0.5rem 0' }}>Manage User Role</h3>
                            <p style={{ color: '#94a3b8', margin: 0 }}>Adjust permissions for <strong>{editingUser.username}</strong></p>
                        </div>

                        <div className="form-group" style={{ marginBottom: '2rem' }}>
                            <label style={{ display: 'block', marginBottom: '0.75rem', color: '#94a3b8', fontSize: '0.875rem' }}>System Access Role</label>
                            <select
                                value={editingUser.role}
                                onChange={(e) => setEditingUser({ ...editingUser, role: e.target.value })}
                                style={{
                                    width: '100%',
                                    padding: '0.75rem',
                                    background: 'rgba(15, 23, 42, 0.8)',
                                    border: '1px solid rgba(255,255,255,0.1)',
                                    borderRadius: '8px',
                                    color: 'white',
                                    cursor: 'pointer'
                                }}
                            >
                                <option value="ADMIN">ADMIN (Full Control)</option>
                                <option value="KYC_ANALYST">KYC_ANALYST (Onboarding)</option>
                                <option value="KYC_REVIEWER">KYC_REVIEWER (Supervisory)</option>
                                <option value="AFC_REVIEWER">AFC_REVIEWER (Financial Crime)</option>
                                <option value="ACO_REVIEWER">ACO_REVIEWER (Compliance)</option>
                                <option value="AUDITOR">AUDITOR (Read Only)</option>
                                <option value="USER">USER (General Access)</option>
                            </select>
                        </div>

                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                            <Button variant="secondary" onClick={() => setEditingUser(null)}>Cancel</Button>
                            <Button onClick={handleUpdateRole} style={{ background: 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)' }}>Save Changes</Button>
                        </div>
                    </div>
                </div>
            )}

            {/* Create User Modal */}
            {isCreateModalOpen && (
                <div className="modal" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.8)', backdropFilter: 'blur(4px)' }}>
                    <div className="modal-content glass-section" style={{ width: '100%', maxWidth: '500px', padding: '2rem', borderRadius: '16px', border: '1px solid rgba(255,255,255,0.1)' }}>
                        <div style={{ marginBottom: '1.5rem' }}>
                            <h3 style={{ margin: '0 0 0.5rem 0' }}>Create New Security Principal</h3>
                            <p style={{ color: '#94a3b8', margin: 0 }}>Provision a new account with specific system roles</p>
                        </div>

                        <form onSubmit={(e) => { e.preventDefault(); handleCreateUser(); }}>
                            <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                                <label style={{ display: 'block', marginBottom: '0.5rem', color: '#94a3b8', fontSize: '0.875rem' }}>Username</label>
                                <input
                                    type="text"
                                    placeholder="e.g. jdoe_kyc"
                                    value={newUser.username}
                                    onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
                                    style={{
                                        width: '100%',
                                        padding: '0.75rem',
                                        background: 'rgba(15, 23, 42, 0.8)',
                                        border: '1px solid rgba(255,255,255,0.1)',
                                        borderRadius: '8px',
                                        color: 'white'
                                    }}
                                    autoFocus
                                />
                            </div>
                            <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                                <label style={{ display: 'block', marginBottom: '0.5rem', color: '#94a3b8', fontSize: '0.875rem' }}>Initial Password</label>
                                <input
                                    type="password"
                                    placeholder="••••••••"
                                    value={newUser.password}
                                    onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
                                    style={{
                                        width: '100%',
                                        padding: '0.75rem',
                                        background: 'rgba(15, 23, 42, 0.8)',
                                        border: '1px solid rgba(255,255,255,0.1)',
                                        borderRadius: '8px',
                                        color: 'white'
                                    }}
                                />
                            </div>
                            <div className="form-group" style={{ marginBottom: '2rem' }}>
                                <label style={{ display: 'block', marginBottom: '0.5rem', color: '#94a3b8', fontSize: '0.875rem' }}>Designated Role</label>
                                <select
                                    value={newUser.role}
                                    onChange={(e) => setNewUser({ ...newUser, role: e.target.value })}
                                    style={{
                                        width: '100%',
                                        padding: '0.75rem',
                                        background: 'rgba(15, 23, 42, 0.8)',
                                        border: '1px solid rgba(255,255,255,0.1)',
                                        borderRadius: '8px',
                                        color: 'white',
                                        cursor: 'pointer'
                                    }}
                                >
                                    <option value="ADMIN">ADMIN</option>
                                    <option value="KYC_ANALYST">KYC_ANALYST</option>
                                    <option value="KYC_REVIEWER">KYC_REVIEWER</option>
                                    <option value="AFC_REVIEWER">AFC_REVIEWER</option>
                                    <option value="ACO_REVIEWER">ACO_REVIEWER</option>
                                    <option value="AUDITOR">AUDITOR</option>
                                    <option value="USER">USER</option>
                                </select>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                                <Button variant="secondary" onClick={() => setIsCreateModalOpen(false)} type="button">Cancel</Button>
                                <Button type="submit" style={{ background: 'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)' }}>Provision Account</Button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminUserManagement;
