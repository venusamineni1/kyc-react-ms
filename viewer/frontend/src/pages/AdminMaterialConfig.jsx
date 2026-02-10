import React, { useState, useEffect } from 'react';
import { clientService } from '../services/clientService';
import Button from '../components/Button';
import { useNotification } from '../contexts/NotificationContext';

const AdminMaterialConfig = () => {
    const [configs, setConfigs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const { notify } = useNotification();
    const [editingConfig, setEditingConfig] = useState(null);

    const loadConfigs = async () => {
        setLoading(true);
        try {
            const data = await clientService.getAdminConfigs();
            setConfigs(data);
        } catch (err) {
            notify('Failed to load configurations', 'error');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadConfigs();
    }, []);

    const handleSave = async (config) => {
        try {
            await clientService.saveAdminConfig(config);
            notify('Configuration updated successfully', 'success');
            setEditingConfig(null);
            loadConfigs();
        } catch (err) {
            notify('Update failed', 'error');
        }
    };

    const filteredConfigs = configs.filter(c =>
        c.entityName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        c.columnName.toLowerCase().includes(searchTerm.toLowerCase())
    );

    // Group by entity
    const groupedConfigs = filteredConfigs.reduce((acc, config) => {
        if (!acc[config.entityName]) acc[config.entityName] = [];
        acc[config.entityName].push(config);
        return acc;
    }, {});

    const getBadgeClass = (category) => {
        switch (category) {
            case 'RISK': return 'suspended';
            case 'SCREENING': return 'pending';
            case 'BOTH': return 'active';
            case 'NONE': return 'inactive';
            default: return '';
        }
    };

    if (loading) return <p className="loading">Loading configurations...</p>;

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
            <div className="glass-section" style={{ padding: '2rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem' }}>
                    <div>
                        <h2 style={{ margin: 0, fontSize: '1.8rem', background: 'linear-gradient(to right, #fff, #888)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                            Categorization Rules
                        </h2>
                        <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem', maxWidth: '600px' }}>
                            Configure how client data modifications are prioritized. These rules drive automated screening and risk re-assessment triggers during ingestion.
                        </p>
                    </div>
                    <div className="search-box" style={{ width: '300px' }}>
                        <input
                            type="text"
                            placeholder="Search fields or entities..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="btn btn-secondary"
                            style={{ width: '100%', textAlign: 'left', cursor: 'text', padding: '0.6rem 1rem' }}
                        />
                    </div>
                </div>

                {Object.keys(groupedConfigs).length === 0 ? (
                    <p style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: '3rem' }}>
                        No configuration rules match your search.
                    </p>
                ) : (
                    Object.entries(groupedConfigs).map(([entity, entityConfigs]) => (
                        <div key={entity} style={{ marginBottom: '2.5rem' }}>
                            <h4 style={{
                                color: 'var(--accent-primary)',
                                borderBottom: '1px solid rgba(255,255,255,0.1)',
                                paddingBottom: '0.5rem',
                                marginBottom: '1rem',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '10px'
                            }}>
                                <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--accent-primary)' }}></span>
                                {entity} Entity
                            </h4>
                            <div className="rules-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '1rem' }}>
                                {entityConfigs.map((config) => (
                                    <div
                                        key={config.columnName}
                                        className="glass-section"
                                        style={{
                                            background: 'rgba(255,255,255,0.03)',
                                            border: '1px solid rgba(255,255,255,0.05)',
                                            padding: '1rem',
                                            transition: 'all 0.3s ease'
                                        }}
                                    >
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                            <div>
                                                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px' }}>Field</div>
                                                <div style={{ fontSize: '1.1rem', fontWeight: 'bold' }}>{config.columnName}</div>
                                            </div>

                                            {editingConfig && editingConfig.entityName === config.entityName && editingConfig.columnName === config.columnName ? (
                                                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', alignItems: 'flex-end' }}>
                                                    <select
                                                        value={editingConfig.category}
                                                        onChange={(e) => setEditingConfig({ ...editingConfig, category: e.target.value })}
                                                        style={{
                                                            background: '#1a1a1a',
                                                            color: 'white',
                                                            border: '1px solid #444',
                                                            borderRadius: '4px',
                                                            padding: '4px 8px',
                                                            fontSize: '0.8rem'
                                                        }}
                                                    >
                                                        <option value="SCREENING">SCREENING</option>
                                                        <option value="RISK">RISK</option>
                                                        <option value="BOTH">BOTH</option>
                                                        <option value="NONE">NONE</option>
                                                    </select>
                                                    <div style={{ display: 'flex', gap: '5px' }}>
                                                        <Button variant="secondary" onClick={() => handleSave(editingConfig)} style={{ padding: '0.2rem 0.6rem', fontSize: '0.7rem', background: 'rgba(82, 196, 26, 0.2)' }}>Save</Button>
                                                        <Button variant="secondary" onClick={() => setEditingConfig(null)} style={{ padding: '0.2rem 0.6rem', fontSize: '0.7rem' }}>Cancel</Button>
                                                    </div>
                                                </div>
                                            ) : (
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                                                    <span className={`status-badge ${getBadgeClass(config.category)}`} style={{ minWidth: '80px', textAlign: 'center' }}>
                                                        {config.category}
                                                    </span>
                                                    <button
                                                        onClick={() => setEditingConfig(config)}
                                                        style={{
                                                            background: 'none',
                                                            border: 'none',
                                                            color: 'var(--text-secondary)',
                                                            cursor: 'pointer',
                                                            padding: '5px'
                                                        }}
                                                        title="Edit Rule"
                                                    >
                                                        ⚙️
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                        <div style={{ marginTop: '0.8rem', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                                            {config.category === 'SCREENING' && "Triggers AML/Sanctions rescreening."}
                                            {config.category === 'RISK' && "Triggers risk score recalculation."}
                                            {config.category === 'BOTH' && "Triggers both screening and risk updates."}
                                            {config.category === 'NONE' && "Modification is logged but no downstream action taken."}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))
                )}
            </div>

            <div className="glass-section" style={{ padding: '1.5rem', background: 'rgba(246, 211, 101, 0.05)', border: '1px solid rgba(246, 211, 101, 0.2)' }}>
                <h5 style={{ margin: '0 0 0.5rem 0', color: '#f6d365' }}>Admin Guidance</h5>
                <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    High-impact fields (like Citizenship or Identity Numbers) should generally be set to <strong>BOTH</strong> or <strong>SCREENING</strong>.
                    Operational fields (like Language or Occupation) typically fall under <strong>RISK</strong> or <strong>NONE</strong> depending on your policy.
                </p>
            </div>
        </div>
    );
};

export default AdminMaterialConfig;
