import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useInbox } from '../contexts/InboxContext';
import { clientService } from '../services/clientService';
import { caseService } from '../services/caseService';

const DashboardCard = ({ to, id, title, description, color, permission, badgeCount }) => {
    const { hasPermission } = useAuth();

    if (permission && !hasPermission(permission)) return null;

    return (
        <Link to={to} id={id} className="glass-section dashboard-card" style={{ textDecoration: 'none', transition: 'transform 0.2s', position: 'relative' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <h3 style={{ marginTop: 0, color }}>{title}</h3>
                {badgeCount > 0 && (
                    <span style={{
                        backgroundColor: '#ef4444',
                        color: '#fff',
                        fontSize: '0.75rem',
                        padding: '0.2rem 0.6rem',
                        borderRadius: '12px',
                        fontWeight: 'bold',
                        boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                    }}>
                        {badgeCount} New
                    </span>
                )}
            </div>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.5rem' }}>{description}</p>
        </Link>
    );
};

const Dashboard = () => {
    const { hasPermission } = useAuth();
    const { inboxCount } = useInbox();
    const [recentChanges, setRecentChanges] = React.useState([]);
    const [loading, setLoading] = React.useState(false);
    const [kpi, setKpi] = React.useState({ open: 0, inReview: 0, approvedMonth: 0, pendingChanges: 0 });

    React.useEffect(() => {
        if (hasPermission('VIEW_CHANGES')) {
            setLoading(true);
            clientService.getMaterialChanges(0, 5)
                .then(data => {
                    const content = data.content || [];
                    setRecentChanges(content);
                    setKpi(k => ({ ...k, pendingChanges: data.totalElements || content.length }));
                })
                .catch(err => console.error('Dashboard changes fetch failed', err))
                .finally(() => setLoading(false));
        }
    }, [hasPermission]);

    React.useEffect(() => {
        if (hasPermission('MANAGE_CASES')) {
            caseService.getCases(0)
                .then(result => {
                    const cases = result.content || [];
                    const now = new Date();
                    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
                    const open = cases.filter(c => c.status !== 'APPROVED' && c.status !== 'REJECTED').length;
                    const inReview = cases.filter(c => (c.status || '').includes('REVIEW')).length;
                    const approvedMonth = cases.filter(c => c.status === 'APPROVED' && new Date(c.createdDate) >= startOfMonth).length;
                    setKpi(k => ({ ...k, open, inReview, approvedMonth }));
                })
                .catch(() => {});
        }
    }, [hasPermission]);

    const kpiCards = [
        { label: 'Open Cases', value: kpi.open, color: '#4facfe', icon: '📂' },
        { label: 'In Review', value: kpi.inReview, color: '#faad14', icon: '🔍' },
        { label: 'Approved This Month', value: kpi.approvedMonth, color: '#52c41a', icon: '✅' },
        { label: 'Pending Changes', value: kpi.pendingChanges, color: '#ff4d4f', icon: '⚡' },
    ];

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            {/* KPI summary row */}
            {hasPermission('MANAGE_CASES') && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
                    {kpiCards.map((card, i) => (
                        <div key={i} className="glass-section" style={{ padding: '1.25rem 1.5rem', marginBottom: 0, display: 'flex', alignItems: 'center', gap: '1rem', borderLeft: `4px solid ${card.color}` }}>
                            <div style={{ fontSize: '1.75rem', background: 'rgba(255,255,255,0.05)', width: '46px', height: '46px', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                                {card.icon}
                            </div>
                            <div>
                                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{card.label}</div>
                                <div style={{ fontSize: '1.75rem', fontWeight: 700, color: card.color }}>{card.value}</div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

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
                    to="/prospects"
                    id="prospectsCard"
                    title="Prospects Directory"
                    description="Create new clients and track their onboarding screening and risk progress."
                    color="#f6d365"
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
                    to="/inbox"
                    id="inboxCard"
                    title="My Task Inbox"
                    description="View and action your assigned KYC workflow tasks."
                    color="#48c6ef"
                    badgeCount={inboxCount}
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
