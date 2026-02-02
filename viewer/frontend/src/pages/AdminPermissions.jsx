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

    const fetchPermissions = async () => {
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

    if (!hasPermission('MANAGE_PERMISSIONS')) return <div className="error">Unauthorized</div>;

    return (
        <div className="glass-section">
            <h2 style={{ marginBottom: '1.5rem' }}>Role Permissions</h2>

            {loading ? (
                <p className="loading">Loading permissions...</p>
            ) : error ? (
                <p className="error">{error}</p>
            ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.5rem' }}>
                    {Object.entries(roles).map(([role, permissions]) => (
                        <div key={role} className="glass-section" style={{ margin: 0, padding: '1rem' }}>
                            <h3 style={{ color: 'var(--primary-color)', marginTop: 0 }}>{role}</h3>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                                {permissions.map(p => (
                                    <span key={p} className="status-badge" style={{ fontSize: '0.75rem' }}>{p}</span>
                                ))}
                            </div>
                            <div style={{ marginTop: '1rem', textAlign: 'right' }}>
                                <Button variant="secondary" style={{ padding: '0.3rem 0.6rem' }} onClick={() => handleEditClick(role, permissions)}>Edit Role</Button>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Edit Role Modal */}
            {editingRole && (
                <div className="modal" style={{ display: 'block' }}>
                    <div className="modal-content">
                        <h3>Edit Permissions: {editingRole}</h3>
                        <div style={{ maxHeight: '300px', overflowY: 'auto', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', margin: '1rem 0' }}>
                            {allPermissions.map(p => (
                                <label key={p} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', padding: '0.5rem', background: 'rgba(255,255,255,0.05)', borderRadius: '4px' }}>
                                    <input
                                        type="checkbox"
                                        checked={selectedPermissions.includes(p)}
                                        onChange={() => togglePermission(p)}
                                    />
                                    <span style={{ fontSize: '0.9rem' }}>{p}</span>
                                </label>
                            ))}
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                            <Button variant="secondary" onClick={() => setEditingRole(null)}>Cancel</Button>
                            <Button onClick={handleSave}>Save Changes</Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminPermissions;
