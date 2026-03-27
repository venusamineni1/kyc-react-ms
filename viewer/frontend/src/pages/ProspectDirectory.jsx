import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { prospectService } from '../services/prospectService';
import Pagination from '../components/Pagination';
import Button from '../components/Button';
import Modal from '../components/Modal';
import { useNotification } from '../contexts/NotificationContext';

const ProspectDirectory = () => {
    const { notify } = useNotification();
    const navigate = useNavigate();
    const [prospects, setProspects] = useState([]);
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [page, setPage] = useState(0);

    // Modal state
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [formData, setFormData] = useState({
        firstName: '', lastName: '', dateOfBirth: '',
        citizenship1: '', placeOfBirth: '',
        countryOfTax: '', occupation: '',
        addressLine1: '', city: '', country: '', addressType: 'RESIDENTIAL'
    });
    const [documents, setDocuments] = useState([]); // Array of { file: null, type: 'ID_DOCUMENT', comment: '' }
    const [submitting, setSubmitting] = useState(false);

    const handleAddDocument = () => setDocuments([...documents, { file: null, type: 'ID_DOCUMENT', comment: '' }]);
    const handleRemoveDocument = (idx) => { const nd = [...documents]; nd.splice(idx, 1); setDocuments(nd); };
    const handleDocumentChange = (idx, field, val) => { const nd = [...documents]; nd[idx][field] = val; setDocuments(nd); };

    const loadProspects = async (p = 0) => {
        setLoading(true);
        try {
            const result = await prospectService.getProspects(p);
            setProspects(result.content);
            setData(result);
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadProspects(page);
    }, [page]);

    const handleCreateProspect = async () => {
        if (!formData.firstName || !formData.lastName) {
            notify('First Name and Last Name are required', 'warning');
            return;
        }
        setSubmitting(true);
        try {
            const payload = new FormData();
            const { addressLine1, city, country, addressType, ...clientRoot } = formData;
            const clientData = {
                ...clientRoot,
                addresses: addressLine1 ? [{
                    addressType,
                    addressLine1,
                    city,
                    country
                }] : []
            };
            
            payload.append('client', JSON.stringify(clientData));
            
            documents.forEach(doc => {
                if (doc.file) {
                    payload.append('documents', doc.file);
                    payload.append('documentTypes', doc.type || 'ONBOARDING');
                    payload.append('documentComments', doc.comment || '');
                }
            });

            const newProspect = await prospectService.onboardProspect(payload);
            
            if (newProspect && newProspect.status === 'APPROVED') {
                notify('Prospect automatically approved and onboarded!', 'success');
            } else if (newProspect && newProspect.status === 'REJECTED') {
                notify('Prospect rejected by risk evaluation algorithms.', 'error');
            } else {
                notify('Prospect submitted! Pipeline paused for manual review check.', 'warning');
            }
            setIsModalOpen(false);
            setFormData({
                firstName: '', lastName: '', dateOfBirth: '',
                citizenship1: '', placeOfBirth: '',
                countryOfTax: '', occupation: '',
                addressLine1: '', city: '', country: '', addressType: 'RESIDENTIAL'
            });
            setDocuments([]);
            loadProspects(0);
        } catch (err) {
            notify('Failed to create prospect: ' + err.message, 'error');
        } finally {
            setSubmitting(false);
        }
    };


    const getStatusBadgeStyle = (status) => {
        switch (status) {
            case 'NEW': return { backgroundColor: 'rgba(59, 130, 246, 0.2)', color: '#3b82f6', border: '1px solid rgba(59, 130, 246, 0.3)' };
            case 'SCREENING_IN_PROGRESS': return { backgroundColor: 'rgba(139, 92, 246, 0.2)', color: '#8b5cf6', border: '1px solid rgba(139, 92, 246, 0.3)' };
            case 'RISK_EVALUATION_IN_PROGRESS': return { backgroundColor: 'rgba(236, 72, 153, 0.2)', color: '#ec4899', border: '1px solid rgba(236, 72, 153, 0.3)' };
            case 'IN_REVIEW': return { backgroundColor: 'rgba(245, 158, 11, 0.2)', color: '#fbbf24', border: '1px solid rgba(245, 158, 11, 0.3)' };
            default: return { backgroundColor: 'rgba(107, 114, 128, 0.2)', color: '#9ca3af', border: '1px solid rgba(107, 114, 128, 0.3)' };
        }
    };

    return (
        <div className="client-directory-page">
            <div style={{ marginBottom: '2rem' }}>
                <div className="page-header" style={{ alignItems: 'flex-start', borderBottom: 'none', paddingBottom: '1rem', marginBottom: '1.5rem' }}>
                    <div>
                        <h1 style={{ fontSize: '2rem', margin: '0 0 0.5rem 0', background: 'var(--header-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                            New Client Onboarding
                        </h1>
                        <p style={{ color: 'var(--text-secondary)', margin: 0 }}>Manage prospects and active onboarding workflows</p>
                    </div>
                    <Button onClick={() => setIsModalOpen(true)}>
                        + Onboard Client
                    </Button>
                </div>
            </div>

            <div className="glass-section" style={{ padding: '0' }}>
                <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h3 style={{ margin: 0 }}>Active Prospects</h3>
                </div>

                <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                            <tr style={{ background: 'var(--hover-bg)' }}>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>ID</th>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>Name</th>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>Start Date</th>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: '500' }}>Journey Status</th>
                                <th style={{ padding: '1.25rem 1.5rem', textAlign: 'right', color: 'var(--text-secondary)', fontWeight: '500' }}>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading && !prospects.length ? (
                                <tr>
                                    <td colSpan="5" style={{ padding: '4rem', textAlign: 'center' }}>
                                        <div className="loading-spinner">Loading prospects...</div>
                                    </td>
                                </tr>
                            ) : error ? (
                                <tr>
                                    <td colSpan="5" style={{ padding: '4rem', textAlign: 'center', color: '#ef4444' }}>
                                        ⚠️ Error: {error}
                                    </td>
                                </tr>
                            ) : prospects.length === 0 ? (
                                <tr>
                                    <td colSpan="5" style={{ padding: '4rem', textAlign: 'center', color: '#64748b' }}>
                                        No active onboardings right now.
                                    </td>
                                </tr>
                            ) : (
                                prospects.map((prospect) => (
                                    <tr key={prospect.clientID} className="table-row-hover" style={{ borderBottom: '1px solid var(--glass-border)' }}>
                                        <td style={{ padding: '1.25rem 1.5rem', fontWeight: '600', color: '#3b82f6' }}>#{prospect.clientID}</td>
                                        <td style={{ padding: '1.25rem 1.5rem', fontWeight: '500' }}>{prospect.firstName} {prospect.lastName}</td>
                                        <td style={{ padding: '1.25rem 1.5rem', color: 'var(--text-secondary)' }}>{new Date(prospect.onboardingDate).toLocaleDateString()}</td>
                                        <td style={{ padding: '1.25rem 1.5rem' }}>
                                            <span style={{
                                                padding: '0.25rem 0.75rem', borderRadius: '20px', fontSize: '0.75rem', fontWeight: '600',
                                                ...getStatusBadgeStyle(prospect.status)
                                            }}>
                                                {prospect.status.replace(/_/g, ' ')}
                                            </span>
                                        </td>
                                        <td style={{ padding: '1.25rem 1.5rem', textAlign: 'right' }}>
                                            <Link to={`/clients/${prospect.clientID}`}>
                                                <Button variant="secondary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.875rem' }}>View Details</Button>
                                            </Link>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
                {data && <div style={{ padding: '1.5rem', borderTop: '1px solid var(--glass-border)' }}>
                    <Pagination data={data} onPageChange={(p) => setPage(p)} />
                </div>}
            </div>

            <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title="Create Prospect Onboarding" maxWidth="800px" closeOnOutsideClick={false}>
                <div style={{ display: 'flex', flexDirection: 'column', maxHeight: '75vh' }}>
                    <div style={{ overflowY: 'auto', paddingRight: '0.5rem', paddingBottom: '1rem', flex: 1, display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <label className="form-label" style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>First Name</label>
                            <input className="modern-input" type="text" value={formData.firstName} onChange={e => setFormData({ ...formData, firstName: e.target.value })} style={{ width: '100%', padding: '0.8rem', background: 'var(--glass-bg)', color: '#fff', border: '1px solid var(--glass-border)', borderRadius: '8px', boxSizing: 'border-box' }} />
                        </div>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <label className="form-label" style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Last Name</label>
                            <input className="modern-input" type="text" value={formData.lastName} onChange={e => setFormData({ ...formData, lastName: e.target.value })} style={{ width: '100%', padding: '0.8rem', background: 'var(--glass-bg)', color: '#fff', border: '1px solid var(--glass-border)', borderRadius: '8px', boxSizing: 'border-box' }} />
                        </div>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <label className="form-label" style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Date of Birth</label>
                            <input className="modern-input" type="date" value={formData.dateOfBirth} onChange={e => setFormData({ ...formData, dateOfBirth: e.target.value })} style={{ width: '100%', padding: '0.8rem', background: 'var(--glass-bg)', color: '#fff', border: '1px solid var(--glass-border)', borderRadius: '8px', boxSizing: 'border-box' }} />
                        </div>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <label className="form-label" style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Citizenship</label>
                            <input className="modern-input" type="text" value={formData.citizenship1} onChange={e => setFormData({ ...formData, citizenship1: e.target.value })} style={{ width: '100%', padding: '0.8rem', background: 'var(--glass-bg)', color: '#fff', border: '1px solid var(--glass-border)', borderRadius: '8px', boxSizing: 'border-box' }} />
                        </div>
                    </div>

                    <div style={{ marginTop: '1rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                            <h4 style={{ margin: 0, color: 'var(--text-primary)' }}>Addresses</h4>
                        </div>
                        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr', gap: '1.5rem' }}>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                <label className="form-label" style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Address Line 1</label>
                                <input className="modern-input" type="text" value={formData.addressLine1} onChange={e => setFormData({ ...formData, addressLine1: e.target.value })} style={{ width: '100%', padding: '0.8rem', background: 'var(--glass-bg)', color: '#fff', border: '1px solid var(--glass-border)', borderRadius: '8px', boxSizing: 'border-box' }} />
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                <label className="form-label" style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>City</label>
                                <input className="modern-input" type="text" value={formData.city} onChange={e => setFormData({ ...formData, city: e.target.value })} style={{ width: '100%', padding: '0.8rem', background: 'var(--glass-bg)', color: '#fff', border: '1px solid var(--glass-border)', borderRadius: '8px', boxSizing: 'border-box' }} />
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                <label className="form-label" style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Country</label>
                                <input className="modern-input" type="text" value={formData.country} onChange={e => setFormData({ ...formData, country: e.target.value })} style={{ width: '100%', padding: '0.8rem', background: 'var(--glass-bg)', color: '#fff', border: '1px solid var(--glass-border)', borderRadius: '8px', boxSizing: 'border-box' }} />
                            </div>
                        </div>
                    </div>

                    <div style={{ marginTop: '1rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                            <h4 style={{ margin: 0, color: 'var(--text-primary)' }}>Identification Documents</h4>
                            <Button variant="secondary" onClick={handleAddDocument} style={{ fontSize: '0.75rem', padding: '0.4rem 0.8rem' }}>+ Add Document</Button>
                        </div>
                        {documents.map((doc, index) => (
                            <div key={index} style={{ padding: '1rem', background: 'rgba(255,255,255,0.03)', borderRadius: '8px', marginBottom: '1rem', border: '1px solid var(--glass-border)' }}>
                                <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '0.5rem' }}>
                                    <button onClick={() => handleRemoveDocument(index)} style={{ background: 'transparent', border: 'none', color: '#ff4d4f', cursor: 'pointer', fontSize: '1.2rem', padding: 0, lineHeight: 1 }}>×</button>
                                </div>
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '1rem' }}>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                        <label className="form-label" style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Type</label>
                                        <select value={doc.type} onChange={(e) => handleDocumentChange(index, 'type', e.target.value)} style={{ width: '100%', padding: '0.8rem', background: 'var(--glass-bg)', color: '#fff', border: '1px solid var(--glass-border)', borderRadius: '8px', boxSizing: 'border-box' }}>
                                            <option value="ID_DOCUMENT">ID Document</option>
                                            <option value="PASSPORT">Passport</option>
                                            <option value="PROOF_OF_ADDRESS">Proof of Address</option>
                                            <option value="COMPANY_REGISTRY">Company Registry</option>
                                            <option value="UTILITY_BILL">Utility Bill</option>
                                            <option value="OTHER">Other</option>
                                        </select>
                                    </div>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                        <label className="form-label" style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>File</label>
                                        <input className="modern-input" type="file" onChange={(e) => handleDocumentChange(index, 'file', e.target.files[0])} style={{ width: '100%', padding: '0.8rem', background: 'var(--glass-bg)', color: '#fff', border: '1px solid var(--glass-border)', borderRadius: '8px', boxSizing: 'border-box' }} />
                                    </div>
                                </div>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                    <label className="form-label" style={{ display: 'block', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Comment</label>
                                    <input className="modern-input" type="text" placeholder="Optional comments..." value={doc.comment} onChange={(e) => handleDocumentChange(index, 'comment', e.target.value)} style={{ width: '100%', padding: '0.8rem', background: 'var(--glass-bg)', color: '#fff', border: '1px solid var(--glass-border)', borderRadius: '8px', boxSizing: 'border-box' }} />
                                </div>
                            </div>
                        ))}
                    </div>

                    </div>
                    <div style={{ display: 'flex', gap: '1rem', paddingTop: '1.5rem', borderTop: '1px solid var(--glass-border)', marginTop: 'auto' }}>
                        <Button onClick={handleCreateProspect} disabled={submitting} style={{ flex: 1 }}>{submitting ? 'Processing Pipeline...' : 'Start Onboarding'}</Button>
                        <Button variant="secondary" onClick={() => setIsModalOpen(false)}>Cancel</Button>
                    </div>
                </div>
            </Modal>
        </div>
    );
};

export default ProspectDirectory;
