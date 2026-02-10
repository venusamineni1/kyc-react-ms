import React, { useState, useEffect } from 'react';
import Button from '../components/Button';
import { useAuth } from '../contexts/AuthContext';
import { useNotification } from '../contexts/NotificationContext';
import apiClient from '../services/apiClient';

const AdminPermissions = () => {
    const { hasPermission } = useAuth();
    const { notify } = useNotification();
    const [roles, setRoles] = useState({});
    const [allPermissions, setAllPermissions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editingRole, setEditingRole] = useState(null);
    const [selectedPermissions, setSelectedPermissions] = useState([]);
    const [permSearch, setPermSearch] = useState('');

    const fetchPermissions = async () => {
        setLoading(true);
        try {
            const data = await apiClient.get('/permissions');
            setRoles(data);

            const allData = await apiClient.get('/permissions/all');
            setAllPermissions(allData);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchPermissions();
    }, []);

    const handleEditClick = (role, currentPermissions) => {
        setEditingRole(role);
        setSelectedPermissions([...currentPermissions]);
    };

    const togglePermission = (permission) => {
        if (selectedPermissions.includes(permission)) {
            setSelectedPermissions(selectedPermissions.filter(p => p !== permission));
        } else {
            setSelectedPermissions([...selectedPermissions, permission]);
        }
    };

    const handleSave = async () => {
        try {
            await apiClient.post(`/permissions/role/${editingRole}`, { permissions: selectedPermissions });
            setEditingRole(null);
            fetchPermissions();
            notify('Permissions updated successfully', 'success');
        } catch (err) {
            notify('Update failed: ' + err.message, 'error');
        }
    };

    const getPermissionCategory = (perm) => {
        if (perm.includes('USER')) return 'User Management';
        if (perm.includes('CLIENT')) return 'Client Data';
        if (perm.includes('CASE')) return 'Workflow/Cases';
        if (perm.includes('RISK') || perm.includes('SCREENING')) return 'Compliance';
        if (perm.includes('CONFIG')) return 'System Setup';
        return 'General';
    };

    if (!hasPermission('MANAGE_PERMISSIONS')) return <div className="error-container glass-section"><h3>Unauthorized</h3><p>Access Denied</p></div>;

    const filteredAllPerms = allPermissions.filter(p => p.toLowerCase().includes(permSearch.toLowerCase()));

    return (
        <div className="admin-permissions-page">
            <div className="page-header" style={{ marginBottom: '2.5rem' }}>
                <h1 style={{ fontSize: '2rem', margin: '0 0 0.5rem 0', background: 'linear-gradient(135deg, #fff 0%, #94a3b8 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                    Access Control Matrix
                </h1>
                <p style={{ color: '#94a3b8', margin: 0 }}>Configure and audit granular permissions for system security roles</p>
            </div>

            {loading ? (
                <div style={{ padding: '4rem', textAlign: 'center' }} className="loading">Initializing security modules...</div>
            ) : error ? (
                <div className="error-container glass-section" style={{ color: '#ef4444' }}>{error}</div>
            ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: '2rem' }}>
                    {Object.entries(roles).map(([role, permissions]) => (
                        <div key={role} className="glass-section role-card" style={{ margin: 0, padding: '1.75rem', display: 'flex', flexDirection: 'column', borderTop: '4px solid #6366f1' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                                    <div style={{ width: '40px', height: '40px', borderRadius: '10px', background: 'rgba(99, 102, 241, 0.1)', color: '#6366f1', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                        🛡️
                                    </div>
                                    <h3 style={{ margin: 0, fontSize: '1.25rem' }}>{role.replace('_', ' ')}</h3>
                                </div>
                                <span style={{ fontSize: '0.75rem', fontWeight: 'bold', color: '#94a3b8', background: 'rgba(255,255,255,0.05)', padding: '0.25rem 0.6rem', borderRadius: '4px' }}>
                                    {permissions.length} PERMS
                                </span>
                            </div>

                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', flexGrow: 1, marginBottom: '2rem' }}>
                                {permissions.length > 0 ? permissions.map(p => (
                                    <span key={p} style={{
                                        fontSize: '0.7rem',
                                        padding: '0.2rem 0.6rem',
                                        borderRadius: '4px',
                                        background: 'rgba(255,255,255,0.05)',
                                        color: '#cbd5e1',
                                        border: '1px solid rgba(255,255,255,0.1)',
                                        fontWeight: '500'
                                    }}>
                                        {p}
                                    </span>
                                )) : <span style={{ color: '#64748b', fontStyle: 'italic', fontSize: '0.875rem' }}>No direct permissions assigned</span>}
                            </div>

                            <div style={{ textAlign: 'right' }}>
                                <Button variant="secondary" onClick={() => handleEditClick(role, permissions)} style={{ width: '100%', justifyContent: 'center' }}>
                                    Edit Role Configuration
                                </Button>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Edit Role Modal */}
            {editingRole && (
                <div className="modal" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.85)', backdropFilter: 'blur(8px)' }}>
                    <div className="modal-content glass-section" style={{ width: '100%', maxWidth: '750px', maxHeight: '90vh', display: 'flex', flexDirection: 'column', padding: '0', borderRadius: '16px', overflow: 'hidden' }}>
                        <div style={{ padding: '1.5rem 2rem', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
                            <h3 style={{ margin: '0 0 0.25rem 0' }}>Configure Role: {editingRole}</h3>
                            <p style={{ color: '#94a3b8', margin: 0, fontSize: '0.875rem' }}>Toggle granular permissions to adjust access levels</p>
                        </div>

                        <div style={{ padding: '1rem 2rem', background: 'rgba(15, 23, 42, 0.4)' }}>
                            <input
                                type="text"
                                placeholder="Filter permissions..."
                                value={permSearch}
                                onChange={(e) => setPermSearch(e.target.value)}
                                style={{
                                    width: '100%',
                                    padding: '0.75rem 1rem',
                                    background: 'rgba(15, 23, 42, 0.8)',
                                    border: '1px solid rgba(255,255,255,0.1)',
                                    borderRadius: '8px',
                                    color: 'white'
                                }}
                            />
                        </div>

                        <div style={{ padding: '2rem', overflowY: 'auto', flexGrow: 1, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                            {filteredAllPerms.map(p => (
                                <label key={p} className="table-row-hover" style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '0.75rem',
                                    cursor: 'pointer',
                                    padding: '0.75rem 1rem',
                                    background: selectedPermissions.includes(p) ? 'rgba(99, 102, 241, 0.1)' : 'rgba(255,255,255,0.03)',
                                    borderRadius: '8px',
                                    border: selectedPermissions.includes(p) ? '1px solid rgba(99, 102, 241, 0.3)' : '1px solid rgba(255,255,255,0.05)',
                                    transition: 'all 0.2s'
                                }}>
                                    <input
                                        type="checkbox"
                                        checked={selectedPermissions.includes(p)}
                                        onChange={() => togglePermission(p)}
                                        style={{ width: '18px', height: '18px', cursor: 'pointer', accentColor: '#6366f1' }}
                                    />
                                    <div>
                                        <div style={{ fontSize: '0.9rem', fontWeight: '500', color: selectedPermissions.includes(p) ? 'white' : '#cbd5e1' }}>{p}</div>
                                        <div style={{ fontSize: '0.7rem', color: '#64748b' }}>{getPermissionCategory(p)}</div>
                                    </div>
                                </label>
                            ))}
                        </div>

                        <div style={{ padding: '1.5rem 2rem', background: 'rgba(255,255,255,0.02)', borderTop: '1px solid rgba(255,255,255,0.1)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div style={{ fontSize: '0.875rem', color: '#94a3b8' }}>
                                <strong>{selectedPermissions.length}</strong> active permissions
                            </div>
                            <div style={{ display: 'flex', gap: '1rem' }}>
                                <Button variant="secondary" onClick={() => setEditingRole(null)}>Discard Changes</Button>
                                <Button onClick={handleSave} style={{ background: 'linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)' }}>Update Access Control</Button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminPermissions;
