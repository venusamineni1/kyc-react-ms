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
                // Filter for tasks belonging to this case (by DB ID or CMMN Instance ID)
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
            loadCaseData(); // Refresh all data
        } catch (err) {
            notify('Failed to complete task: ' + err.message, 'error');
        }
    };

    const handleOpenAssignModal = async () => {
        // Map Case Status to User Role
        const statusToRoleMap = {
            'PROCESSING': 'KYC_ANALYST',
            'KYC_ANALYST': 'KYC_ANALYST', // In case status is already the step name
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
            if (!users || users.length === 0) {
                notify(`No users found with role: ${role}`, 'warning');
                // Fallback: show empty list but still open modal
                setAssignableUsers([]);
            } else {
                setAssignableUsers(users);
            }
            setIsAssignModalOpen(true);
        } catch (err) {
            console.error("Failed to load users for role:", role, err);
            notify('Failed to load users: ' + err.message, 'error');
        }
    };

    if (loading) return <p className="loading">Loading case details...</p>;
    if (error) return <p className="error">{error}</p>;
    if (!kycCase) return <p className="error">Case not found</p>;

    const workflowSteps = ['KYC_ANALYST', 'REVIEWER_REVIEW', 'AFC_REVIEW', 'ACO_REVIEW', 'APPROVED'];

    return (
        <div>
            <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
                <h1 style={{ margin: 0 }}>Case #{kycCase.caseID} - {kycCase.clientName}</h1>
                <div style={{ display: 'flex', gap: '1rem' }}>

                    {/* Lifecycle Actions */}
                    {['KYC_ANALYST', 'REVIEWER_REVIEW', 'AFC_REVIEW', 'ACO_REVIEW'].includes(kycCase.status) && (kycCase.assignedTo === user.username || myTasks.length > 0) && (
                        <>
                            <Button onClick={() => handleTransition('APPROVE')} disabled={transitioning} style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}>Approve</Button>
                            <Button onClick={() => handleTransition('REJECT')} disabled={transitioning} style={{ backgroundColor: '#ff4d4f', borderColor: '#ff4d4f' }}>Reject</Button>
                        </>
                    )}

                    {/* Assignment Controls */}
                    {['APPROVED', 'REJECTED'].indexOf(kycCase.status) === -1 && (
                        <>
                            {kycCase.assignedTo !== user.username && (
                                <Button variant="secondary" onClick={() => handleAssign(user.username)} disabled={assigning}>
                                    Assign to Me
                                </Button>
                            )}
                            <Button variant="secondary" onClick={handleOpenAssignModal} disabled={assigning}>
                                assign to...
                            </Button>
                        </>
                    )}

                    <Link to={`/cases/${id}/questionnaire`} className="btn btn-secondary">Questionnaire</Link>
                    <Link to="/cases" className="back-link">Back to list</Link>
                </div>
            </header>

            {/* Legacy Workflow Steps - Hide for CMMN */}
            {kycCase.workflowType !== 'CMMN' && (
                <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '2rem' }}>
                    {workflowSteps.map((step, index) => (
                        <React.Fragment key={step}>
                            <div
                                className={`workflow-step ${kycCase.status === step ? 'active' : ''}`}
                                style={{
                                    flex: 1, textAlign: 'center', padding: '0.75rem',
                                    background: kycCase.status === step ? 'var(--primary-color)' : 'rgba(255,255,255,0.05)',
                                    border: `1px solid ${kycCase.status === step ? 'var(--primary-color)' : 'var(--glass-border)'}`,
                                    borderRadius: '24px',
                                    transition: 'all 0.3s ease',
                                    fontWeight: kycCase.status === step ? 'bold' : 'normal',
                                    boxShadow: kycCase.status === step ? '0 4px 12px rgba(0,0,0,0.2)' : 'none',
                                    whiteSpace: 'nowrap'
                                }}
                            >
                                {step.replace(/_/g, ' ')}
                            </div>
                            {index < workflowSteps.length - 1 && (
                                <div style={{ display: 'flex', alignItems: 'center', color: 'rgba(255,255,255,0.3)' }}>
                                    <span style={{ fontSize: '1.2rem' }}>→</span>
                                </div>
                            )}
                        </React.Fragment>
                    ))}
                </div>
            )}

            <div style={{ display: 'flex', gap: '2rem', marginBottom: '1.5rem' }}>
                <section className="glass-section" style={{ flex: 1 }}>
                    <h3>Case Information</h3>
                    <div className="case-info-grid">
                        <div className="info-item"><strong>Case ID</strong><span>{kycCase.caseID}</span></div>
                        <div className="info-item"><strong>Client ID</strong><span>{kycCase.clientID}</span></div>
                        <div className="info-item"><strong>Status</strong><span><span className="status-badge">{kycCase.status}</span></span></div>
                        <div className="info-item"><strong>Assigned To</strong><span>{kycCase.assignedTo || 'Unassigned'}</span></div>
                    </div>
                </section>

                <section className="glass-section" style={{ flex: 1, display: 'flex', gap: '1rem', alignItems: 'center', justifyContent: 'space-around' }}>
                    {/* Risk Pulse Miniature */}
                    <div style={{ textAlign: 'center' }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Risk Pulse</h4>
                        {riskHistory.length > 0 ? (
                            <div>
                                <div style={{
                                    fontSize: '2rem',
                                    fontWeight: 'bold',
                                    color: riskHistory[0].overallRiskLevel === 'HIGH' ? '#ff4d4f' : riskHistory[0].overallRiskLevel === 'MEDIUM' ? '#faad14' : '#52c41a',
                                    lineHeight: 1
                                }}>
                                    {riskHistory[0].overallRiskScore}
                                </div>
                                <div style={{
                                    fontSize: '0.8rem',
                                    color: riskHistory[0].overallRiskLevel === 'HIGH' ? '#ff4d4f' : riskHistory[0].overallRiskLevel === 'MEDIUM' ? '#faad14' : '#52c41a'
                                }}>
                                    {riskHistory[0].overallRiskLevel}
                                </div>
                            </div>
                        ) : <div style={{ color: '#666', fontStyle: 'italic' }}>N/A</div>}
                    </div>

                    <div style={{ width: '1px', height: '60px', background: 'var(--glass-border)' }}></div>

                    {/* Screening Panel Compact Integration */}
                    <div style={{ flex: 1 }}>
                        <h4 style={{ margin: '0 0 0.5rem 0', color: 'var(--text-secondary)', fontSize: '0.9rem', textAlign: 'center' }}>Screening</h4>
                        <ScreeningPanel clientId={kycCase.clientID} hasPermission={false} />
                    </div>
                </section>
            </div>

            {/* Case Timeline */}
            <section className="glass-section" style={{ marginBottom: '1.5rem' }}>
                <h3>Case Timeline (CMMN)</h3>
                <CaseTimeline items={timeline} />
            </section>

            {/* Discretionary Actions */}
            <CaseActions id={id} onActionTriggered={loadCaseData} />

            {/* My Active Tasks Section */}
            {/* My Active Tasks Section */}
            <section className="glass-section" style={{ marginTop: '1.5rem', borderLeft: '4px solid #faad14' }}>
                <h3 style={{ color: '#faad14' }}>⚡ My Active Tasks</h3>
                <p style={{ fontSize: '0.9rem', color: '#ccc', marginBottom: '1rem' }}>
                    Tasks assigned to you or your roles that require action.
                </p>
                {myTasks.length > 0 ? (
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                            <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--glass-border)' }}>
                                <th style={{ padding: '0.5rem' }}>Task Name</th>
                                <th style={{ padding: '0.5rem' }}>Created</th>
                                <th style={{ padding: '0.5rem' }}>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            {myTasks.map(task => (
                                <tr key={task.taskId} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                                    <td style={{ padding: '0.5rem' }}>{task.name}</td>
                                    <td style={{ padding: '0.5rem' }}>{new Date(task.createTime).toLocaleString()}</td>
                                    <td style={{ padding: '0.5rem' }}>
                                        <Button onClick={() => handleCompleteTask(task.taskId)}>
                                            Complete
                                        </Button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                ) : (
                    <div style={{ padding: '1rem', textAlign: 'center', color: '#888', fontStyle: 'italic', background: 'rgba(255,255,255,0.05)', borderRadius: '8px' }}>
                        No active tasks currently assigned to you or your groups.
                    </div>
                )}
            </section>

            <section className="glass-section" style={{ marginTop: '1.5rem' }}>
                <h3>Case Events</h3>
                {events && events.length > 0 ? (
                    <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '1rem' }}>
                        <thead>
                            <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--glass-border)' }}>
                                <th style={{ padding: '0.5rem' }}>Date</th>
                                <th style={{ padding: '0.5rem' }}>Type</th>
                                <th style={{ padding: '0.5rem' }}>Description</th>
                                <th style={{ padding: '0.5rem' }}>Source</th>
                            </tr>
                        </thead>
                        <tbody>
                            {events.map(event => (
                                <tr key={event.eventID} style={{
                                    borderBottom: '1px solid rgba(255,255,255,0.05)',
                                    background: event.eventType === 'RISK_CHANGED' ? 'rgba(255, 150, 50, 0.1)' : 'transparent',
                                    borderLeft: event.eventType === 'RISK_CHANGED' ? '3px solid #ffaa00' : 'none'
                                }}>
                                    <td style={{ padding: '0.5rem' }}>{new Date(event.eventDate).toLocaleString()}</td>
                                    <td style={{ padding: '0.5rem' }}>
                                        <span className={`status-badge ${event.eventType === 'RISK_CHANGED' ? 'warning' : 'info'}`}>
                                            {event.eventType}
                                        </span>
                                    </td>
                                    <td style={{ padding: '0.5rem' }}>{event.eventDescription}</td>
                                    <td style={{ padding: '0.5rem' }}>{event.eventSource}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                ) : (
                    <p style={{ color: '#aaa', fontStyle: 'italic' }}>No events recorded for this case.</p>
                )}
            </section>

            <section className="glass-section" style={{ marginTop: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                    <h3 style={{ margin: 0 }}>Documents</h3>
                    {['APPROVED', 'REJECTED'].indexOf(kycCase.status) === -1 && (
                        <Button onClick={() => setIsDocModalOpen(true)}>Upload Document</Button>
                    )}
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Type</th>
                            <th>Name</th>
                            <th>Version</th>
                            <th>Uploaded By</th>
                            <th>Date</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {docs.map(d => (
                            <tr key={d.documentID}>
                                <td>{d.category}</td>
                                <td><a href={`/api/cases/documents/${d.documentID}`} target="_blank" rel="noreferrer">{d.documentName}</a></td>
                                <td><span className="status-badge info">v{d.version || 1}</span></td>
                                <td>{d.uploadedBy}</td>
                                <td>{new Date(d.uploadDate).toLocaleDateString()}</td>
                                <td>
                                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                                        <Button variant="secondary" onClick={() => handleViewHistory(d.documentName)} style={{ fontSize: '0.8rem', padding: '0.2rem 0.5rem' }}>History</Button>
                                        <Button variant="secondary" onClick={() => {
                                            setUploadData({ ...uploadData, documentName: d.documentName, category: d.category });
                                            setIsDocModalOpen(true);
                                        }} style={{ fontSize: '0.8rem', padding: '0.2rem 0.5rem' }}>New Ver</Button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </section>

            {/* Document History Modal */}
            <Modal
                isOpen={isHistoryModalOpen}
                onClose={() => setIsHistoryModalOpen(false)}
                title="Document History"
                maxWidth="600px"
            >
                {historyLoading ? <p>Loading history...</p> : (
                    <table style={{ width: '100%' }}>
                        <thead>
                            <tr style={{ textAlign: 'left' }}>
                                <th>Ver</th>
                                <th>Name</th>
                                <th>Uploaded By</th>
                                <th>Date</th>
                            </tr>
                        </thead>
                        <tbody>
                            {selectedDocHistory.map(v => (
                                <tr key={v.documentID} style={{ borderBottom: '1px solid var(--glass-border)' }}>
                                    <td><span className="status-badge info">v{v.version || 1}</span></td>
                                    <td><a href={`/api/cases/documents/${v.documentID}`} target="_blank" rel="noreferrer">{v.documentName}</a></td>
                                    <td>{v.uploadedBy}</td>
                                    <td>{new Date(v.uploadDate).toLocaleString()}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </Modal>

            {/* Upload Document Modal */}
            <Modal
                isOpen={isDocModalOpen}
                onClose={() => setIsDocModalOpen(false)}
                title="Upload Document"
                maxWidth="450px"
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem' }}>File</label>
                        <input
                            type="file"
                            onChange={(e) => setUploadData({ ...uploadData, file: e.target.files[0] })}
                            style={{ width: '100%', color: 'var(--text-color)' }}
                        />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem' }}>Category</label>
                        {uploadData.documentName ? (
                            <div style={{ padding: '0.5rem', background: 'rgba(255,255,255,0.1)', borderRadius: '4px', fontStyle: 'italic' }}>
                                Uploading new version for: <strong>{uploadData.documentName}</strong> ({uploadData.category})
                            </div>
                        ) : (
                            <select
                                value={uploadData.category}
                                onChange={(e) => setUploadData({ ...uploadData, category: e.target.value })}
                                style={{ width: '100%', padding: '0.5rem', background: 'var(--glass-bg)', color: 'var(--text-color)', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                            >
                                <option value="IDENTIFICATION">Identification</option>
                                <option value="PROOF_OF_ADDRESS">Proof of Address</option>
                                <option value="SOURCE_OF_WEALTH">Source of Wealth</option>
                                <option value="OTHER">Other</option>
                            </select>
                        )}
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem' }}>Comment</label>
                        <input
                            type="text"
                            value={uploadData.comment}
                            onChange={(e) => setUploadData({ ...uploadData, comment: e.target.value })}
                            placeholder="Optional comment"
                            style={{ width: '100%', padding: '0.5rem', background: 'var(--glass-bg)', color: 'var(--text-color)', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                        />
                    </div>
                    <Button onClick={handleUpload} disabled={uploading}>
                        {uploading ? 'Uploading...' : 'Upload'}
                    </Button>
                </div>
            </Modal>
            {/* Comments Section */}
            <section className="glass-section" style={{ marginTop: '1.5rem' }}>
                <h3>Comments & Workflow Notes</h3>
                <div style={{ maxHeight: '300px', overflowY: 'auto', marginBottom: '1rem', background: 'rgba(0,0,0,0.2)', padding: '1rem', borderRadius: '8px' }}>
                    {comments.length > 0 ? (
                        comments.map((c, i) => (
                            <div key={i} style={{ marginBottom: '0.8rem', borderBottom: '1px solid rgba(255,255,255,0.05)', paddingBottom: '0.5rem' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: '#aaa', marginBottom: '0.2rem' }}>
                                    <strong style={{ color: '#fff' }}>{c.userID || c.userId}</strong>
                                    <span>{new Date(c.commentDate || c.time).toLocaleString()}</span>
                                </div>
                                <div style={{ whiteSpace: 'pre-wrap' }}>{c.commentText || c.message}</div>
                            </div>
                        ))
                    ) : (
                        <p style={{ color: '#666', fontStyle: 'italic' }}>No comments yet.</p>
                    )}
                </div>

                {/* Comment Input for Transitions */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    <label style={{ color: '#faad14', fontSize: '0.9rem', fontWeight: 'bold' }}>
                        Workflow Comment (Required for Approve/Reject):
                    </label>
                    <textarea
                        value={commentInput}
                        onChange={(e) => setCommentInput(e.target.value)}
                        placeholder="Type your comment here before clicking Approve or Reject..."
                        style={{
                            width: '100%',
                            padding: '0.75rem',
                            borderRadius: '8px',
                            border: '1px solid var(--glass-border)',
                            background: 'var(--glass-bg)',
                            color: 'var(--text-color)',
                            minHeight: '80px'
                        }}
                    />
                </div>
            </section>

            {/* Assignment Modal */}
            <Modal
                isOpen={isAssignModalOpen}
                onClose={() => setIsAssignModalOpen(false)}
                title={`Assign Case (${kycCase.status})`}
                maxWidth="400px"
            >
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                    <p style={{ color: '#ccc' }}>Select a user from the <strong>{kycCase.status}</strong> group:</p>
                    <div style={{ maxHeight: '300px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                        {assignableUsers.map(u => (
                            <button
                                key={u.username}
                                onClick={() => handleAssign(u.username)}
                                disabled={assigning}
                                style={{
                                    padding: '0.75rem',
                                    background: 'var(--glass-bg)',
                                    border: '1px solid var(--glass-border)',
                                    borderRadius: '0.5rem',
                                    color: 'var(--text-color)',
                                    cursor: 'pointer',
                                    textAlign: 'left',
                                    display: 'flex',
                                    justifyContent: 'space-between'
                                }}
                            >
                                <span>{u.username}</span>
                                {kycCase.assignedTo === u.username && <span style={{ fontSize: '0.8rem', color: '#6ee7b7' }}>Current</span>}
                            </button>
                        ))}
                        {assignableUsers.length === 0 && <p style={{ color: '#999' }}>No eligible users found.</p>}
                    </div>
                </div>
            </Modal>
        </div>
    );
};

export default CaseDetails;
