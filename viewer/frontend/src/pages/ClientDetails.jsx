import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { clientService } from '../services/clientService';
import Button from '../components/Button';
import Modal from '../components/Modal'; // Restored Modal
import { useAuth } from '../contexts/AuthContext';
import { useNotification } from '../contexts/NotificationContext'; // Restored useNotification
import { caseService } from '../services/caseService'; // Restored caseService
import { riskService } from '../services/riskService';
import { useNavigate } from 'react-router-dom';
import Questionnaire from './Questionnaire';
import ScreeningPanel from '../components/ScreeningPanel';

const Section = ({ title, children, actions }) => (
    <section className="glass-section" style={{ marginTop: '1.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ margin: 0 }}>{title}</h3>
            {actions}
        </div>
        {children}
    </section>
);

const DetailItem = ({ label, value }) => (
    <div className="info-item">
        <strong>{label}</strong>
        <span>{value || '-'}</span>
    </div>
);

const formatAddress = (addr) => {
    return `${addr.addressLine1}, ${addr.city}, ${addr.country} `;
};

const ClientDetails = () => {
    const { id } = useParams();
    const { hasPermission } = useAuth();
    const { notify } = useNotification();
    const navigate = useNavigate();
    const [client, setClient] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Modal States
    const [isCaseModalOpen, setIsCaseModalOpen] = useState(false);
    const [caseReason, setCaseReason] = useState('Periodic Review');
    const [isPartyModalOpen, setIsPartyModalOpen] = useState(false);
    const [partyData, setPartyData] = useState({
        firstName: '', lastName: '', relationType: 'DIRECTOR', status: 'ACTIVE'
    });

    const [viewParty, setViewParty] = useState(null);
    const [cases, setCases] = useState([]);
    const [riskHistory, setRiskHistory] = useState([]);
    const [isRiskHistoryOpen, setIsRiskHistoryOpen] = useState(false);
    const [selectedAssessment, setSelectedAssessment] = useState(null);
    const [assessmentDetails, setAssessmentDetails] = useState([]);
    const [creatingCase, setCreatingCase] = useState(false);
    const [runningAssessment, setRunningAssessment] = useState(false);
    const [viewQuestionnaireCaseId, setViewQuestionnaireCaseId] = useState(null);

    const fetchDetails = async () => {
        setLoading(true);
        try {
            const clientData = await clientService.getClientDetails(id);
            setClient(clientData);

            // Fetch cases for this client
            try {
                const casesData = await caseService.getCasesByClient(id);
                setCases(casesData);
            } catch (e) {
                console.error("Failed to fetch cases", e);
            }

            // Fetch risk history
            try {
                if (clientData && clientData.clientID) {
                    const riskData = await riskService.getRiskHistory(clientData.clientID);
                    setRiskHistory(riskData);
                }
            } catch (e) {
                console.error("Failed to fetch risk history", e);
            }
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
            const caseId = await caseService.createCase(id, caseReason);
            setIsCaseModalOpen(false);
            notify('Case created successfully', 'success');
            navigate(`/ cases / ${caseId} `);
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

    if (loading) return <p className="loading">Loading details...</p>;
    if (error) return <p className="error">{error}</p>;
    if (!client) return <p className="error">Client not found</p>;

    return (
        <div>
            <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
                <h1 style={{ margin: 0 }}>{client.firstName} {client.lastName}</h1>
                <div style={{ display: 'flex', gap: '1rem' }}>
                    {hasPermission('MANAGE_CASES') && (
                        <Button onClick={() => setIsCaseModalOpen(true)}>Create KYC Case</Button>
                    )}
                    <Link to="/clients" className="back-link">Back to list</Link>
                </div>
            </header>

            <Section title="Identity Information">
                <div style={{ display: 'flex', gap: '2rem' }}>
                    <div style={{ flex: 2 }} className="case-info-grid">
                        <DetailItem label="Title" value={client.titlePrefix} />
                        <DetailItem label="First Name" value={client.firstName} />
                        <DetailItem label="Middle Name" value={client.middleName} />
                        <DetailItem label="Last Name" value={client.lastName} />
                        <DetailItem label="Suffix" value={client.titleSuffix} />
                        <DetailItem label="Name at Birth" value={client.nameAtBirth} />
                        <DetailItem label="Nickname" value={client.nickName} />
                        <DetailItem label="Gender" value={client.gender} />
                        <DetailItem label="Date of Birth" value={client.dateOfBirth} />
                    </div>

                    {/* Risk Pulse Panel */}
                    <div style={{
                        flex: 1,
                        background: 'rgba(255,255,255,0.03)',
                        border: '1px solid var(--glass-border)',
                        borderRadius: '8px',
                        padding: '1.5rem',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        position: 'relative'
                    }}>
                        <div style={{ position: 'absolute', top: '10px', right: '10px', display: 'flex', gap: '5px' }}>
                            {hasPermission('MANAGE_RISK') && (
                                <>
                                    <button
                                        onClick={() => setIsRiskHistoryOpen(true)}
                                        title="View History"
                                        style={{
                                            background: 'transparent',
                                            border: '1px solid var(--glass-border)',
                                            borderRadius: '4px',
                                            color: 'var(--text-primary)',
                                            cursor: 'pointer',
                                            padding: '4px 8px',
                                            fontSize: '1.2rem'
                                        }}
                                    >
                                        🕒
                                    </button>
                                    <button
                                        onClick={async () => {
                                            if (runningAssessment) return;
                                            setRunningAssessment(true);
                                            try {
                                                await riskService.calculateRisk(client.clientID);
                                                // Refresh history
                                                const history = await riskService.getRiskHistory(client.clientID);
                                                setRiskHistory(history);
                                                notify('Risk Assessment completed successfully', 'success');
                                            } catch (e) {
                                                notify('Failed to run risk assessment: ' + e.message, 'error');
                                            } finally {
                                                setRunningAssessment(false);
                                            }
                                        }}
                                        disabled={runningAssessment}
                                        title="Run Risk Assessment"
                                        style={{
                                            background: 'transparent',
                                            border: '1px solid var(--glass-border)',
                                            borderRadius: '4px',
                                            color: 'var(--text-primary)',
                                            cursor: 'pointer',
                                            padding: '4px 8px',
                                            fontSize: '1.2rem',
                                            opacity: runningAssessment ? 0.5 : 1
                                        }}
                                    >
                                        ▶️
                                    </button>
                                </>
                            )}
                        </div>

                        <h4 style={{ margin: '0 0 1rem 0', color: 'var(--text-secondary)' }}>Risk Pulse</h4>

                        {riskHistory.length > 0 ? (
                            <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
                                <div style={{
                                    fontSize: '3rem',
                                    fontWeight: 'bold',
                                    color: riskHistory[0].overallRiskLevel === 'HIGH' ? '#ff4d4f' : riskHistory[0].overallRiskLevel === 'MEDIUM' ? '#faad14' : '#52c41a',
                                    lineHeight: 1
                                }}>
                                    {riskHistory[0].overallRiskScore}
                                </div>
                                <div style={{
                                    marginTop: '0.5rem',
                                    fontSize: '1.2rem',
                                    fontWeight: '500',
                                    color: riskHistory[0].overallRiskLevel === 'HIGH' ? '#ff4d4f' : riskHistory[0].overallRiskLevel === 'MEDIUM' ? '#faad14' : '#52c41a'
                                }}>
                                    {riskHistory[0].overallRiskLevel}
                                </div>
                            </div>
                        ) : (
                            <div style={{ marginBottom: '1.5rem', fontStyle: 'italic', color: 'var(--text-secondary)' }}>
                                No assessment run
                            </div>
                        )}

                    </div>

                    {/* Screening Panel */}
                    <ScreeningPanel
                        clientId={client.clientID}
                        hasPermission={hasPermission('MANAGE_SCREENING') || hasPermission('MANAGE_RISK')} // Use existing permission or new one
                    />
                </div>
            </Section>

            <Section title="Status & Metadata">
                <div className="case-info-grid">
                    <DetailItem label="Client ID" value={client.clientID} />
                    <DetailItem label="Status" value={<span className="status-badge">{client.status}</span>} />
                    <DetailItem label="Onboarding Date" value={client.onboardingDate} />
                    <DetailItem label="Occupation" value={client.occupation} />
                </div>
            </Section>

            <Section title="Citizenship & Compliance">
                <div className="case-info-grid">
                    <DetailItem label="Primary Citizenship" value={client.citizenship1} />
                    <DetailItem label="Secondary Citizenship" value={client.citizenship2} />
                    <DetailItem label="Language" value={client.language} />
                    <DetailItem label="Country of Tax" value={client.countryOfTax} />
                    <DetailItem label="Source of Funds" value={client.sourceOfFundsCountry} />
                    <DetailItem label="FATCA Status" value={client.fatcaStatus} />
                    <DetailItem label="CRS Status" value={client.crsStatus} />
                </div>
            </Section>

            <Section title="Addresses">
                {client.addresses ? (
                    <table>
                        <thead>
                            <tr>
                                <th>Type</th>
                                <th>Address</th>
                                <th>City</th>
                                <th>Country</th>
                            </tr>
                        </thead>
                        <tbody>
                            {client.addresses.map((a, i) => (
                                <tr key={i}>
                                    <td>{a.addressType}</td>
                                    <td>{a.addressLine1}</td>
                                    <td>{a.city}</td>
                                    <td>{a.country}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                ) : (
                    <div style={{ padding: '1rem', fontStyle: 'italic', color: 'var(--text-secondary)' }}>
                        Restricted Access: You do not have permission to view addresses.
                    </div>
                )}
            </Section>

            <Section title="Related Parties" actions={
                hasPermission("MANAGE_CLIENTS") && <Button variant="secondary" onClick={() => setIsPartyModalOpen(true)}>+ Add Party</Button>
            }>
                {client.relatedParties ? (
                    <table>
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Relation</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            {client.relatedParties.map((rp, i) => (
                                <tr key={i}>
                                    <td>{rp.firstName} {rp.lastName}</td>
                                    <td>{rp.relationType}</td>
                                    <td><span className="status-badge">{rp.status}</span></td>
                                    <td>
                                        <Button variant="secondary" onClick={() => setViewParty(rp)}>Details</Button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                ) : (
                    <div style={{ padding: '1rem', fontStyle: 'italic', color: 'var(--text-secondary)' }}>
                        Restricted Access: You do not have permission to view related parties.
                    </div>
                )}
            </Section>

            <Section title="Identifiers">
                {client.identifiers && client.identifiers.length > 0 ? (
                    <table>
                        <thead>
                            <tr>
                                <th>Type</th>
                                <th>Value</th>
                                <th>Authority</th>
                            </tr>
                        </thead>
                        <tbody>
                            {client.identifiers.map((id, i) => (
                                <tr key={i}>
                                    <td>{id.identifierType}</td>
                                    <td>{id.identifierNumber || id.identifierValue}</td>
                                    <td>{id.issuingAuthority}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                ) : (
                    <div style={{ padding: '1rem', fontStyle: 'italic', color: 'var(--text-secondary)' }}>
                        {client.identifiers === null ? "Restricted Access." : "No identifiers recorded."}
                    </div>
                )}
            </Section>


            {/* Relevant Cases Section */}
            <Section title="Relevant Cases" actions={
                cases && cases.length > 0 && (
                    <Button
                        onClick={() => {
                            const latestCaseId = cases.sort((a, b) => b.caseID - a.caseID)[0].caseID;
                            setViewQuestionnaireCaseId(latestCaseId);
                        }}
                        variant="secondary"
                        style={{ fontSize: '0.9rem', padding: '0.3rem 0.8rem' }}
                    >
                        📄 View Latest Questionnaire
                    </Button>
                )
            }>
                {cases && cases.length > 0 ? (
                    <table>
                        <thead>
                            <tr>
                                <th>Case ID</th>
                                <th>Reason</th>
                                <th>Status</th>
                                <th>Created Date</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            {cases.map((c) => (
                                <tr key={c.caseID}>
                                    <td>{c.caseID}</td>
                                    <td>{c.reason}</td>
                                    <td><span className="status-badge">{c.status}</span></td>
                                    <td>{c.createdDate}</td>
                                    <td>
                                        <Link to={`/ cases / ${c.caseID} `} className="btn btn-secondary">View Case</Link>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                ) : (
                    <div style={{ padding: '1rem', color: 'var(--text-secondary)' }}>
                        No cases found for this client.
                    </div>
                )}
            </Section>

            {client.accounts && client.accounts.length > 0 && (
                <Section title="Accounts">
                    <table>
                        <thead>
                            <tr>
                                <th>Account Number</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            {client.accounts.map((acc, i) => (
                                <tr key={i}>
                                    <td>{acc.accountNumber}</td>
                                    <td><span className="status-badge">{acc.accountStatus}</span></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </Section>
            )}

            {client.portfolios && client.portfolios.length > 0 && (
                <Section title="Portfolios">
                    <table>
                        <thead>
                            <tr>
                                <th>Portfolio</th>
                                <th>Account</th>
                                <th>Onboarding Date</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            {client.portfolios.map((p, i) => (
                                <tr key={i}>
                                    <td>{p.portfolioText}</td>
                                    <td>{p.accountNumber}</td>
                                    <td>{p.onboardingDate}</td>
                                    <td><span className="status-badge">{p.status}</span></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </Section>
            )}

            {/* Create Case Modal */}
            <Modal
                isOpen={isCaseModalOpen}
                onClose={() => setIsCaseModalOpen(false)}
                title="Create New KYC Case"
                maxWidth="400px"
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem' }}>Reason for Opening Case</label>
                        <select
                            value={caseReason}
                            onChange={(e) => setCaseReason(e.target.value)}
                            style={{ width: '100%', padding: '0.5rem', background: 'rgba(255,255,255,0.05)', color: 'white', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                        >
                            <option value="Periodic Review">Periodic Review</option>
                            <option value="Material Change">Material Change</option>
                            <option value="Onboarding">Onboarding</option>
                            <option value="Ad-hoc Review">Ad-hoc Review</option>
                        </select>
                    </div>
                    <Button onClick={handleCreateCase} disabled={creatingCase}>{creatingCase ? 'Creating...' : 'Create Case'}</Button>
                </div>
            </Modal>

            {/* Risk History Modal */}
            <Modal
                isOpen={isRiskHistoryOpen}
                onClose={() => {
                    setIsRiskHistoryOpen(false);
                    setSelectedAssessment(null);
                    setAssessmentDetails([]);
                }}
                title={selectedAssessment ? `Assessment Details(${new Date(selectedAssessment.createdAt).toLocaleDateString()})` : "Risk Assessment History"}
                maxWidth="800px"
            >
                {selectedAssessment ? (
                    <div>
                        <Button variant="secondary" onClick={() => setSelectedAssessment(null)} style={{ marginBottom: '1rem' }}>
                            ← Back to History
                        </Button>
                        <div className="case-info-grid" style={{ marginBottom: '1.5rem', background: 'rgba(255,255,255,0.03)', padding: '1rem', borderRadius: '8px' }}>
                            <DetailItem label="Overall Score" value={selectedAssessment.overallRiskScore} />
                            <DetailItem label="Risk Level" value={selectedAssessment.overallRiskLevel} />
                            <DetailItem label="Initial Level" value={selectedAssessment.initialRiskLevel} />
                            <DetailItem label="Logic Applied" value={selectedAssessment.typeOfLogicApplied} />
                        </div>

                        <h4>Risk breakdown</h4>
                        {assessmentDetails && assessmentDetails.length > 0 ? (
                            <table>
                                <thead>
                                    <tr>
                                        <th>Risk Type</th>
                                        <th>Element</th>
                                        <th>Value</th>
                                        <th>Score</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {assessmentDetails.map((d, i) => (
                                        <tr key={i}>
                                            <td>{d.riskType}</td>
                                            <td>{d.elementName}</td>
                                            <td>{d.elementValue}</td>
                                            <td>{d.riskScore}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        ) : (
                            <p style={{ fontStyle: 'italic' }}>No detailed breakdown available.</p>
                        )}
                    </div>
                ) : (
                    riskHistory && riskHistory.length > 0 ? (
                        <table>
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Overall Score</th>
                                    <th>Risk Level</th>
                                    <th>Initial Level</th>
                                </tr>
                            </thead>
                            <tbody>
                                {riskHistory.map((h, i) => (
                                    <tr key={i} onClick={async () => {
                                        try {
                                            const details = await riskService.getAssessmentDetails(h.assessmentID);
                                            setAssessmentDetails(details);
                                            setSelectedAssessment(h);
                                        } catch (e) {
                                            notify('Failed to load details: ' + e.message, 'error');
                                        }
                                    }} style={{ cursor: 'pointer' }} className="clickable-row">
                                        <td>{new Date(h.createdAt).toLocaleString()}</td>
                                        <td>{h.overallRiskScore}</td>
                                        <td><span className={`status - badge ${h.overallRiskLevel === 'HIGH' ? 'rejected' : h.overallRiskLevel === 'MEDIUM' ? 'pending' : 'active'} `}>{h.overallRiskLevel}</span></td>
                                        <td>{h.initialRiskLevel}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    ) : (
                        <div style={{ padding: '1rem', fontStyle: 'italic', color: 'var(--text-secondary)' }}>
                            No risk assessments recorded.
                        </div>
                    )
                )}
            </Modal>

            {/* Add Party Modal */}
            <Modal
                isOpen={isPartyModalOpen}
                onClose={() => setIsPartyModalOpen(false)}
                title="Add Related Party"
                maxWidth="500px"
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                        <div>
                            <label style={{ display: 'block', marginBottom: '0.5rem' }}>First Name</label>
                            <input
                                type="text"
                                value={partyData.firstName}
                                onChange={(e) => setPartyData({ ...partyData, firstName: e.target.value })}
                                style={{ width: '100%', padding: '0.5rem', background: 'rgba(255,255,255,0.05)', color: 'white', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                            />
                        </div>
                        <div>
                            <label style={{ display: 'block', marginBottom: '0.5rem' }}>Last Name</label>
                            <input
                                type="text"
                                value={partyData.lastName}
                                onChange={(e) => setPartyData({ ...partyData, lastName: e.target.value })}
                                style={{ width: '100%', padding: '0.5rem', background: 'rgba(255,255,255,0.05)', color: 'white', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                            />
                        </div>
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem' }}>Relation Type</label>
                        <select
                            value={partyData.relationType}
                            onChange={(e) => setPartyData({ ...partyData, relationType: e.target.value })}
                            style={{ width: '100%', padding: '0.5rem', background: 'rgba(255,255,255,0.05)', color: 'white', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                        >
                            <option value="DIRECTOR">Director</option>
                            <option value="SHAREHOLDER">Shareholder</option>
                            <option value="UBO">Ultimate Beneficial Owner</option>
                            <option value="AUTHORIZED_SIGNATORY">Authorized Signatory</option>
                        </select>
                    </div>
                    <Button onClick={handleAddParty}>Add Party</Button>
                </div>
            </Modal>
            {/* View Party Modal */}
            {viewParty && (
                <div className="modal" style={{ display: 'block' }}>
                    <div className="modal-content" style={{ maxWidth: '800px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                            <h3 style={{ margin: 0 }}>Related Party Details: {viewParty.firstName} {viewParty.lastName}</h3>
                            <Button variant="secondary" onClick={() => setViewParty(null)}>Close</Button>
                        </div>

                        <Section title="Identity">
                            <div className="case-info-grid">
                                <DetailItem label="Relation" value={viewParty.relationType} />
                                <DetailItem label="Status" value={<span className="status-badge">{viewParty.status}</span>} />
                                <DetailItem label="Date of Birth" value={viewParty.dateOfBirth} />
                                <DetailItem label="Citizenship" value={viewParty.citizenship1} />
                                <DetailItem label="Role" value={viewParty.role} />
                            </div>
                        </Section>

                        <Section title="Addresses">
                            {viewParty.addresses && viewParty.addresses.length > 0 ? (
                                <ul>
                                    {viewParty.addresses.map((addr, i) => (
                                        <li key={i}>{formatAddress(addr)} ({addr.addressType})</li>
                                    ))}
                                </ul>
                            ) : <p>No addresses recorded.</p>}
                        </Section>

                        <Section title="Identifiers">
                            {viewParty.identifiers && viewParty.identifiers.length > 0 ? (
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Type</th>
                                            <th>Value</th>
                                            <th>Authority</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {viewParty.identifiers.map((id, i) => (
                                            <tr key={i}>
                                                <td>{id.identifierType}</td>
                                                <td>{id.identifierNumber || id.identifierValue}</td>
                                                <td>{id.issuingAuthority}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            ) : <p>No identifiers recorded.</p>}
                        </Section>
                    </div>
                </div>
            )}

            {/* Questionnaire Modal */}
            <Modal
                isOpen={!!viewQuestionnaireCaseId}
                onClose={() => setViewQuestionnaireCaseId(null)}
                title={`Questionnaire(Case #${viewQuestionnaireCaseId})`}
                maxWidth="900px"
            >
                {viewQuestionnaireCaseId && (
                    <Questionnaire caseId={viewQuestionnaireCaseId} readOnly={true} />
                )}
            </Modal>
        </div>
    );
};

export default ClientDetails;
