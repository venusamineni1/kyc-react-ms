import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { clientService } from '../services/clientService';
import { caseService } from '../services/caseService';
import { riskService } from '../services/riskService';
import Button from '../components/Button';
import Modal from '../components/Modal';
import { useAuth } from '../contexts/AuthContext';
import { useNotification } from '../contexts/NotificationContext';
import Questionnaire from './Questionnaire';
import ScreeningPanel from '../components/ScreeningPanel';

const TabButton = ({ active, label, onClick, icon }) => (
    <button
        onClick={onClick}
        className={`tab-btn ${active ? 'active' : ''}`}
        style={{
            background: active ? 'rgba(255,255,255,0.1)' : 'transparent',
            border: 'none',
            borderBottom: active ? '2px solid var(--accent-primary)' : '2px solid transparent',
            color: active ? 'var(--text-primary)' : 'var(--text-secondary)',
            padding: '1rem 1.5rem',
            cursor: 'pointer',
            fontSize: '0.9rem',
            fontWeight: active ? '600' : '400',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            transition: 'all 0.2s ease'
        }}
    >
        <span>{icon}</span> {label}
    </button>
);

const Section = ({ title, children, actions }) => (
    <section className="glass-section" style={{ marginTop: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
            <h3 style={{ margin: 0, fontSize: '1.2rem', color: 'var(--text-primary)' }}>{title}</h3>
            {actions}
        </div>
        {children}
    </section>
);

const DetailItem = ({ label, value }) => (
    <div className="info-item" style={{ marginBottom: '1rem' }}>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '4px' }}>{label}</div>
        <div style={{ fontSize: '1rem', color: 'var(--text-primary)' }}>{value || '-'}</div>
    </div>
);

