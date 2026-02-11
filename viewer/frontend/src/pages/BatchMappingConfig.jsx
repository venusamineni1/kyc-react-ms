import React, { useState, useEffect } from 'react';
import apiClient from '../services/apiClient';
import { useNotification } from '../contexts/NotificationContext';
import './BatchMappingConfig.css';

const TARGET_PATHS = [
    // Header/Meta
    { path: 'record.uniRcrdId', label: 'Unique Record ID', category: 'Metadata', description: 'The unique identifier for the record.' },
    { path: 'record.type', label: 'Record Type', category: 'Metadata', description: 'Type of record (P for Individual, C for Corporate).' },
    { path: 'record.recStat', label: 'Record Status', category: 'Metadata', description: 'Status (e.g., M for Main).' },

    // Names
    { path: 'name.full', label: 'Full Name', category: 'Personal Info', description: 'Individual\'s full name.' },
    { path: 'name.tit', label: 'Title', category: 'Personal Info', description: 'Individual\'s title (e.g., Mr, Ms).' },
    { path: 'name.fir', label: 'First Name', category: 'Personal Info', description: 'Individual\'s given name.' },
    { path: 'name.mid', label: 'Middle Name', category: 'Personal Info', description: 'Individual\'s middle name.' },
    { path: 'name.sur', label: 'Surname', category: 'Personal Info', description: 'Individual\'s last name.' },

    // Individual Details
    { path: 'individual.gender', label: 'Gender', category: 'Details', description: 'Gender of the individual.' },
    { path: 'individual.dob', label: 'Date of Birth', category: 'Details', description: 'Date of birth (YYYY-MM-DD).' },
    { path: 'individual.placeOfBirth', label: 'Place of Birth', category: 'Details', description: 'City/Town of birth.' },
    { path: 'individual.cntr', label: 'Country of Birth', category: 'Details', description: 'Country of birth code.' },
    { path: 'individual.occupation', label: 'Occupation', category: 'Details', description: 'Individual\'s primary occupation.' },

    // Nationality & Address
    { path: 'individual.nationality', label: 'Primary Nationality (Country)', category: 'Regional', description: 'Primary citizenship code.' },
    { path: 'individual.nationality.legDoc', label: 'Legitimisation Doc', category: 'Regional', description: 'Type of ID (e.g., Passport).' },
    { path: 'individual.nationality.idNr', label: 'ID Number', category: 'Regional', description: 'ID document number.' },

    { path: 'individual.address', label: 'Address Line 1', category: 'Regional', description: 'Primary residential street address.' },
    { path: 'individual.address.line', label: 'Address Line 1 (Alias)', category: 'Regional', description: 'Same as Address Line 1.' },
    { path: 'individual.address.city', label: 'City', category: 'Regional', description: 'City name.' },
    { path: 'individual.address.zip', label: 'Zip Code', category: 'Regional', description: 'Postal/Zip code.' },
    { path: 'individual.address.prov', label: 'Province/State', category: 'Regional', description: 'State or Province.' },
    { path: 'individual.address.cntr', label: 'Country', category: 'Regional', description: 'Country code.' },

    // Others
    { path: 'account.nr', label: 'Account Number', category: 'Account Info', description: 'Associated account identifier.' },
    { path: 'kyc.pepFlag', label: 'PEP Flag', category: 'KYC', description: 'Politically Exposed Person flag (Y/N).' },
    { path: 'comment', label: 'Record Comment', category: 'Other', description: 'Any additional internal comments.' }
];

const SOURCE_FIELDS = [
    { value: 'clientID', label: 'Client ID' },
    { value: 'firstName', label: 'First Name' },
    { value: 'middleName', label: 'Middle Name' },
    { value: 'lastName', label: 'Last Name' },
    { value: 'fullName', label: 'Full Name (Auto-joined)' },
    { value: 'gender', label: 'Gender' },
    { value: 'dateOfBirth', label: 'Date of Birth' },
    { value: 'citizenship1', label: 'Citizenship 1' },
    { value: 'citizenship2', label: 'Citizenship 2' },
    { value: 'occupation', label: 'Occupation' },
    { value: 'language', label: 'Language' },
    { value: 'countryOfTax', label: 'Country of Tax' },
    { value: 'sourceOfFundsCountry', label: 'Source of Funds Country' },
    { value: 'fatcaStatus', label: 'FATCA Status' },
    { value: 'crsStatus', label: 'CRS Status' },
    { value: 'status', label: 'Client Status' }
];

