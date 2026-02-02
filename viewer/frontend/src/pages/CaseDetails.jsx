import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { caseService } from '../services/caseService';
import { useAuth } from '../contexts/AuthContext';
import Button from '../components/Button';
import Modal from '../components/Modal';
import { useNotification } from '../contexts/NotificationContext';

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

    // Modal States
    const [isDocModalOpen, setIsDocModalOpen] = useState(false);

    const [uploadData, setUploadData] = useState({
        file: null, category: 'IDENTIFICATION', comment: ''
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
            setComments(commentsData);
            setDocs(docsData);
            setEvents(eventsData);

            // Fetch related cases after getting case details
            if (caseData.clientID) {
                const related = await caseService.getCasesByClient(caseData.clientID);
                setRelatedCases(related.filter(c => c.caseID !== parseInt(id)));
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

            await caseService.uploadDocument(id, formData);
            setIsDocModalOpen(false);
            setUploadData({ file: null, category: 'IDENTIFICATION', comment: '' });
            setUploadData({ file: null, category: 'IDENTIFICATION', comment: '' });
            loadCaseData();
            notify('Document uploaded successfully', 'success');
        } catch (err) {
            notify('Upload failed: ' + err.message, 'error');
        } finally {
            setUploading(false);
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

    const handleOpenAssignModal = async () => {
        // Determine role based on current status
        // Heuristic: Status typically matches the Group Name in our simple workflow
        const role = kycCase.status;
        if (!role || role === 'APPROVED' || role === 'REJECTED') {
            notify('Cannot assign closed or invalid cases', 'warning');
            return;
        }

        try {
            const users = await caseService.getUsersByRole(role);
            setAssignableUsers(users);
            setIsAssignModalOpen(true);
        } catch (err) {
            notify('Failed to load users: ' + err.message, 'error');
        }
    };

    if (loading) return <p className="loading">Loading case details...</p>;
    if (error) return <p className="error">{error}</p>;
    if (!kycCase) return <p className="error">Case not found</p>;

    const workflowSteps = ['KYC_ANALYST', 'KYC_REVIEWER', 'AFC_REVIEWER', 'ACO_REVIEWER', 'APPROVED'];

    return (
        <div>
            <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
                <h1 style={{ margin: 0 }}>Case #{kycCase.caseID} - {kycCase.clientName}</h1>
                <div style={{ display: 'flex', gap: '1rem' }}>

                    {/* Assignment Controls */}
                    {['APPROVED', 'REJECTED'].indexOf(kycCase.status) === -1 && (
                        <>
                            {kycCase.assignedTo !== user.username && (
                                <Button variant="secondary" onClick={() => handleAssign(user.username)} disabled={assigning}>
                                    Assign to Me
                                </Button>
                            )}
                            <Button variant="secondary" onClick={handleOpenAssignModal} disabled={assigning}>
                                Assign to...
                            </Button>
                        </>
                    )}

                    <Link to={`/cases/${id}/questionnaire`} className="btn btn-secondary">Questionnaire</Link>
                    <Link to="/cases" className="back-link">Back to list</Link>
                </div>
            </header>

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
                            {step.replace('_', ' ')}
                        </div>
                        {index < workflowSteps.length - 1 && (
                            <div style={{ display: 'flex', alignItems: 'center', color: 'rgba(255,255,255,0.3)' }}>
                                <span style={{ fontSize: '1.2rem' }}>→</span>
                            </div>
                        )}
                    </React.Fragment>
                ))}
            </div>

            <section className="glass-section">
                <h3>Case Information</h3>
                <div className="case-info-grid">
                    <div className="info-item"><strong>Case ID</strong><span>{kycCase.caseID}</span></div>
                    <div className="info-item"><strong>Client ID</strong><span>{kycCase.clientID}</span></div>
                    <div className="info-item"><strong>Status</strong><span><span className="status-badge">{kycCase.status}</span></span></div>
                    <div className="info-item"><strong>Assigned To</strong><span>{kycCase.assignedTo || 'Unassigned'}</span></div>
                </div>
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
                            <th>Uploaded By</th>
                            <th>Date</th>
                        </tr>
                    </thead>
                    <tbody>
                        {docs.map(d => (
                            <tr key={d.documentID}>
                                <td>{d.category}</td>
                                <td><a href={`/api/cases/documents/${d.documentID}`} target="_blank" rel="noreferrer">{d.documentName}</a></td>
                                <td>{d.uploadedBy}</td>
                                <td>{new Date(d.uploadDate).toLocaleDateString()}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </section>

            {relatedCases.length > 0 && (
                <section className="glass-section" style={{ marginTop: '1.5rem' }}>
                    <h3 style={{ marginBottom: '1rem' }}>Related Cases</h3>
                    <table>
                        <thead>
                            <tr>
                                <th>Case ID</th>
                                <th>Reason</th>
                                <th>Status</th>
                                <th>Created</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            {relatedCases.map(rc => (
                                <tr key={rc.caseID}>
                                    <td>{rc.caseID}</td>
                                    <td>{rc.reason}</td>
                                    <td><span className="status-badge">{rc.status}</span></td>
                                    <td>{rc.createdDate ? new Date(rc.createdDate).toLocaleDateString() : '-'}</td>
                                    <td>
                                        <Link to={`/cases/${rc.caseID}`} className="btn btn-secondary" style={{ fontSize: '0.8rem', padding: '0.2rem 0.5rem' }}>View</Link>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </section>
            )}

            <section className="glass-section" style={{ marginTop: '1.5rem' }}>
                <h3>Workflow Actions & Comments</h3>
                <div style={{ maxHeight: '300px', overflowY: 'auto', marginBottom: '1.5rem' }}>
                    {comments.map((c, i) => (
                        <div key={i} style={{ marginBottom: '1rem', borderBottom: '1px solid var(--glass-border)', paddingBottom: '0.5rem' }}>
                            <div style={{ fontSize: '0.8rem', opacity: 0.6 }}>{c.userID} ({c.role}) - {new Date(c.commentDate).toLocaleString()}</div>
                            <div>{c.commentText}</div>
                        </div>
                    ))}
                </div>
                <textarea
                    placeholder="Add a comment..."
                    value={commentInput}
                    onChange={(e) => setCommentInput(e.target.value)}
                    style={{ width: '100%', height: '100px', background: 'rgba(255,255,255,0.05)', color: 'white', border: '1px solid var(--glass-border)', borderRadius: '8px', padding: '0.5rem', marginBottom: '1rem' }}
                />
                {successMessage && <div className="success" style={{ padding: '0.5rem', marginBottom: '1rem', background: 'rgba(0, 255, 0, 0.1)', border: '1px solid #0f0', borderRadius: '4px' }}>{successMessage}</div>}
                <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', alignItems: 'center' }}>
                    {validationError && (
                        <div style={{
                            color: '#ff6b6b',
                            background: 'rgba(255, 0, 0, 0.1)',
                            border: '1px solid #ff6b6b',
                            padding: '0.5rem',
                            borderRadius: '4px',
                            marginRight: 'auto',
                            fontWeight: 'bold',
                            flex: 1
                        }}>
                            ⚠️ {validationError}
                        </div>
                    )}

                    {/* Only show actions if NOT closed AND (assigned to me OR (unassigned AND (my role matches OR I am admin))) */}
                    {['APPROVED', 'REJECTED'].indexOf(kycCase.status) === -1 && (kycCase.assignedTo === user.username || (!kycCase.assignedTo && (user.role === 'ADMIN' || user.role === kycCase.status))) && (
                        <>
                            <Button variant="secondary" style={{ backgroundColor: '#ff5555' }} onClick={() => handleTransition('REJECT')} disabled={!!(transitioning || successMessage)}>Reject</Button>
                            <Button onClick={() => handleTransition('APPROVE')} disabled={!!(transitioning || successMessage)}>{transitioning ? 'Processing...' : 'Approve / Forward'}</Button>
                        </>
                    )}
                </div>
            </section>



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
                            style={{ width: '100%', color: 'white' }}
                        />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem' }}>Category</label>
                        <select
                            value={uploadData.category}
                            onChange={(e) => setUploadData({ ...uploadData, category: e.target.value })}
                            style={{ width: '100%', padding: '0.5rem', background: 'rgba(255,255,255,0.05)', color: 'white', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                        >
                            <option value="IDENTIFICATION">Identification</option>
                            <option value="PROOF_OF_ADDRESS">Proof of Address</option>
                            <option value="SOURCE_OF_WEALTH">Source of Wealth</option>
                            <option value="OTHER">Other</option>
                        </select>
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem' }}>Comment</label>
                        <input
                            type="text"
                            value={uploadData.comment}
                            onChange={(e) => setUploadData({ ...uploadData, comment: e.target.value })}
                            placeholder="Optional comment"
                            style={{ width: '100%', padding: '0.5rem', background: 'rgba(255,255,255,0.05)', color: 'white', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                        />
                    </div>
                    <Button onClick={handleUpload} disabled={uploading}>
                        {uploading ? 'Uploading...' : 'Upload'}
                    </Button>
                </div>
            </Modal>

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
                                    background: 'rgba(255,255,255,0.05)',
                                    border: '1px solid var(--glass-border)',
                                    borderRadius: '0.5rem',
                                    color: 'white',
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
