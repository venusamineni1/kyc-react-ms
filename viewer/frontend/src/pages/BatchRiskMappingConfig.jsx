import React, { useState, useEffect, useCallback } from 'react';
import apiClient from '../services/apiClient';
import './BatchMappingConfig.css';
import { useNotification } from '../contexts/NotificationContext';

const API_BASE = '/risk/batch';
const CLIENTS_API = '/clients'; // Assuming this exists or using a mock

const SOURCE_FIELDS = [
    { label: 'Client ID', value: 'clientID' },
    { label: 'First Name', value: 'firstName' },
    { label: 'Middle Name', value: 'middleName' },
    { label: 'Last Name', value: 'lastName' },
    { label: 'Citizenship 1', value: 'citizenship1' },
    { label: 'Citizenship 2', value: 'citizenship2' },
    { label: 'Gender', value: 'gender' },
    { label: 'Date of Birth', value: 'dateOfBirth' },
    { label: 'Occupation', value: 'occupation' },
    { label: 'Country of Tax', value: 'countryOfTax' },
    { label: 'Source of Funds Country', value: 'sourceOfFundsCountry' },
    { label: 'Address Line 1', value: 'addressLine1' },
    { label: 'City', value: 'city' },
    { label: 'Zip Code', value: 'zipCode' },
    { label: 'Province', value: 'province' },
    { label: 'Country', value: 'country' },
    { label: 'Nationality', value: 'nationality' },
    { label: 'Legal Doc Type', value: 'legDocType' },
    { label: 'ID Number', value: 'idNumber' }
];

const TARGET_PATHS = [
    { path: 'clientDetails.defenceRevenue', label: 'Defence Revenue', description: 'Annual revenue from defence sector (if applicable)', category: 'CLIENT DETAILS' },
    { path: 'clientDetails.recordID', label: 'Unique Record ID', description: 'Internal identifier for the client record', category: 'CLIENT DETAILS' },
    { path: 'clientDetails.clientAdoptionCountry', label: 'Client Adoption Country', description: 'Country code where the client was adopted (e.g., DE)', category: 'CLIENT DETAILS' },

    { path: 'entityRiskType.typeKYCLegalEntityCode', label: 'Legal Entity Code', description: 'KYC Legal Entity classification code', category: 'ENTITY RISK' },

    { path: 'industryRiskType.industryCode', label: 'Industry Code', description: 'Primary industry classification code', category: 'INDUSTRY RISK' },
    { path: 'industryRiskType.codeType', label: 'Code Type', description: 'Type of industry code used', category: 'INDUSTRY RISK' },

    { path: 'geoRiskType.clientCountryReg', label: 'Client Country Registration', description: 'Country of registration', category: 'GEO RISK' },
    { path: 'geoRiskType.countryOfOp', label: 'Country of Operation', description: 'Prinicipal country of operation', category: 'GEO RISK' },
    { path: 'geoRiskType.originOfFunds', label: 'Origin of Funds', description: 'Primary country for origin of funds', category: 'GEO RISK' },
    { path: 'geoRiskType.addressType.clientDomicile', label: 'Client Domicile', description: 'Country of domicile', category: 'GEO RISK' },

    { path: 'productRiskType.productCode', label: 'Product Code', description: 'Primary product or service code', category: 'PRODUCT RISK' },

    { path: 'channelRiskType.channelCode', label: 'Channel Code', description: 'Distribution or service channel code', category: 'CHANNEL RISK' }
];

const categories = [...new Set(TARGET_PATHS.map(tp => tp.category))];