const BatchMappingConfig = () => {
    const [mappings, setMappings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const { notify } = useNotification();

    // Test Feature State
    const [clients, setClients] = useState([]);
    const [selectedClient, setSelectedClient] = useState('');
    const [testXml, setTestXml] = useState('');
    const [generating, setGenerating] = useState(false);

    const [collapsedCategories, setCollapsedCategories] = useState({});

    const toggleCategory = (category) => {
        setCollapsedCategories(prev => ({
            ...prev,
            [category]: !prev[category]
        }));
    };

    useEffect(() => {
        fetchMappings();
        fetchClients();
    }, []);

    const fetchMappings = async () => {
        try {
            const data = await apiClient.get('/screening/batch/mapping');
            const initialMappings = TARGET_PATHS.map(tp => {
                const existing = data.find(d => d.targetPath === tp.path);
                return existing || { targetPath: tp.path, sourceField: '', defaultValue: '', transformation: '' };
            });
            setMappings(initialMappings);
        } catch (error) {
            notify('Failed to load mapping configuration', 'error');
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    const fetchClients = async () => {
        try {
            const response = await apiClient.get('/clients?size=100');
            // Assuming paginated response with .content
            setClients(response.content || response);
        } catch (error) {
            console.error("Failed to load clients for testing", error);
        }
    };

    const handleGenerateTest = async () => {
        if (!selectedClient) return;
        setGenerating(true);
        setTestXml('');
        try {
            const client = clients.find(c => c.clientID === parseInt(selectedClient));
            if (!client) throw new Error("Client not found");

            // Clean client object to match expected input if necessary, 
            // but the backend should handle extra fields gracefully via JSON serialization
            const response = await apiClient.post('/screening/batch/test-generate', client);
            setTestXml(response);
            notify('Test XML generated', 'success');
        } catch (error) {
            notify('Failed to generate test XML', 'error');
            console.error(error);
        } finally {
            setGenerating(false);
        }
    };

    const handleMappingChange = (path, field, value) => {
        setMappings(prev => prev.map(m =>
            m.targetPath === path ? { ...m, [field]: value } : m
        ));
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            await apiClient.post('/screening/batch/mapping', mappings);
            notify('Mapping configuration saved successfully', 'success');
        } catch (error) {
            notify('Failed to save mapping configuration', 'error');
            console.error(error);
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return <div className="loading-state">Loading configuration...</div>;
    }



    // Group target paths by category
    const categories = [...new Set(TARGET_PATHS.map(tp => tp.category))];

    return (
        <div className="container batch-mapping-container">
            {/* Header */}
            <div className="page-header">
                <div className="page-title">
                    <h1>Batch Screening Configuration</h1>
                    <p>Map internal client data attributes to the external screening XML schema.</p>
                </div>
                <div className="header-actions">
                    <span className="sync-status">Last synced: Just now</span>
                    <button
                        onClick={handleSave}
                        disabled={saving}
                        className="btn"
                    >
                        {saving ? (
                            <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <svg className="animate-spin" style={{ width: '1em', height: '1em' }} xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                                Saving...
                            </span>
                        ) : 'Save Configuration'}
                    </button>
                </div>
            </div>

            {/* Test Configuration */}
            <div className="category-section test-section" style={{ border: '1px solid rgba(56, 189, 248, 0.3)', background: 'rgba(56, 189, 248, 0.02)' }}>
                <div className="category-header">
                    <h2 className="category-title" style={{ color: 'var(--primary-color)' }}>Test Configuration</h2>
                </div>
                <div className="mapping-card">
                    <div className="field-info">
                        <div className="field-label">Generate Test XML</div>
                        <div className="field-desc">Select a client to preview the generated XML based on current mappings.</div>
                    </div>
                    <div className="mapping-controls" style={{ display: 'flex', flexDirection: 'row', gap: '1rem', alignItems: 'flex-end', justifyContent: 'flex-start' }}>
                        <div className="control-group form-group" style={{ width: '45%', flex: 'none' }}>
                            <label className="control-label" style={{ display: 'block', marginBottom: '0.35rem', fontSize: '0.7rem', opacity: 0.8, fontWeight: 700, textTransform: 'uppercase' }}>Select Test Client</label>
                            <div style={{ position: 'relative' }}>
                                <select
                                    value={selectedClient}
                                    onChange={(e) => setSelectedClient(e.target.value)}
                                    style={{
                                        width: '100%',
                                        padding: '0 0.5rem',
                                        height: '32px',
                                        lineHeight: '32px',
                                        background: 'rgba(56, 189, 248, 0.05)',
                                        border: '1px solid rgba(56, 189, 248, 0.2)',
                                        borderRadius: '4px',
                                        color: '#e2e8f0',
                                        appearance: 'none',
                                        whiteSpace: 'nowrap',
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis'
                                    }}
                                >
                                    <option value="">-- Select Client --</option>
                                    {clients.map(c => (
                                        <option key={c.clientID} value={c.clientID}>
                                            {c.firstName} {c.lastName} (ID: {c.clientID})
                                        </option>
                                    ))}
                                </select>
                                <div style={{ position: 'absolute', right: '10px', top: '50%', transform: 'translateY(-50%)', pointerEvents: 'none', opacity: 0.7 }}>
                                    <svg width="10" height="6" viewBox="0 0 10 6" fill="none" stroke="currentColor" strokeWidth="1.5"><path d="M1 1L5 5L9 1" /></svg>
                                </div>
                            </div>
                        </div>
                        <div className="control-group" style={{ width: 'auto', flex: 'none' }}>
                            <button
                                className="btn"
                                onClick={handleGenerateTest}
                                disabled={!selectedClient || generating}
                                style={{ height: '32px', padding: '0 1rem', whiteSpace: 'nowrap', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                            >
                                {generating ? 'Generating...' : 'Generate XML'}
                            </button>
                        </div>
                    </div>
                </div>
                {testXml && (
                    <div className="xml-preview-container">
                        <label className="control-label mb-2 block">Generated XML Preview</label>
                        <pre className="xml-preview-box">
                            {testXml}
                        </pre>
                    </div>
                )}
            </div>

            <div className="mapping-content">
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
                                                        {/* Custom Arrow */}
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

            {/* Info Footer */}
            <div className="info-footer">
                <div className="info-item">
                    <div className="info-icon">
                        <svg width="24" height="24" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                    </div>
                    <div className="info-content">
                        <h4>Mapping Logic</h4>
                        <p>The system prioritizes the <strong>Source Attribute</strong>. If the source data is null, it falls back to the <strong>Default Value</strong>.</p>
                    </div>
                </div>
                <div className="info-divider"></div>
                <div className="info-item">
                    <div className="info-content">
                        <h4>Auto-Calculations</h4>
                        <p>Checksums (SHA256) are automatically generated for all records. No manual configuration required.</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default BatchMappingConfig;