const ClientDetails = () => {
    const { id } = useParams();
    const { hasPermission } = useAuth();
    const { notify } = useNotification();
    const navigate = useNavigate();

    const [client, setClient] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeTab, setActiveTab] = useState('overview');

    // Records
    const [cases, setCases] = useState([]);
    const [riskHistory, setRiskHistory] = useState([]);
    const [materialChanges, setMaterialChanges] = useState([]);

    // Modals
    const [isCaseModalOpen, setIsCaseModalOpen] = useState(false);
    const [caseReason, setCaseReason] = useState('Periodic Review');
    const [caseComment, setCaseComment] = useState('');
    const [isPartyModalOpen, setIsPartyModalOpen] = useState(false);
    const [partyData, setPartyData] = useState({ firstName: '', lastName: '', relationType: 'DIRECTOR', status: 'ACTIVE' });
    const [isRiskHistoryOpen, setIsRiskHistoryOpen] = useState(false);

    // UI Helpers
    const [creatingCase, setCreatingCase] = useState(false);
    const [runningAssessment, setRunningAssessment] = useState(false);
    const [viewQuestionnaireCaseId, setViewQuestionnaireCaseId] = useState(null);
    const [selectedAssessment, setSelectedAssessment] = useState(null);
    const [assessmentDetails, setAssessmentDetails] = useState([]);
    const [viewParty, setViewParty] = useState(null);

    const fetchDetails = async () => {
        setLoading(true);
        try {
            const clientData = await clientService.getClientDetails(id);
            setClient(clientData);

            // Parallel fetches
            const [casesData, riskData, changesData] = await Promise.all([
                caseService.getCasesByClient(id).catch(() => []),
                riskService.getRiskHistory(id).catch(() => []),
                clientService.getClientChanges(id).catch(() => [])
            ]);

            setCases(casesData);
            setRiskHistory(riskData);
            setMaterialChanges(changesData);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchDetails();
    }, [id]);

    const handleCreateCase = async () => {
        if (creatingCase) return;
        setCreatingCase(true);
        try {
            const caseId = await caseService.createCase(id, caseReason, caseComment);
            notify('Case created successfully', 'success');
            setIsCaseModalOpen(false);
            setCaseComment('');
            navigate(`/cases/${caseId}`);
        } catch (err) {
            notify('Failed to create case: ' + err.message, 'error');
        } finally {
            setCreatingCase(false);
        }
    };

    const handleAddParty = async () => {
        try {
            await clientService.addRelatedParty(id, partyData);
            setIsPartyModalOpen(false);
            setPartyData({ firstName: '', lastName: '', relationType: 'DIRECTOR', status: 'ACTIVE' });
            fetchDetails();
            notify('Related party added', 'success');
        } catch (err) {
            notify('Failed to add party: ' + err.message, 'error');
        }
    };

    if (loading) return <div className="loading-container"><p className="loading">Initializing client profile...</p></div>;
    if (error) return <div className="error-container"><p className="error">{error}</p></div>;
    if (!client) return <div className="error-container"><p className="error">Client record not found</p></div>;

    const latestRisk = riskHistory[0];

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {/* Professional Hero Header */}
            <div className="glass-section" style={{ padding: '2rem', borderLeft: '4px solid var(--accent-primary)', background: 'linear-gradient(135deg, rgba(255,255,255,0.05) 0%, rgba(255,255,255,0.01) 100%)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div style={{ display: 'flex', gap: '2rem', alignItems: 'center' }}>
                        <div style={{
                            width: '80px', height: '80px', borderRadius: '50%',
                            background: 'var(--accent-primary)', display: 'flex',
                            alignItems: 'center', justifyContent: 'center', fontSize: '2rem',
                            boxShadow: '0 0 20px rgba(0, 242, 254, 0.3)'
                        }}>
                            {client.firstName[0]}{client.lastName[0]}
                        </div>
                        <div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '0.5rem' }}>
                                <h1 style={{ margin: 0, fontSize: '2.2rem' }}>{client.firstName} {client.lastName}</h1>
                                <span className="status-badge" style={{ verticalAlign: 'middle', fontSize: '0.8rem' }}>{client.status}</span>
                            </div>
                            <div style={{ color: 'var(--text-secondary)', display: 'flex', gap: '1.5rem', fontSize: '0.9rem' }}>
                                <span><strong>ID:</strong> #{client.clientID}</span>
                                <span><strong>Onboarded:</strong> {client.onboardingDate}</span>
                                <span><strong>Type:</strong> Individual</span>
                            </div>
                        </div>
                    </div>
                    <div style={{ display: 'flex', gap: '1rem' }}>
                        {hasPermission('MANAGE_CASES') && (
                            <Button onClick={() => setIsCaseModalOpen(true)}>Initiate Review</Button>
                        )}
                        <Link to="/clients" className="btn btn-secondary" style={{ textDecoration: 'none' }}>Directory</Link>
                    </div>
                </div>
            </div>

            {/* Main Content Area with Tabs */}
            <div className="glass-section" style={{ padding: '0', overflow: 'hidden' }}>
                <div style={{ display: 'flex', borderBottom: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.02)' }}>
                    <TabButton active={activeTab === 'overview'} label="Overview" icon="👤" onClick={() => setActiveTab('overview')} />
                    <TabButton active={activeTab === 'compliance'} label="Compliance & Risk" icon="🛡️" onClick={() => setActiveTab('compliance')} />
                    <TabButton active={activeTab === 'parties'} label="Related Parties" icon="👥" onClick={() => setActiveTab('parties')} />
                    <TabButton active={activeTab === 'activity'} label="Activity & Audit" icon="📊" onClick={() => setActiveTab('activity')} />
                </div>

                <div style={{ padding: '2rem' }}>
                    {activeTab === 'overview' && (
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '3rem' }}>
                            <div>
                                <h4 style={{ color: 'var(--accent-primary)', marginBottom: '1.5rem' }}>Personal Details</h4>
                                <div className="case-info-grid">
                                    <DetailItem label="Prefix" value={client.titlePrefix} />
                                    <DetailItem label="Full Name" value={`${client.firstName} ${client.middleName || ''} ${client.lastName}`} />
                                    <DetailItem label="Gender" value={client.gender} />
                                    <DetailItem label="Date of Birth" value={client.dateOfBirth} />
                                    <DetailItem label="Occupation" value={client.occupation} />
                                    <DetailItem label="Language" value={client.language} />
                                </div>
                            </div>
                            <div>
                                <h4 style={{ color: 'var(--accent-primary)', marginBottom: '1.5rem' }}>Addresses</h4>
                                {client.addresses && client.addresses.length > 0 ? (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                        {client.addresses.map((a, i) => (
                                            <div key={i} style={{ padding: '1rem', background: 'rgba(255,255,255,0.03)', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.05)' }}>
                                                <div style={{ fontSize: '0.7rem', color: 'var(--accent-primary)', textTransform: 'uppercase', marginBottom: '4px' }}>{a.addressType}</div>
                                                <div style={{ fontSize: '0.95rem' }}>{a.addressLine1}, {a.city}, {a.country}</div>
                                            </div>
                                        ))}
                                    </div>
                                ) : <p style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>No addresses recorded.</p>}

                                <h4 style={{ color: 'var(--accent-primary)', margin: '2rem 0 1.5rem 0' }}>Identifiers</h4>
                                {client.identifiers && client.identifiers.length > 0 ? (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                        {client.identifiers.map((id, i) => (
                                            <div key={i} style={{ padding: '1rem', background: 'rgba(255,255,255,0.03)', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.05)' }}>
                                                <div style={{ fontSize: '0.7rem', color: 'var(--accent-primary)', textTransform: 'uppercase', marginBottom: '4px' }}>{id.identifierType}</div>
                                                <div style={{ fontSize: '0.95rem' }}>{id.identifierNumber || id.identifierValue} <small style={{ color: 'var(--text-secondary)' }}>via {id.issuingAuthority}</small></div>
                                            </div>
                                        ))}
                                    </div>
                                ) : <p style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>No identifiers recorded.</p>}
                            </div>
                        </div>
                    )}

                    {activeTab === 'compliance' && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
                            <div style={{ display: 'grid', gridTemplateColumns: 'minmax(300px, 1fr) 2fr', gap: '2rem' }}>
                                {/* Risk Pulse Widget */}
                                <div className="glass-section" style={{ padding: '2rem', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: 'rgba(255,255,255,0.02)' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', marginBottom: '1.5rem' }}>
                                        <h4 style={{ margin: 0 }}>Risk Pulse</h4>
                                        <div style={{ display: 'flex', gap: '8px' }}>
                                            <button onClick={() => setIsRiskHistoryOpen(true)} className="btn-icon" title="History">🕒</button>
                                            {hasPermission('MANAGE_RISK') && (
                                                <button
                                                    onClick={async () => {
                                                        if (runningAssessment) return;
                                                        setRunningAssessment(true);
                                                        try {
                                                            await riskService.calculateRisk(id);
                                                            const history = await riskService.getRiskHistory(id);
                                                            setRiskHistory(history);
                                                            notify('Assessment successful', 'success');
                                                        } catch (e) { notify(e.message, 'error'); }
                                                        finally { setRunningAssessment(false); }
                                                    }}
                                                    className={`btn-icon ${runningAssessment ? 'spinning' : ''}`}
                                                    title="Refresh Risk"
                                                >
                                                    🔄
                                                </button>
                                            )}
                                        </div>
                                    </div>

                                    {latestRisk ? (
                                        <>
                                            <div style={{
                                                width: '120px', height: '120px', borderRadius: '50%',
                                                border: `8px solid ${latestRisk.overallRiskLevel === 'HIGH' ? '#ff4d4f' : latestRisk.overallRiskLevel === 'MEDIUM' ? '#faad14' : '#52c41a'}`,
                                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                                fontSize: '2.5rem', fontWeight: 'bold', marginBottom: '1rem',
                                                boxShadow: `inset 0 0 20px ${latestRisk.overallRiskLevel === 'HIGH' ? 'rgba(255,77,79,0.2)' : 'rgba(82,196,26,0.2)'}`
                                            }}>
                                                {latestRisk.overallRiskScore}
                                            </div>
                                            <div style={{ fontSize: '1.2rem', fontWeight: 'bold', color: latestRisk.overallRiskLevel === 'HIGH' ? '#ff4d4f' : latestRisk.overallRiskLevel === 'MEDIUM' ? '#faad14' : '#52c41a' }}>
                                                {latestRisk.overallRiskLevel} RISK
                                            </div>
                                            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
                                                Last assessed: {new Date(latestRisk.createdAt).toLocaleDateString()}
                                            </div>
                                        </>
                                    ) : <p>No assessments run.</p>}
                                </div>

                                <ScreeningPanel clientId={id} hasPermission={hasPermission('MANAGE_SCREENING')} />
                            </div>

                            <Section title="Compliance Profile">
                                <div className="case-info-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
                                    <DetailItem label="Primary Citizenship" value={client.citizenship1} />
                                    <DetailItem label="Secondary Citizenship" value={client.citizenship2} />
                                    <DetailItem label="Country of Tax" value={client.countryOfTax} />
                                    <DetailItem label="Source of Funds" value={client.sourceOfFundsCountry} />
                                    <DetailItem label="FATCA Status" value={client.fatcaStatus} />
                                    <DetailItem label="CRS Status" value={client.crsStatus} />
                                </div>
                            </Section>
                        </div>
                    )}

                    {activeTab === 'parties' && (
                        <Section title="Governance & Relationships" actions={
                            hasPermission("MANAGE_CLIENTS") && <Button variant="secondary" onClick={() => setIsPartyModalOpen(true)}>+ Add Relation</Button>
                        }>
                            {client.relatedParties && client.relatedParties.length > 0 ? (
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Name</th>
                                            <th>Relation</th>
                                            <th>Status</th>
                                            <th style={{ textAlign: 'right' }}>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {client.relatedParties.map((rp, i) => (
                                            <tr key={i}>
                                                <td><div style={{ fontWeight: '600' }}>{rp.firstName} {rp.lastName}</div></td>
                                                <td>{rp.relationType}</td>
                                                <td><span className="status-badge">{rp.status}</span></td>
                                                <td style={{ textAlign: 'right' }}>
                                                    <Button variant="secondary" style={{ fontSize: '0.8rem', padding: '0.2rem 0.6rem' }} onClick={() => setViewParty(rp)}>Profile</Button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            ) : <p style={{ color: 'var(--text-secondary)', fontStyle: 'italic', textAlign: 'center', padding: '2rem' }}>No related parties recorded.</p>}
                        </Section>
                    )}

                    {activeTab === 'activity' && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
                            <Section title="KYC Case History" actions={
                                cases.length > 0 && (
                                    <Button variant="secondary" onClick={() => setViewQuestionnaireCaseId(cases[0].caseID)} style={{ fontSize: '0.8rem', padding: '0.2rem 0.6rem' }}>
                                        Latest Questionnaire
                                    </Button>
                                )
                            }>
                                {cases.length > 0 ? (
                                    <table>
                                        <thead>
                                            <tr>
                                                <th>Case ID</th>
                                                <th>Reason</th>
                                                <th>Status</th>
                                                <th>Date</th>
                                                <th style={{ textAlign: 'right' }}>Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {cases.map((c) => (
                                                <tr key={c.caseID}>
                                                    <td>#{c.caseID}</td>
                                                    <td>{c.reason}</td>
                                                    <td><span className={`status-badge ${c.status === 'APPROVED' ? 'active' : c.status === 'REJECTED' ? 'rejected' : 'pending'}`}>{c.status}</span></td>
                                                    <td style={{ fontSize: '0.8rem' }}>{c.createdDate}</td>
                                                    <td style={{ textAlign: 'right' }}>
                                                        <Link to={`/cases/${c.caseID}`} className="btn btn-secondary" style={{ fontSize: '0.8rem', padding: '0.2rem 0.6rem', textDecoration: 'none' }}>Open</Link>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                ) : <p style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: '2rem' }}>No case history found.</p>}
                            </Section>

                            <Section title="Material Changes Audit">
                                {materialChanges.length > 0 ? (
                                    <table>
                                        <thead>
                                            <tr>
                                                <th>Date</th>
                                                <th>Category</th>
                                                <th>Field</th>
                                                <th>New Value</th>
                                                <th>Status</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {materialChanges.map((mc) => (
                                                <tr key={mc.changeID}>
                                                    <td style={{ fontSize: '0.8rem' }}>{new Date(mc.changeDate).toLocaleDateString()}</td>
                                                    <td><span className={`status-badge ${mc.category === 'RISK' ? 'suspended' : mc.category === 'SCREENING' ? 'pending' : 'active'}`}>{mc.category}</span></td>
                                                    <td><strong>{mc.columnName}</strong></td>
                                                    <td style={{ color: '#51cf66', fontWeight: '500' }}>{mc.newValue}</td>
                                                    <td><span className={`status-badge ${mc.status === 'PENDING' ? 'rejected' : 'active'}`}>{mc.status}</span></td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                ) : <p style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: '2rem' }}>No material changes recorded.</p>}
                            </Section>
                        </div>
                    )}
                </div>
            </div>

            {/* Modals */}
            {/* Redesigned Initiate Review Modal */}
            <Modal isOpen={isCaseModalOpen} onClose={() => setIsCaseModalOpen(false)} title="Initiate Professional Review" maxWidth="500px">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                    <div style={{ padding: '1rem', background: 'rgba(0, 242, 254, 0.05)', borderRadius: '8px', border: '1px solid rgba(0, 242, 254, 0.1)' }}>
                        <p style={{ color: 'var(--text-primary)', margin: '0 0 0.5rem 0', fontWeight: '600' }}>Review Context</p>
                        <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: 0 }}>
                            You are initiating a formal KYC review for <strong>{client.firstName} {client.lastName}</strong>.
                            This action will create a new CMMN-backed case and notify the assigned team.
                        </p>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1.2rem' }}>
                        <div>
                            <label style={{ display: 'block', marginBottom: '0.6rem', fontSize: '0.85rem', fontWeight: '600', color: 'var(--text-secondary)' }}>Trigger Reason</label>
                            <select
                                value={caseReason}
                                onChange={(e) => setCaseReason(e.target.value)}
                                style={{
                                    width: '100%', padding: '0.8rem', background: 'rgba(255,255,255,0.05)',
                                    color: 'white', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px',
                                    outline: 'none'
                                }}
                            >
                                <option value="Periodic Review">Periodic Review</option>
                                <option value="Material Change">Material Change</option>
                                <option value="Onboarding">Onboarding</option>
                                <option value="Ad-hoc Review">Ad-hoc Review</option>
                                <option value="Enhanced Due Diligence">Enhanced Due Diligence</option>
                            </select>
                        </div>

                        <div>
                            <label style={{ display: 'block', marginBottom: '0.6rem', fontSize: '0.85rem', fontWeight: '600', color: 'var(--text-secondary)' }}>Initial Rationale / Comments</label>
                            <textarea
                                value={caseComment}
                                onChange={(e) => setCaseComment(e.target.value)}
                                placeholder="Enter the reason for initiating this review and any preliminary findings..."
                                style={{
                                    width: '100%', padding: '0.8rem', background: 'rgba(255,255,255,0.05)',
                                    color: 'white', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px',
                                    minHeight: '120px', outline: 'none', resize: 'vertical'
                                }}
                            />
                        </div>
                    </div>

                    <div style={{ display: 'flex', gap: '1rem', marginTop: '0.5rem' }}>
                        <Button onClick={handleCreateCase} disabled={creatingCase} style={{ flex: 2 }}>
                            {creatingCase ? 'Initializing Workflow...' : 'Launch Formal Review'}
                        </Button>
                        <Button variant="secondary" onClick={() => setIsCaseModalOpen(false)} style={{ flex: 1 }}>
                            Cancel
                        </Button>
                    </div>
                </div>
            </Modal>

            <Modal isOpen={isRiskHistoryOpen} onClose={() => { setIsRiskHistoryOpen(false); setSelectedAssessment(null); }} title="Assessment Intelligence" maxWidth="900px">
                {selectedAssessment ? (
                    <div>
                        <Button variant="secondary" onClick={() => setSelectedAssessment(null)} style={{ marginBottom: '1.5rem' }}>← Back to Timeline</Button>
                        <div className="glass-section" style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem', padding: '1.5rem', marginBottom: '2rem' }}>
                            <DetailItem label="Overall Score" value={selectedAssessment.overallRiskScore} />
                            <DetailItem label="Level" value={selectedAssessment.overallRiskLevel} />
                            <DetailItem label="Initial Level" value={selectedAssessment.initialRiskLevel} />
                            <DetailItem label="Methodology" value={selectedAssessment.typeOfLogicApplied} />
                        </div>
                        <h4 style={{ color: 'var(--accent-primary)', marginBottom: '1rem' }}>Risk Factor Breakdown</h4>
                        <table>
                            <thead>
                                <tr><th>Type</th><th>Element</th><th>Evidence</th><th>Score</th></tr>
                            </thead>
                            <tbody>
                                {assessmentDetails.map((d, i) => (
                                    <tr key={i}>
                                        <td>{d.riskType}</td>
                                        <td><strong>{d.elementName}</strong></td>
                                        <td>{d.elementValue}</td>
                                        <td style={{ fontWeight: 'bold' }}>{d.riskScore}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    riskHistory.length > 0 ? (
                        <table>
                            <thead>
                                <tr><th>Timestamp</th><th>Score</th><th>Level</th><th>Method</th><th>Action</th></tr>
                            </thead>
                            <tbody>
                                {riskHistory.map((h, i) => (
                                    <tr key={i}>
                                        <td>{new Date(h.createdAt).toLocaleString()}</td>
                                        <td style={{ fontWeight: 'bold' }}>{h.overallRiskScore}</td>
                                        <td><span className={`status-badge ${h.overallRiskLevel === 'HIGH' ? 'rejected' : 'active'}`}>{h.overallRiskLevel}</span></td>
                                        <td>{h.typeOfLogicApplied}</td>
                                        <td><Button variant="secondary" onClick={async () => {
                                            const details = await riskService.getAssessmentDetails(h.assessmentID);
                                            setAssessmentDetails(details);
                                            setSelectedAssessment(h);
                                        }} style={{ fontSize: '0.7rem', padding: '0.2rem 0.5rem' }}>Analyze</Button></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    ) : <p>No history available.</p>
                )}
            </Modal>

            <Modal isOpen={isPartyModalOpen} onClose={() => setIsPartyModalOpen(false)} title="Establish Personal Connection" maxWidth="500px">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                        <input type="text" placeholder="First Name" value={partyData.firstName} onChange={(e) => setPartyData({ ...partyData, firstName: e.target.value })} className="form-control" style={{ padding: '0.6rem', background: '#1a1a1a', border: '1px solid #333', color: 'white' }} />
                        <input type="text" placeholder="Last Name" value={partyData.lastName} onChange={(e) => setPartyData({ ...partyData, lastName: e.target.value })} className="form-control" style={{ padding: '0.6rem', background: '#1a1a1a', border: '1px solid #333', color: 'white' }} />
                    </div>
                    <select value={partyData.relationType} onChange={(e) => setPartyData({ ...partyData, relationType: e.target.value })} className="form-control" style={{ padding: '0.6rem', background: '#1a1a1a', border: '1px solid #333', color: 'white' }}>
                        <option value="DIRECTOR">Director</option>
                        <option value="SHAREHOLDER">Shareholder</option>
                        <option value="UBO">Ultimate Beneficial Owner</option>
                    </select>
                    <Button onClick={handleAddParty}>Connect Relationship</Button>
                </div>
            </Modal>

            {viewParty && (
                <Modal isOpen={true} onClose={() => setViewParty(null)} title={`${viewParty.firstName} ${viewParty.lastName} Profile`} maxWidth="800px">
                    <div className="case-info-grid" style={{ marginBottom: '2rem' }}>
                        <DetailItem label="Relationship" value={viewParty.relationType} />
                        <DetailItem label="Status" value={viewParty.status} />
                        <DetailItem label="Citizenship" value={viewParty.citizenship1} />
                        <DetailItem label="Role" value={viewParty.role} />
                    </div>
                    {viewParty.identifiers && (
                        <Section title="Identifiers">
                            <table>
                                <thead><tr><th>Type</th><th>Value</th></tr></thead>
                                <tbody>
                                    {viewParty.identifiers.map((id, i) => (
                                        <tr key={i}><td>{id.identifierType}</td><td>{id.identifierValue}</td></tr>
                                    ))}
                                </tbody>
                            </table>
                        </Section>
                    )}
                </Modal>
            )}

            <Modal isOpen={!!viewQuestionnaireCaseId} onClose={() => setViewQuestionnaireCaseId(null)} title={`Compliance Questionnaire (Case #${viewQuestionnaireCaseId})`} maxWidth="950px">
                <Questionnaire caseId={viewQuestionnaireCaseId} readOnly={true} />
            </Modal>
        </div>
    );
};

export default ClientDetails;
