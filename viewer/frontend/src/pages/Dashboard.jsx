import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

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
    return (
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
                role="ADMIN"
            />
            <DashboardCard
                to="/admin/audits"
                id="auditCard"
                title="User Audit History"
                description="View logs of all user actions in the system."
                color="#ff6b6b"
                role="ADMIN"
            />
            <DashboardCard
                to="/admin/config"
                id="systemConfigCard"
                title="System Configuration"
                description="View backend system settings and properties."
                color="#6c757d" // Using a more standard color for consistency
                role="ADMIN"
            />
        </div>
    );
};

export default Dashboard;
