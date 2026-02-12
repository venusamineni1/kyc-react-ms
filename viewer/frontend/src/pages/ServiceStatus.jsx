import React, { useState, useEffect } from 'react';
import apiClient from '../services/apiClient';
import { useNotification } from '../contexts/NotificationContext';
import './ServiceStatus.css';

const ServiceStatus = () => {
    const [services, setServices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(null);
    const { notify } = useNotification();

    const fetchStatus = async () => {
        try {
            // If fetching silently (auto-refresh), don't set main loading
            // But initial load needs it.
            const data = await apiClient.request('/services');
            setServices(data || []);
        } catch (error) {
            console.error("Failed to fetch service status", error);
            // Don't notify on every poll failure to avoid spam
            if (loading) notify('Failed to load service status', 'error');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchStatus();
        const interval = setInterval(fetchStatus, 5000); // Poll every 5 seconds
        return () => clearInterval(interval);
    }, []);

    const handleAction = async (serviceName, action) => {
        setActionLoading(`${serviceName}-${action}`);
        try {
            await apiClient.post(`/services/${serviceName}/${action}`);
            notify(`Service ${serviceName} ${action} triggered successfully`, 'success');
            // Refresh status immediately, though it might take time to reflect
            setTimeout(fetchStatus, 1000);
            setTimeout(fetchStatus, 3000); // Check again later
        } catch (error) {
            console.error(`Failed to ${action} ${serviceName}`, error);
            notify(`Failed to ${action} ${serviceName}: ${error.message}`, 'error');
        } finally {
            setActionLoading(null);
        }
    };

    return (
        <div className="service-status-container">
            <h1 className="page-title">System Status</h1>

            {loading ? (
                <div className="loading-spinner">Loading...</div>
            ) : (
                <div className="status-card">
                    <table className="status-table">
                        <thead>
                            <tr>
                                <th>Service Name</th>
                                <th>Port</th>
                                <th>Status</th>
                                <th style={{ textAlign: 'right' }}>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {services.map((service) => (
                                <tr key={service.key}>
                                    <td>
                                        <div className="service-name-cell">
                                            <strong>{service.name}</strong>
                                            <span className="service-key">{service.key}</span>
                                        </div>
                                    </td>
                                    <td>{service.port}</td>
                                    <td>
                                        <span className={`status-badge ${service.status.toLowerCase()}`}>
                                            {service.status}
                                        </span>
                                    </td>
                                    <td className="actions-cell">
                                        <button
                                            className="action-btn start-btn"
                                            disabled={service.status === 'UP' || actionLoading}
                                            onClick={() => handleAction(service.key, 'start')}
                                            title="Start Service"
                                        >
                                            Start
                                        </button>
                                        <button
                                            className="action-btn stop-btn"
                                            disabled={service.status === 'DOWN' || actionLoading}
                                            onClick={() => handleAction(service.key, 'stop')}
                                            title="Stop Service"
                                        >
                                            Stop
                                        </button>
                                        <button
                                            className="action-btn restart-btn"
                                            disabled={actionLoading}
                                            onClick={() => handleAction(service.key, 'restart')}
                                            title="Restart Service"
                                        >
                                            Restart
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default ServiceStatus;
