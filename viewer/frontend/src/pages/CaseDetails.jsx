import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { caseService } from '../services/caseService';
import { riskService } from '../services/riskService';
import { useAuth } from '../contexts/AuthContext';
import Button from '../components/Button';
import Modal from '../components/Modal';
import { useNotification } from '../contexts/NotificationContext';
import CaseTimeline from '../components/CaseTimeline';
import CaseActions from '../components/CaseActions';
import ScreeningPanel from '../components/ScreeningPanel';

const CaseDetails = () => {
    const { id } = useParams();
    const { hasPermission, user } = useAuth();
    const { notify } = useNotification();
    const [kycCase, setKycCase] = useState(null);
    const [comments, setComments] = useState([]);
    const [docs, setDocs] = useState([]);
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [commentInput, setCommentInput] = useState('');
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState('');
    const [validationError, setValidationError] = useState(null);
    const [transitioning, setTransitioning] = useState(false);
    const [relatedCases, setRelatedCases] = useState([]);
    const [timeline, setTimeline] = useState([]);
    const [riskHistory, setRiskHistory] = useState([]);
    const [myTasks, setMyTasks] = useState([]);
    const [activeTab, setActiveTab] = useState('flow');

    // Modal States
    const [isDocModalOpen, setIsDocModalOpen] = useState(false);

    // Version History State
    const [isHistoryModalOpen, setIsHistoryModalOpen] = useState(false);
    const [selectedDocHistory, setSelectedDocHistory] = useState([]);
    const [historyLoading, setHistoryLoading] = useState(false);

    const [uploadData, setUploadData] = useState({
        file: null, category: 'IDENTIFICATION', comment: '', documentName: ''
    });
    const [uploading, setUploading] = useState(false);

    // Assignment States
    const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
    const [assignableUsers, setAssignableUsers] = useState([]);
    const [selectedAssignee, setSelectedAssignee] = useState('');
    const [assigning, setAssigning] = useState(false);

    const loadCaseData = async () => {
        if (!kycCase) setLoading(true);
        try {
            const [caseData, commentsData, docsData, eventsData] = await Promise.all([
                caseService.getCaseDetails(id),
                caseService.getCaseComments(id),
                caseService.getCaseDocuments(id),
                caseService.getCaseEvents(id)
            ]);
            setKycCase(caseData);
            setComments(commentsData || []);
            setDocs(docsData || []);
            setEvents(eventsData || []);

            // Fetch related cases after getting case details
            if (caseData.clientID) {
                const related = await caseService.getCasesByClient(caseData.clientID);
                setRelatedCases(related.filter(c => c.caseID !== parseInt(id)));

                // Fetch Risk History
                try {
                    const riskData = await riskService.getRiskHistory(caseData.clientID);
                    setRiskHistory(riskData);
                } catch (rErr) {
                    console.error("Failed to fetch risk history", rErr);
                }
            }

            // Fetch CMMN Timeline if applicable
            if (caseData.workflowType === 'CMMN') {
                try {
                    const timelineData = await caseService.getCaseTimeline(id);
                    setTimeline(timelineData);
                } catch (tErr) {
                    console.error("Timeline Fetch Error:", tErr);
                }
            }

            // Fetch My Active Tasks for this Case
            try {
                const tasks = await caseService.getUserTasks();
                const caseTasks = tasks.filter(t => {
                    if (t.caseId && String(t.caseId) === String(id)) return true;
                    if (caseData.instanceID && t.caseInstanceId === caseData.instanceID) return true;
                    return false;
                });
                setMyTasks(caseTasks);
            } catch (taskErr) {
                console.error("Failed to fetch user tasks", taskErr);
            }
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCaseData();
    }, [id]);

    const handleTransition = async (action) => {
        if (!commentInput) return notify('Comment is required for workflow actions', 'warning');
        setTransitioning(true);
        setError(null);
        setSuccessMessage('');
        setValidationError(null);
        try {
            await caseService.transitionCase(id, action, commentInput);
            setCommentInput('');
            setSuccessMessage(`Case transitioned (${action}) successfully.`);
            notify(`Case transitioned to ${action} successfully`, 'success');
            loadCaseData();
        } catch (err) {
            console.error("Transition Error:", err);
            notify(err.message, 'error');
            setValidationError(err.message);
        } finally {
            setTransitioning(false);
        }
    };


    const handleUpload = async () => {
        if (!uploadData.file) return notify('Please select a file', 'warning');
        setUploading(true);
        try {
            const formData = new FormData();
            formData.append('file', uploadData.file);
            formData.append('category', uploadData.category);
            formData.append('comment', uploadData.comment);
            formData.append('uploadedBy', user.username);
            if (uploadData.documentName) {
                formData.append('documentName', uploadData.documentName);
            }

            await caseService.uploadDocument(id, formData);
            setIsDocModalOpen(false);
            setUploadData({ file: null, category: 'IDENTIFICATION', comment: '', documentName: '' });
            loadCaseData();
            notify('Document uploaded successfully', 'success');
        } catch (err) {
            notify('Upload failed: ' + err.message, 'error');
        } finally {
            setUploading(false);
        }
    };

    const handleViewHistory = async (docName) => {
        setHistoryLoading(true);
        setIsHistoryModalOpen(true);
        try {
            const versions = await caseService.getDocumentVersions(id, docName);
            setSelectedDocHistory(versions);
        } catch (err) {
            notify('Failed to load history: ' + err.message, 'error');
        } finally {
            setHistoryLoading(false);
        }
    };

    const handleAssign = async (assignee) => {
        setAssigning(true);
        try {
            await caseService.assignCase(id, assignee);
            setSuccessMessage(assignee ? `Case assigned to ${assignee}` : 'Case unassigned');
            notify(assignee ? `Case assigned to ${assignee}` : 'Case unassigned', 'success');
            setIsAssignModalOpen(false);
            loadCaseData();
        } catch (err) {
            notify('Assignment failed: ' + err.message, 'error');
        } finally {
            setAssigning(false);
        }
    };

    const handleCompleteTask = async (taskId) => {
        if (!window.confirm('Are you sure you want to complete this task?')) return;
        try {
            await caseService.completeTask(taskId);
            notify('Task completed successfully', 'success');
            loadCaseData();
        } catch (err) {
            notify('Failed to complete task: ' + err.message, 'error');
        }
    };

    const handleOpenAssignModal = async () => {
        const statusToRoleMap = {
            'PROCESSING': 'KYC_ANALYST',
            'KYC_ANALYST': 'KYC_ANALYST',
            'REVIEWER_REVIEW': 'KYC_REVIEWER',
            'AFC_REVIEW': 'AFC_REVIEWER',
            'ACO_REVIEW': 'ACO_REVIEWER'
        };

        const role = statusToRoleMap[kycCase.status];

        if (!role) {
            notify(`Cannot assign case in status: ${kycCase.status}`, 'warning');
            return;
        }

        try {
            const users = await caseService.getUsersByRole(role);
            setAssignableUsers(users || []);
            setIsAssignModalOpen(true);
        } catch (err) {
            notify('Failed to load users: ' + err.message, 'error');
        }
    };

    if (loading) return <p className="loading">Loading case details...</p>;
    if (error) return (
        <div className="glass-section" style={{ textAlign: 'center', padding: '3rem' }}>
            <p className="error">{error}</p>
            <Link to="/cases" className="btn btn-secondary" style={{ marginTop: '1rem' }}>Back to Case Management</Link>
        </div>
    );
    if (!kycCase) return <p className="error">Case not found</p>;

    const workflowSteps = ['KYC_ANALYST', 'REVIEWER_REVIEW', 'AFC_REVIEW', 'ACO_REVIEW', 'APPROVED'];

    return (
        <div className="case-details-page">
            {/* Case Hero Header */}
            <div className="hero-header glass-section" style={{ marginBottom: '2rem', padding: '2rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div style={{ display: 'flex', gap: '2rem', alignItems: 'center' }}>
                        <div className="case-id-badge">
                            <span style={{ fontSize: '0.8rem', opacity: 0.7 }}>CASE ID</span>
                            <span style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>#{kycCase.caseID}</span>
                        </div>
                        <div>
                            <h1 style={{ margin: 0, fontSize: '2.5rem', color: '#fff' }}>{kycCase.clientName}</h1>
                            <div style={{ display: 'flex', gap: '1.5rem', marginTop: '0.5rem', color: 'rgba(255,255,255,0.6)' }}>
                                <span><strong>Type:</strong> Onboarding</span>
                                <span><strong>Created:</strong> {new Date(kycCase.createdDate).toLocaleDateString()}</span>
                                <span><strong>Assignee:</strong> {kycCase.assignedTo || 'Unassigned'}</span>
                            </div>
                        </div>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '1rem' }}>
                        <span className={`status-badge-lg ${kycCase.status.toLowerCase()}`}>
                            {kycCase.status.replace(/_/g, ' ')}
                        </span>
                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                            <Link to={`/clients/${kycCase.clientID}`} className="btn-glass">View Client Profile</Link>
                            <Link to="/cases" className="btn-glass">Back to List</Link>
                        </div>
                    </div>
                </div>
            </div>

            {/* Tabbed Navigation */}
            <div className="tabs-container" style={{ marginBottom: '2rem', borderBottom: '1px solid var(--glass-border)' }}>
                <button className={`tab-btn ${activeTab === 'flow' ? 'active' : ''}`} onClick={() => setActiveTab('flow')}>
                    Processing & Decisions
                </button>
                <button className={`tab-btn ${activeTab === 'profile' ? 'active' : ''}`} onClick={() => setActiveTab('profile')}>
                    Risk & Screening
                </button>
                <button className={`tab-btn ${activeTab === 'timeline' ? 'active' : ''}`} onClick={() => setActiveTab('timeline')}>
                    Timeline & Docs
                </button>
                {/* Questionnaire link is special as it's a separate page, but we could list it here if we want to navigate */}
                <Link to={`/cases/${id}/questionnaire`} className="tab-btn" style={{ textDecoration: 'none' }}>
                    Identity Questionnaire ↗
                </Link>
            </div>

            {/* Tab Content */}
            <div className="tab-content">
                {activeTab === 'flow' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
                        {/* Legacy Workflow Steps Visualization */}
                        {kycCase.workflowType !== 'CMMN' && (
                            <section className="glass-section">
                                <h3 style={{ marginBottom: '1.5rem' }}>Process Flow</h3>
                                <div className="workflow-stepper">
                                    {workflowSteps.map((step, index) => (
                                        <React.Fragment key={step}>
                                            <div className={`step-item ${kycCase.status === step ? 'active' : workflowSteps.indexOf(kycCase.status) > index ? 'completed' : ''}`}>
                                                <div className="step-circle">{index + 1}</div>
                                                <span className="step-label">{step.replace(/_/g, ' ')}</span>
                                            </div>
                                            {index < workflowSteps.length - 1 && <div className="step-connector"></div>}
                                        </React.Fragment>
                                    ))}
                                </div>
                            </section>
                        )}

                        {/* Decision Panel */}
                        <section className="glass-section" style={{ borderLeft: '4px solid var(--primary-color)' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                                <h3>Decision Support</h3>
                                <div style={{ display: 'flex', gap: '1rem' }}>
                                    {/* Lifecycle Actions */}
                                    {['KYC_ANALYST', 'REVIEWER_REVIEW', 'AFC_REVIEW', 'ACO_REVIEW'].includes(kycCase.status) && (kycCase.assignedTo === user.username || myTasks.length > 0) && (
                                        <>
                                            <Button onClick={() => handleTransition('APPROVE')} disabled={transitioning} style={{ backgroundColor: '#52c41a', color: '#fff' }}>Approve Case</Button>
                                            <Button onClick={() => handleTransition('REJECT')} disabled={transitioning} style={{ backgroundColor: '#ff4d4f', color: '#fff' }}>Reject Case</Button>
                                        </>
                                    )}

                                    {/* Assignment Controls */}
                                    {['APPROVED', 'REJECTED'].indexOf(kycCase.status) === -1 && (
                                        <>
                                            {kycCase.assignedTo !== user.username && (
                                                <Button variant="secondary" onClick={() => handleAssign(user.username)} disabled={assigning}>Assign to Me</Button>
                                            )}
                                            <Button variant="secondary" onClick={handleOpenAssignModal} disabled={assigning}>Assign To...</Button>
                                        </>
                                    )}
                                </div>
                            </div>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                <label style={{ color: 'var(--text-secondary)', fontWeight: '600' }}>Decision Comment (Required)</label>
                                <textarea
                                    value={commentInput}
                                    onChange={(e) => setCommentInput(e.target.value)}
                                    placeholder="Provide detailed rationale for your decision..."
                                    className="decision-textarea"
                                />
                            </div>
                        </section>

                        {/* Recent Comments */}
                        <section className="glass-section">
                            <h3>Workflow Audit Trail</h3>
                            <div className="comments-list">
                                {comments.map((c, i) => (
                                    <div key={i} className="comment-bubble">
                                        <div className="comment-meta">
                                            <strong>{c.userID || c.userId}</strong>
                                            <span>{new Date(c.commentDate || c.time).toLocaleString()}</span>
                                        </div>
                                        <p className="comment-text">{c.commentText || c.message}</p>
                                    </div>
                                ))}
                                {comments.length === 0 && <p style={{ color: '#666', fontStyle: 'italic' }}>No audit comments recorded.</p>}
                            </div>
                        </section>
                    </div>
                )}

                {activeTab === 'profile' && (
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
                        <section className="glass-section">
                            <h3 style={{ marginBottom: '1.5rem' }}>Client Risk Pulse</h3>
                            {riskHistory.length > 0 ? (
                                <div style={{ textAlign: 'center', padding: '2rem' }}>
                                    <div className="risk-score-large" style={{
                                        color: riskHistory[0].overallRiskLevel === 'HIGH' ? '#ff4d4f' : riskHistory[0].overallRiskLevel === 'MEDIUM' ? '#faad14' : '#52c41a'
                                    }}>
                                        {riskHistory[0].overallRiskScore}
                                    </div>
                                    <div style={{ fontSize: '1.2rem', fontWeight: 'bold', textTransform: 'uppercase', marginBottom: '1rem' }}>
                                        {riskHistory[0].overallRiskLevel} RISK
                                    </div>
                                    <p style={{ color: 'var(--text-secondary)' }}>Last calculated on {new Date(riskHistory[0].calculationDate).toLocaleDateString()}</p>
                                </div>
                            ) : (
                                <div style={{ padding: '3rem', textAlign: 'center', color: '#666' }}>No risk assessments available for this client.</div>
                            )}
                        </section>

                        <section className="glass-section">
                            <h3 style={{ marginBottom: '1.5rem' }}>Screening Verdicts</h3>
                            <ScreeningPanel clientId={kycCase.clientID} hasPermission={true} />
                        </section>

                        <section className="glass-section" style={{ gridColumn: 'span 2' }}>
                            <CaseActions id={id} onActionTriggered={loadCaseData} />
                        </section>
                    </div>
                )}

                {activeTab === 'timeline' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
                        {/* Documents Section */}
                        <section className="glass-section">
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                                <h3>Supporting Documents</h3>
                                {['APPROVED', 'REJECTED'].indexOf(kycCase.status) === -1 && (
                                    <Button onClick={() => setIsDocModalOpen(true)}>Upload New Document</Button>
                                )}
                            </div>
                            <table className="modern-table">
                                <thead>
                                    <tr>
                                        <th>Category</th>
                                        <th>Filename</th>
                                        <th>Version</th>
                                        <th>Uploaded By</th>
                                        <th>Date</th>
                                        <th style={{ textAlign: 'right' }}>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {docs.map(d => (
                                        <tr key={d.documentID}>
                                            <td style={{ fontWeight: '600' }}>{d.category}</td>
                                            <td><a href={`/api/cases/documents/${d.documentID}`} target="_blank" rel="noreferrer" className="doc-link">{d.documentName}</a></td>
                                            <td><span className="status-badge-modern info">v{d.version || 1}</span></td>
                                            <td>{d.uploadedBy}</td>
                                            <td>{new Date(d.uploadDate).toLocaleDateString()}</td>
                                            <td style={{ textAlign: 'right' }}>
                                                <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                                                    <button onClick={() => handleViewHistory(d.documentName)} className="btn-action-sm">History</button>
                                                    <button onClick={() => {
                                                        setUploadData({ ...uploadData, documentName: d.documentName, category: d.category });
                                                        setIsDocModalOpen(true);
                                                    }} className="btn-action-sm">New Version</button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                    {docs.length === 0 && (
                                        <tr>
                                            <td colSpan="6" style={{ textAlign: 'center', padding: '2rem', color: '#666' }}>No documents uploaded.</td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        </section>

                        {/* Timeline */}
                        <section className="glass-section">
                            <h3>Case Lifecycle Timeline</h3>
                            <CaseTimeline items={timeline} />
                        </section>

                        {/* Events log */}
                        <section className="glass-section">
                            <h3>Technical Events Log</h3>
                            <div className="event-log">
                                {events.map(event => (
                                    <div key={event.eventID} className={`event-item ${event.eventType.toLowerCase()}`}>
                                        <div className="event-dot"></div>
                                        <div className="event-content">
                                            <div className="event-header">
                                                <span className="event-type">{event.eventType}</span>
                                                <span className="event-date">{new Date(event.eventDate).toLocaleString()}</span>
                                            </div>
                                            <p className="event-desc">{event.eventDescription}</p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </section>
                    </div>
                )}
            </div>

            {/* Modals remain similar but with updated styling */}
            <Modal isOpen={isHistoryModalOpen} onClose={() => setIsHistoryModalOpen(false)} title="Document Version History" maxWidth="600px">
                {historyLoading ? <p>Loading history...</p> : (
                    <table className="modern-table">
                        <thead>
                            <tr><th>Ver</th><th>Filename</th><th>By</th><th>Date</th></tr>
                        </thead>
                        <tbody>
                            {selectedDocHistory.map(v => (
                                <tr key={v.documentID}>
                                    <td><span className="status-badge-modern info">v{v.version || 1}</span></td>
                                    <td><a href={`/api/cases/documents/${v.documentID}`} target="_blank" rel="noreferrer" className="doc-link">{v.documentName}</a></td>
                                    <td>{v.uploadedBy}</td>
                                    <td>{new Date(v.uploadDate).toLocaleString()}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </Modal>

            <Modal isOpen={isDocModalOpen} onClose={() => setIsDocModalOpen(false)} title="Upload Documentation" maxWidth="450px">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                    <div className="form-group">
                        <label>Select File</label>
                        <input type="file" onChange={(e) => setUploadData({ ...uploadData, file: e.target.files[0] })} style={{ width: '100%' }} />
                    </div>
                    <div className="form-group">
                        <label>Category</label>
                        {uploadData.documentName ? (
                            <div className="info-box">Uploading new version for: <strong>{uploadData.documentName}</strong></div>
                        ) : (
                            <select value={uploadData.category} onChange={(e) => setUploadData({ ...uploadData, category: e.target.value })} className="modern-select">
                                <option value="IDENTIFICATION">Identification</option>
                                <option value="PROOF_OF_ADDRESS">Proof of Address</option>
                                <option value="SOURCE_OF_WEALTH">Source of Wealth</option>
                                <option value="OTHER">Other</option>
                            </select>
                        )}
                    </div>
                    <div className="form-group">
                        <label>Comment</label>
                        <input type="text" value={uploadData.comment} onChange={(e) => setUploadData({ ...uploadData, comment: e.target.value })} placeholder="Internal note..." className="modern-input" />
                    </div>
                    <Button onClick={handleUpload} disabled={uploading}>{uploading ? 'Processing...' : 'Upload Document'}</Button>
                </div>
            </Modal>

            <Modal isOpen={isAssignModalOpen} onClose={() => setIsAssignModalOpen(false)} title={`Reassign Case (${kycCase.status})`} maxWidth="400px">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <p style={{ color: 'var(--text-secondary)' }}>Select a qualified officer for the <strong>{kycCase.status}</strong> step:</p>
                    <div className="assign-list">
                        {assignableUsers.map(u => (
                            <button key={u.username} onClick={() => handleAssign(u.username)} className="assign-item">
                                {u.username} {kycCase.assignedTo === u.username && <span className="current-label">Current</span>}
                            </button>
                        ))}
                    </div>
                </div>
            </Modal>

            <style>{`
                .case-id-badge {
                    background: rgba(255,255,255,0.05);
                    border: 1px solid var(--glass-border);
                    padding: 0.5rem 1rem;
                    border-radius: 12px;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                }
                .status-badge-lg {
                    padding: 0.5rem 1.5rem;
                    border-radius: 30px;
                    font-weight: 700;
                    text-transform: uppercase;
                    letter-spacing: 0.05em;
                    background: rgba(255,255,255,0.1);
                    border: 1px solid var(--glass-border);
                }
                .status-badge-lg.approved { color: #52c41a; border-color: #52c41a40; background: #52c41a10; }
                .status-badge-lg.rejected { color: #ff4d4f; border-color: #ff4d4f40; background: #ff4d4f10; }
                .tab-btn {
                    padding: 1rem 1.5rem;
                    background: none;
                    border: none;
                    color: var(--text-secondary);
                    font-weight: 600;
                    cursor: pointer;
                    transition: all 0.3s;
                    border-bottom: 2px solid transparent;
                }
                .tab-btn.active {
                    color: var(--primary-color);
                    border-bottom-color: var(--primary-color);
                }
                .workflow-stepper {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    padding: 1rem 0;
                }
                .step-item {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    gap: 0.5rem;
                    flex: 1;
                    opacity: 0.4;
                }
                .step-item.active { opacity: 1; }
                .step-item.completed { opacity: 0.8; }
                .step-circle {
                    width: 40px;
                    height: 40px;
                    border-radius: 50%;
                    border: 2px solid var(--glass-border);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-weight: bold;
                }
                .step-item.active .step-circle { background: var(--primary-color); border-color: var(--primary-color); }
                .step-item.completed .step-circle { background: #52c41a; border-color: #52c41a; }
                .step-connector { height: 2px; background: var(--glass-border); flex: 1; margin: 0 10px; margin-top: -25px; }
                .decision-textarea {
                    width: 100%;
                    min-height: 120px;
                    background: rgba(0,0,0,0.2);
                    border: 1px solid var(--glass-border);
                    border-radius: 8px;
                    color: #fff;
                    padding: 1rem;
                    font-family: inherit;
                    resize: vertical;
                }
                .comment-bubble {
                    background: rgba(255,255,255,0.02);
                    padding: 1rem;
                    border-radius: 12px;
                    margin-bottom: 1rem;
                    border: 1px solid var(--glass-border);
                }
                .comment-meta {
                    display: flex;
                    justify-content: space-between;
                    font-size: 0.85rem;
                    color: var(--text-secondary);
                    margin-bottom: 0.5rem;
                }
                .risk-score-large { font-size: 4rem; font-weight: 900; line-height: 1; }
                .event-log { display: flex; flexDirection: column; gap: 1rem; }
                .event-item { display: flex; gap: 1.5rem; position: relative; padding-bottom: 1rem; }
                .event-dot { width: 12px; height: 12px; border-radius: 50%; background: var(--glass-border); margin-top: 5px; z-index: 1; }
                .event-item::after { content: ''; position: absolute; left: 5px; top: 15px; bottom: 0; width: 2px; background: var(--glass-border); }
                .event-item:last-child::after { display: none; }
                .event-content { flex: 1; }
                .event-header { display: flex; justify-content: space-between; margin-bottom: 0.25rem; }
                .event-type { font-weight: 700; font-size: 0.8rem; text-transform: uppercase; color: var(--primary-color); }
                .event-date { font-size: 0.8rem; color: var(--text-secondary); }
                .event-desc { color: #ccc; font-size: 0.95rem; }
                .doc-link { color: var(--primary-color); text-decoration: none; font-weight: 600; }
                .btn-action-sm { background: var(--glass-bg); border: 1px solid var(--glass-border); color: #fff; padding: 4px 10px; border-radius: 6px; font-size: 0.8rem; cursor: pointer; }
                .assign-item { width: 100%; padding: 1rem; background: var(--glass-bg); border: 1px solid var(--glass-border); border-radius: 12px; color: #fff; text-align: left; margin-bottom: 0.5rem; cursor: pointer; display: flex; justify-content: space-between; }
                .current-label { font-size: 0.7rem; color: #52c41a; border: 1px solid #52c41a40; padding: 2px 6px; border-radius: 4px; }
                .btn-glass { background: rgba(255,255,255,0.05); color: #fff; text-decoration: none; padding: 0.5rem 1rem; border-radius: 8px; border: 1px solid var(--glass-border); font-size: 0.9rem; transition: all 0.2s; }
                .btn-glass:hover { background: rgba(255,255,255,0.1); border-color: #fff; }
                .modern-select, .modern-input { width: 100%; padding: 0.75rem; background: var(--glass-bg); border: 1px solid var(--glass-border); border-radius: 8px; color: #fff; }
            `}</style>
        </div>
    );
};

export default CaseDetails;