const BatchRiskMappingConfig = () => {
    const [mappings, setMappings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [lastSync, setLastSync] = useState(null);
    const [collapsedCategories, setCollapsedCategories] = useState({});
    const { notify } = useNotification();

    // Test Generation State
    const [clients, setClients] = useState([]);
    const [selectedClient, setSelectedClient] = useState('');
    const [generating, setGenerating] = useState(false);
    const [generatedJson, setGeneratedJson] = useState('');

    useEffect(() => {
        fetchMappings();
        fetchClients();
    }, []);

    const fetchMappings = async () => {
        try {
            const response = await apiClient.get(`${API_BASE}/mapping`);
            setMappings(response || []);
            setLastSync(new Date().toLocaleTimeString());
        } catch (error) {
            console.error('Error fetching mappings:', error);
            notify('Failed to load risk mapping configuration', 'error');
        } finally {
            setLoading(false);
        }
    };

    const fetchClients = async () => {
        try {
            const response = await apiClient.get(CLIENTS_API);
            const clientsList = Array.isArray(response) ? response : (response.content || []);
            setClients(clientsList);
        } catch (error) {
            console.error('Error fetching clients:', error);
        }
    };

    const handleMappingChange = (targetPath, field, value) => {
        setMappings(prev => {
            const existing = prev.find(m => m.targetPath === targetPath);
            if (existing) {
                return prev.map(m => m.targetPath === targetPath ? { ...m, [field]: value } : m);
            } else {
                const category = TARGET_PATHS.find(tp => tp.path === targetPath)?.category;
                return [...prev, { targetPath, [field]: value, category }];
            }
        });
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            const payload = mappings.filter(m => m.sourceField || m.defaultValue);
            await apiClient.post(`${API_BASE}/mapping`, payload);
            setLastSync(new Date().toLocaleTimeString());
            notify('Risk mapping configuration saved successfully', 'success');
        } catch (error) {
            console.error('Error saving mappings:', error);
            notify('Failed to save risk mapping configuration', 'error');
        } finally {
            setSaving(false);
        }
    };

    const handleGenerateTest = async () => {
        if (!selectedClient) return;
        setGenerating(true);
        setGeneratedJson('');
        try {
            const client = clients.find(c => c.clientID === parseInt(selectedClient));
            const response = await apiClient.post(`${API_BASE}/test-generate`, client);
            setGeneratedJson(typeof response === 'string' ? response : JSON.stringify(response, null, 2));
            notify('Test JSON generated successfully', 'success');
        } catch (error) {
            console.error('Error generating test JSON:', error);
            notify('Failed to generate test JSON', 'error');
        } finally {
            setGenerating(false);
        }
    };

    const toggleCategory = (category) => {
        setCollapsedCategories(prev => ({
            ...prev,
            [category]: !prev[category]
        }));
    };

    if (loading) {
        return <div className="loading-state">Loading risk mapping configuration...</div>;
    }

    return (
        <div className="batch-mapping-container">
            <header className="page-header">
                <div className="page-title">
                    <h1>Risk Batch Mapping</h1>
                    <p>Configure dynamic data mappings for automated Risk assessments (JSON)</p>
                </div>
                <div className="header-actions">
                    {lastSync && <span className="sync-status">Last synced: {lastSync}</span>}
                    <button
                        className={`btn btn-primary ${saving ? 'loading' : ''}`}
                        onClick={handleSave}
                        disabled={saving}
                    >
                        {saving ? 'Saving...' : 'Save Configuration'}
                    </button>
                </div>
            </header>

            <div className="content-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '2rem', alignItems: 'flex-start' }}>
                <div className="mappings-column">
                    <div className="layout-controls" style={{ marginBottom: '1rem', display: 'flex', gap: '1rem' }}>
                        <button
                            className="btn btn-secondary"
                            onClick={() => {
                                const allCollapsed = {};
                                categories.forEach(c => allCollapsed[c] = true);
                                setCollapsedCategories(allCollapsed);
                            }}
                            style={{ padding: '0.25rem 0.75rem', fontSize: '0.85rem' }}
                        >
                            Collapse All
                        </button>
                        <button
                            className="btn btn-secondary"
                            onClick={() => setCollapsedCategories({})}
                            style={{ padding: '0.25rem 0.75rem', fontSize: '0.85rem' }}
                        >
                            Expand All
                        </button>
                    </div>

                    {categories.map(category => (
                        <div key={category} className="category-section">
                            <div
                                className="category-header clickable"
                                onClick={() => toggleCategory(category)}
                                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                            >
                                <div className="header-left" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    <svg
                                        className={`toggle-icon ${collapsedCategories[category] ? 'collapsed' : ''}`}
                                        width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                                        style={{ transition: 'transform 0.2s' }}
                                    >
                                        <polyline points="6 9 12 15 18 9"></polyline>
                                    </svg>
                                    <h2 className="category-title" style={{ margin: 0 }}>{category}</h2>
                                </div>
                                <span className="field-count-badge">
                                    {TARGET_PATHS.filter(t => t.category === category).length} FIELDS
                                </span>
                            </div>

                            {!collapsedCategories[category] && (
                                <div className="category-body" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(400px, 1fr))', gap: '1rem', padding: '1rem' }}>
                                    {TARGET_PATHS.filter(tp => tp.category === category).map((tp) => {
                                        const mapping = mappings.find(m => m.targetPath === tp.path) || {};
                                        return (
                                            <div key={tp.path} className="mapping-card" style={{ display: 'flex', flexDirection: 'column', height: '100%', alignItems: 'stretch' }}>
                                                {/* Field Info */}
                                                <div className="field-info" style={{ marginBottom: '0.75rem' }}>
                                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
                                                        <div className="field-label" style={{ fontWeight: 600, fontSize: '0.95rem' }}>{tp.label}</div>
                                                        <span className="path-badge" style={{ fontSize: '0.65em', padding: '2px 6px', background: 'rgba(255,255,255,0.08)', borderRadius: '4px', fontFamily: 'monospace' }}>{tp.path}</span>
                                                    </div>
                                                    <div className="field-desc" style={{ fontSize: '0.8rem', opacity: 0.7, lineHeight: '1.3' }}>{tp.description}</div>
                                                </div>

                                                {/* Mapping Controls - Compact Row */}
                                                <div className="mapping-controls" style={{ marginTop: 'auto', display: 'flex', flexDirection: 'row', gap: '1rem', alignItems: 'flex-start', justifyContent: 'flex-start' }}>
                                                    <div className="control-group form-group" style={{ width: '45%', flex: 'none', marginBottom: 0, display: 'flex', flexDirection: 'column' }}>
                                                        <label className="control-label" style={{ display: 'block', marginBottom: '0.35rem', fontSize: '0.7rem', opacity: 0.8, fontWeight: 700, textTransform: 'uppercase' }}>Source Attribute</label>
                                                        <div style={{ position: 'relative', width: '100%' }}>
                                                            <select
                                                                value={mapping.sourceField || ''}
                                                                onChange={(e) => handleMappingChange(tp.path, 'sourceField', e.target.value)}
                                                                style={{
                                                                    width: '100%',
                                                                    padding: '0 0.5rem',
                                                                    fontSize: '0.85rem',
                                                                    background: 'rgba(255, 255, 255, 0.03)',
                                                                    border: '1px solid rgba(255, 255, 255, 0.1)',
                                                                    borderRadius: '4px',
                                                                    color: '#e2e8f0',
                                                                    height: '32px',
                                                                    lineHeight: '32px',
                                                                    appearance: 'none',
                                                                    WebkitAppearance: 'none'
                                                                }}
                                                            >
                                                                <option value="">-- None --</option>
                                                                {SOURCE_FIELDS.map(sf => (
                                                                    <option key={sf.value} value={sf.value}>{sf.label}</option>
                                                                ))}
                                                            </select>
                                                            <div style={{ position: 'absolute', right: '8px', top: '50%', transform: 'translateY(-50%)', pointerEvents: 'none', opacity: 0.5 }}>
                                                                <svg width="10" height="6" viewBox="0 0 10 6" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><path d="M1 1L5 5L9 1" /></svg>
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <div className="control-group form-group" style={{ width: '45%', flex: 'none', marginBottom: 0, display: 'flex', flexDirection: 'column' }}>
                                                        <label className="control-label" style={{ display: 'block', marginBottom: '0.35rem', fontSize: '0.7rem', opacity: 0.8, fontWeight: 700, textTransform: 'uppercase' }}>Default Value</label>
                                                        <input
                                                            type="text"
                                                            value={mapping.defaultValue || ''}
                                                            onChange={(e) => handleMappingChange(tp.path, 'defaultValue', e.target.value)}
                                                            placeholder="Constant..."
                                                            style={{
                                                                width: '100%',
                                                                padding: '0 0.5rem',
                                                                fontSize: '0.85rem',
                                                                background: 'rgba(255, 255, 255, 0.03)',
                                                                border: '1px solid rgba(255, 255, 255, 0.1)',
                                                                borderRadius: '4px',
                                                                color: '#e2e8f0',
                                                                height: '32px',
                                                                lineHeight: '32px'
                                                            }}
                                                        />
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    ))}
                </div>

                <div className="preview-column">
                    <div className="category-section" style={{ position: 'sticky', top: '2rem' }}>
                        <div className="category-header">
                            <h2 className="category-title">Test Configuration</h2>
                        </div>
                        <div className="category-body" style={{ padding: '1rem' }}>
                            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
                                Select a client to preview the generated JSON based on current mappings.
                            </p>

                            <div className="mapping-controls" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                <div className="control-group form-group">
                                    <label className="control-label">Select Test Client</label>
                                    <div style={{ position: 'relative' }}>
                                        <select
                                            value={selectedClient}
                                            onChange={(e) => setSelectedClient(e.target.value)}
                                            style={{
                                                width: '100%',
                                                padding: '0 0.5rem',
                                                height: '32px',
                                                background: 'rgba(56, 189, 248, 0.05)',
                                                border: '1px solid rgba(56, 189, 248, 0.2)',
                                                borderRadius: '4px',
                                                color: '#e2e8f0',
                                                appearance: 'none'
                                            }}
                                        >
                                            <option value="">-- Select Client --</option>
                                            {clients.map(c => (
                                                <option key={c.clientID} value={c.clientID}>
                                                    {c.firstName} {c.lastName} (ID: {c.clientID})
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                </div>
                                <button
                                    className="btn btn-primary"
                                    onClick={handleGenerateTest}
                                    disabled={!selectedClient || generating}
                                    style={{ height: '36px', width: '100%' }}
                                >
                                    {generating ? 'Generating...' : 'Generate Test JSON'}
                                </button>
                            </div>

                            {generatedJson && (
                                <div className="json-preview" style={{ marginTop: '1.5rem' }}>
                                    <label className="control-label">Preview YAML/JSON</label>
                                    <pre style={{
                                        padding: '1rem',
                                        background: 'rgba(0,0,0,0.3)',
                                        borderRadius: '8px',
                                        fontSize: '0.75rem',
                                        maxHeight: '400px',
                                        overflow: 'auto',
                                        border: '1px solid var(--glass-border)'
                                    }}>
                                        {generatedJson}
                                    </pre>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            <footer className="info-footer">
                <div className="info-item">
                    <div className="info-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><line x1="12" y1="16" x2="12" y2="12" /><line x1="12" y1="8" x2="12.01" y2="8" /></svg>
                    </div>
                    <div className="info-content">
                        <h4>Mapping Logic</h4>
                        <p>Mappings defined here are used to generate the Risk JSON payload. Source fields map to client record attributes.</p>
                    </div>
                </div>
                <div className="info-divider"></div>
                <div className="info-item">
                    <div className="info-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" /><polyline points="10 9 9 9 8 9" /></svg>
                    </div>
                    <div className="info-content">
                        <h4>JSON Schema</h4>
                        <p>The generated output adheres to the Risk Service JSON schema, supporting nested structures like Geo and Entity risk.</p>
                    </div>
                </div>
            </footer>
        </div>
    );
};

export default BatchRiskMappingConfig;
