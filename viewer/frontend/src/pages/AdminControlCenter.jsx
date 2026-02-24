import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const AdminCard = ({ to, title, description, icon }) => (
    <Link to={to} className="glass-section dashboard-card" style={{ textDecoration: 'none', display: 'flex', alignItems: 'flex-start', gap: '1rem', transition: 'transform 0.2s' }}>
        <div style={{ fontSize: '2rem', color: 'var(--primary-color)' }}>{icon}</div>
        <div>
            <h3 style={{ margin: '0 0 0.5rem 0', color: 'var(--text-color)' }}>{title}</h3>
            <p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{description}</p>
        </div>
    </Link>
);

const AdminControlCenter = () => {
    const { hasPermission } = useAuth();

    if (!hasPermission('MANAGE_CONFIG')) {
        return (
            <div className="glass-section">
                <h2>Access Denied</h2>
                <p>You do not have permission to view the Admin Control Center.</p>
                <Link to="/" className="btn btn-secondary">Return to Dashboard</Link>
            </div>
        );
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            <div>
                <h2 style={{ marginBottom: '0.5rem' }}>Admin Control Center</h2>
                <p style={{ color: 'var(--text-secondary)', marginTop: 0 }}>Manage system configurations, mappings, and backend services.</p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.5rem' }}>
                <AdminCard
                    to="/admin/material-configs"
                    title="Material Change Rules"
                    description="Configure which client profile updates trigger compliance review workflows."
                    icon="📋"
                />

                <AdminCard
                    to="/admin/batch-mapping"
                    title="Screening XML Mapping"
                    description="Define how internal client data maps to external screening provider XML schemas."
                    icon="🔄"
                />

                <AdminCard
                    to="/admin/batch-pipeline"
                    title="Screening Pipeline Config"
                    description="Schedule and monitor automated batch screening runs via SFTP."
                    icon="⚙️"
                />

                <AdminCard
                    to="/admin/risk-mapping"
                    title="Risk JSON Mapping"
                    description="Configure field mappings for automated risk assessment engine payloads."
                    icon="📊"
                />

                <AdminCard
                    to="/admin/risk-pipeline"
                    title="Risk Pipeline Config"
                    description="Manage scheduled batch operations for the Risk Assessment microservice."
                    icon="⚖️"
                />

                <AdminCard
                    to="/admin/services"
                    title="System Status"
                    description="Real-time monitor for all KYC microservices (Risk, Screening, Auth, Gateway)."
                    icon="🟢"
                />

                <AdminCard
                    to="/history"
                    title="Batch Execution History"
                    description="Audit logs and status reports for all completed screening and risk batch jobs."
                    icon="📜"
                />
            </div>
        </div>
    );
};

export default AdminControlCenter;
