import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { clientService } from '../services/clientService';

const DashboardCard = ({ to, id, title, description, color, permission }) => {
    const { hasPermission } = useAuth();

    if (permission && !hasPermission(permission)) return null;

    return (
        <Link to={to} id={id} className="glass-section dashboard-card" style={{ textDecoration: 'none', transition: 'transform 0.2s' }}>
            <h3 style={{ marginTop: 0, color }}>{title}</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{description}</p>
        </Link>
    );
};

const Dashboard = () => {
    const { hasPermission } = useAuth();
    const [recentChanges, setRecentChanges] = React.useState([]);
    const [loading, setLoading] = React.useState(false);

    React.useEffect(() => {
        if (hasPermission('VIEW_CHANGES')) {
            setLoading(true);
            clientService.getMaterialChanges(0, 5)
                .then(data => setRecentChanges(data.content || []))
                .catch(err => console.error('Dashboard changes fetch failed', err))
                .finally(() => setLoading(false));
        }
    }, [hasPermission]);

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            <div className="dashboard-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.5rem' }}>
                <DashboardCard
                    to="/clients"
                    id="clientsCard"
                    title="Client Directory"
                    description="View and manage client profiles, identities, and related parties."
                    color="#4facfe"
                    permission="VIEW_CLIENTS"
                />
                <DashboardCard
                    to="/changes"
                    id="changesCard"
                    title="Material Changes"
                    description="Audit trail for all material changes across client entities."
                    color="#00f2fe"
                    permission="VIEW_CHANGES"
                />
                <DashboardCard
                    to="/users"
                    id="usersCard"
                    title="User Management"
                    description="Create users and manage role assignments."
                    color="#f093fb"
                    permission="MANAGE_USERS"
                />
                <DashboardCard
                    to="/permissions"
                    id="permissionsCard"
                    title="Role Permissions"
                    description="Configure fine-grained authorities for each system role."
                    color="#f6d365"
                    permission="MANAGE_PERMISSIONS"
                />
                <DashboardCard
                    to="/cases"
                    id="casesCard"
                    title="Case Manager"
                    description="Manage KYC lifecycles, approvals, and document verification."
                    color="#a1c4fd"
                    permission="MANAGE_CASES"
                />
                <DashboardCard
                    to="/admin/questionnaire"
                    id="questionnaireCard"
                    title="Questionnaire Config"
                    description="Manage questionnaire sections and questions for KYC cases."
                    color="#ff9a9e"
                    permission="MANAGE_CASES"
                />
                <DashboardCard
                    to="/admin/audits"
                    id="auditCard"
                    title="User Audit History"
                    description="View logs of all user actions in the system."
                    color="#ff6b6b"
                    permission="MANAGE_AUDITS"
                />
                <DashboardCard
                    to="/admin/config"
                    id="systemConfigCard"
                    title="System Configuration"
                    description="View backend system settings and properties."
                    color="#6c757d"
                    permission="MANAGE_CONFIG"
                />
            </div>

            {hasPermission('VIEW_CHANGES') && (
                <div className="glass-section" style={{ marginTop: '1rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                        <h3 style={{ margin: 0 }}>Recent Material Changes</h3>
                        <Link to="/changes" className="btn btn-secondary" style={{ fontSize: '0.8rem', padding: '0.3rem 0.8rem' }}>View All Audit Logs</Link>
                    </div>
                    {loading ? (
                        <p style={{ color: 'var(--text-secondary)' }}>Loading latest changes...</p>
                    ) : recentChanges.length > 0 ? (
                        <table style={{ fontSize: '0.85rem' }}>
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Client</th>
                                    <th>Field</th>
                                    <th>Category</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {recentChanges.map(change => (
                                    <tr key={change.changeID}>
                                        <td>{new Date(change.changeDate).toLocaleDateString()}</td>
                                        <td>{change.clientName || 'Client #' + change.clientID}</td>
                                        <td><strong>{change.columnName}</strong></td>
                                        <td>
                                            <span className={`status-badge ${change.category === 'RISK' ? 'suspended' : change.category === 'SCREENING' ? 'pending' : 'active'}`} style={{ fontSize: '0.65rem' }}>
                                                {change.category}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={`status-badge ${change.status === 'PENDING' ? 'rejected' : 'active'}`} style={{ fontSize: '0.65rem' }}>
                                                {change.status}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    ) : (
                        <p style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>No pending material changes found.</p>
                    )}
                </div>
            )}
        </div>
    );
};

export default Dashboard;
