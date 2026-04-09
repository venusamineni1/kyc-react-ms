import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis, ResponsiveContainer, Tooltip } from 'recharts';
import { caseService } from '../services/caseService';
import { riskService } from '../services/riskService';
import { useAuth } from '../contexts/AuthContext';
import Button from '../components/Button';
import Modal from '../components/Modal';
import { useNotification } from '../contexts/NotificationContext';
import CaseTimeline from '../components/CaseTimeline';
import CaseActions from '../components/CaseActions';
import ScreeningPanel from '../components/ScreeningPanel';
import DocumentViewer from '../components/DocumentViewer';
import OcrDataPanel from '../components/OcrDataPanel';
import FraudSignalsPanel from '../components/FraudSignalsPanel';

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
    const [riskDetails, setRiskDetails] = useState([]);
    const [myTasks, setMyTasks] = useState([]);
    const [activeTab, setActiveTab] = useState('flow');

    // Document viewer modal state
    const [isViewerModalOpen, setIsViewerModalOpen] = useState(false);
    const [viewerModalDoc, setViewerModalDoc] = useState(null);

    const handleOpenViewer = (doc) => {
        setViewerModalDoc(doc);
        setIsViewerModalOpen(true);
    };

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
    const [uploadProgress, setUploadProgress] = useState(0);
    const [dragOver, setDragOver] = useState(false);

    // Assignment States
    const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
    const [assignableUsers, setAssignableUsers] = useState([]);
    const [selectedAssignee, setSelectedAssignee] = useState('');
    const [assigning, setAssigning] = useState(false);
    const [runningRisk, setRunningRisk] = useState(false);
 
    // Task Completion States
    const [isCompleteModalOpen, setIsCompleteModalOpen] = useState(false);
    const [taskToComplete, setTaskToComplete] = useState(null);
    const [isCompleting, setIsCompleting] = useState(false);

    // Rework / Cancel / Finalize modal states
    const [isReworkModalOpen, setIsReworkModalOpen] = useState(false);
    const [reworkComment, setReworkComment] = useState('');
    const [isReworking, setIsReworking] = useState(false);
    const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
    const [cancelComment, setCancelComment] = useState('');
    const [isCancelling, setIsCancelling] = useState(false);
    const [isFinalizeModalOpen, setIsFinalizeModalOpen] = useState(false);
    const [finalizeDecision, setFinalizeDecision] = useState('APPROVE'); // 'APPROVE' | 'REJECT'
    const [isFinalizing, setIsFinalizing] = useState(false);

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
                    if (riskData && riskData.length > 0 && riskData[0].assessmentID) {
                        try {
                            const details = await riskService.getAssessmentDetails(riskData[0].assessmentID);
                            setRiskDetails(details || []);
                        } catch { setRiskDetails([]); }
                    }
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

            // Fetch Active Tasks directly for this Case
            try {
                const caseTasks = await caseService.getCaseTasks(id);
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


    const handleUpload = () => {
        if (!uploadData.file) return notify('Please select a file', 'warning');
        setUploading(true);
        setUploadProgress(0);

        const formData = new FormData();
        formData.append('file', uploadData.file);
        formData.append('category', uploadData.category);
        formData.append('comment', uploadData.comment);
        formData.append('uploadedBy', user.username);
        if (uploadData.documentName) formData.append('documentName', uploadData.documentName);

        const token = localStorage.getItem('token');
        const xhr = new XMLHttpRequest();
        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) setUploadProgress(Math.round((e.loaded / e.total) * 100));
        };
        xhr.onload = () => {
            setUploading(false);
            setUploadProgress(0);
            if (xhr.status >= 200 && xhr.status < 300) {
                setIsDocModalOpen(false);
                setUploadData({ file: null, category: 'IDENTIFICATION', comment: '', documentName: '' });
                loadCaseData();
                notify('Document uploaded successfully', 'success');
            } else {
                notify('Upload failed: ' + xhr.statusText, 'error');
            }
        };
        xhr.onerror = () => {
            setUploading(false);
            setUploadProgress(0);
            notify('Upload failed: network error', 'error');
        };
        xhr.open('POST', `/api/cases/${id}/documents`);
        if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
        xhr.send(formData);
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

    const handleConfirmTaskCompletion = async () => {
        if (!taskToComplete) return;
        setIsCompleting(true);
        try {
            await caseService.completeTask(taskToComplete.taskId);
            notify('Task completed successfully', 'success');
            setIsCompleteModalOpen(false);
            setTaskToComplete(null);
            loadCaseData();
        } catch (err) {
            notify('Failed to complete task: ' + err.message, 'error');
        } finally {
            setIsCompleting(false);
        }
    };

    const handleOpenCompleteModal = (task) => {
        setTaskToComplete(task);
        setIsCompleteModalOpen(true);
    };

    const handleRecalculateRisk = async () => {
        if (runningRisk) return;
        setRunningRisk(true);
        try {
            await riskService.calculateRisk(kycCase.clientID);
            notify('Risk Recalculation Successful', 'success');
            // Reload risk history and latest assessment
            const riskData = await riskService.getRiskHistory(kycCase.clientID);
            setRiskHistory(riskData);
            if (riskData && riskData.length > 0 && riskData[0].assessmentID) {
                try {
                    const details = await riskService.getAssessmentDetails(riskData[0].assessmentID);
                    setRiskDetails(details || []);
                } catch { setRiskDetails([]); }
            }
        } catch (err) {
            notify('Risk Calculation Failed: ' + err.message, 'error');
        } finally {
            setRunningRisk(false);
        }
    };

    const handleRework = async () => {
        if (!reworkComment.trim()) return notify('Rework reason is required', 'warning');
        setIsReworking(true);
        try {
            await caseService.reworkCase(id, reworkComment);
            notify('Case sent back to Analyst for rework', 'success');
            setIsReworkModalOpen(false);
            setReworkComment('');
            loadCaseData();
        } catch (err) {
            notify('Rework failed: ' + err.message, 'error');
        } finally {
            setIsReworking(false);
        }
    };

    const handleCancel = async () => {
        setIsCancelling(true);
        try {
            await caseService.cancelCase(id, cancelComment);
            notify('Case cancelled', 'success');
            setIsCancelModalOpen(false);
            setCancelComment('');
            loadCaseData();
        } catch (err) {
            notify('Cancel failed: ' + err.message, 'error');
        } finally {
            setIsCancelling(false);
        }
    };

    const handleFinalize = async () => {
        if (!commentInput.trim()) return notify('Comment is required to finalize', 'warning');
        setIsFinalizing(true);
        try {
            await caseService.transitionCase(id, finalizeDecision, commentInput);
            notify(`Case finalized: ${finalizeDecision}`, 'success');
            setIsFinalizeModalOpen(false);
            setCommentInput('');
            loadCaseData();
        } catch (err) {
            notify('Finalize failed: ' + err.message, 'error');
        } finally {
            setIsFinalizing(false);
        }
    };

    // Determine which workflow actions are available for the current user on this case
    const isActiveParticipant = kycCase && (kycCase.assignedTo === user?.username || myTasks.length > 0);
    const isAnalystStage   = kycCase?.status === 'KYC_ANALYST';
    const isReviewerStage  = kycCase?.status === 'REVIEWER_REVIEW';
    const isAfcStage       = kycCase?.status === 'AFC_REVIEW';
    const isAcoStage       = kycCase?.status === 'ACO_REVIEW';
    const isActiveStage    = isAnalystStage || isReviewerStage || isAfcStage || isAcoStage;
    const canSubmit    = isActiveParticipant && isActiveStage && !isAcoStage;
    const canRework    = isActiveParticipant && (isReviewerStage || isAfcStage || isAcoStage);
    const canFinalize  = isActiveParticipant && (isReviewerStage || isAfcStage || isAcoStage);
    const canCancel    = isActiveParticipant && isAnalystStage;

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
                                <span><strong>Type:</strong> {kycCase.reason || 'Onboarding'}</span>
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
                    Workflow & Documents
                </button>
                <button className={`tab-btn ${activeTab === 'profile' ? 'active' : ''}`} onClick={() => setActiveTab('profile')}>
                    Risk & Screening
                </button>
                <button className={`tab-btn ${activeTab === 'timeline' ? 'active' : ''}`} onClick={() => setActiveTab('timeline')}>
                    Timeline & Actions
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
                                <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                                    {/* Assignment Controls */}
                                    {isActiveStage && (
                                        <>
                                            {kycCase.assignedTo !== user.username && (
                                                <Button variant="secondary" onClick={() => handleAssign(user.username)} disabled={assigning}>Assign to Me</Button>
                                            )}
                                            <Button variant="secondary" onClick={handleOpenAssignModal} disabled={assigning}>Assign To...</Button>
                                        </>
                                    )}

                                    {/* Analyst: Submit + Cancel */}
                                    {canSubmit && isAnalystStage && (
                                        <Button onClick={() => handleTransition('SUBMIT')} disabled={transitioning}
                                            style={{ backgroundColor: '#3b82f6', color: '#fff' }}>
                                            Submit to Reviewer
                                        </Button>
                                    )}
                                    {canCancel && (
                                        <Button onClick={() => setIsCancelModalOpen(true)} disabled={transitioning}
                                            style={{ backgroundColor: '#6b7280', color: '#fff' }}>
                                            Cancel Case
                                        </Button>
                                    )}

                                    {/* Reviewer / AFC: Submit to next stage */}
                                    {canSubmit && isReviewerStage && (
                                        <Button onClick={() => handleTransition('SUBMIT')} disabled={transitioning}
                                            style={{ backgroundColor: '#3b82f6', color: '#fff' }}>
                                            Submit to AFC
                                        </Button>
                                    )}
                                    {canSubmit && isAfcStage && (
                                        <Button onClick={() => handleTransition('SUBMIT')} disabled={transitioning}
                                            style={{ backgroundColor: '#3b82f6', color: '#fff' }}>
                                            Submit to ACO
                                        </Button>
                                    )}

                                    {/* Rework (Reviewer / AFC / ACO) */}
                                    {canRework && (
                                        <Button onClick={() => setIsReworkModalOpen(true)} disabled={transitioning}
                                            style={{ backgroundColor: '#f59e0b', color: '#fff' }}>
                                            Rework
                                        </Button>
                                    )}

                                    {/* Finalize (Reviewer / AFC / ACO) */}
                                    {canFinalize && (
                                        <Button onClick={() => setIsFinalizeModalOpen(true)} disabled={transitioning}
                                            style={{ backgroundColor: '#22c55e', color: '#fff' }}>
                                            Finalize
                                        </Button>
                                    )}
                                </div>
                            </div>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                <label style={{ color: 'var(--text-secondary)', fontWeight: '600' }}>
                                    {isAnalystStage ? 'Submission Notes' : 'Decision Comment (Required for Finalize)'}
                                </label>
                                <textarea
                                    value={commentInput}
                                    onChange={(e) => setCommentInput(e.target.value)}
                                    placeholder="Provide detailed rationale for your decision..."
                                    className="decision-textarea"
                                />
                            </div>
                        </section>

                        {/* Rework Modal */}
                        <Modal isOpen={isReworkModalOpen} onClose={() => { setIsReworkModalOpen(false); setReworkComment(''); }} title="Send for Rework">
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: 0 }}>
                                    This will terminate the current workflow and restart from Analyst Review.
                                    All comments and documents are preserved.
                                </p>
                                <label style={{ fontWeight: 600 }}>Reason for Rework <span style={{ color: '#ef4444' }}>*</span></label>
                                <textarea
                                    value={reworkComment}
                                    onChange={e => setReworkComment(e.target.value)}
                                    placeholder="Explain what needs to be corrected or re-examined..."
                                    rows={4}
                                    style={{ padding: '0.5rem', background: 'var(--hover-bg)', color: 'var(--text-color)', border: '1px solid var(--glass-border)', borderRadius: '6px', resize: 'vertical' }}
                                />
                                <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                                    <Button variant="secondary" onClick={() => { setIsReworkModalOpen(false); setReworkComment(''); }}>Cancel</Button>
                                    <Button onClick={handleRework} disabled={isReworking || !reworkComment.trim()}
                                        style={{ backgroundColor: '#f59e0b', color: '#fff' }}>
                                        {isReworking ? 'Sending...' : 'Send for Rework'}
                                    </Button>
                                </div>
                            </div>
                        </Modal>

                        {/* Cancel Modal */}
                        <Modal isOpen={isCancelModalOpen} onClose={() => { setIsCancelModalOpen(false); setCancelComment(''); }} title="Cancel Case">
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: 0 }}>
                                    This will permanently cancel the case. This action cannot be undone.
                                </p>
                                <label style={{ fontWeight: 600 }}>Reason (optional)</label>
                                <textarea
                                    value={cancelComment}
                                    onChange={e => setCancelComment(e.target.value)}
                                    placeholder="Optional: provide a reason for cancellation..."
                                    rows={3}
                                    style={{ padding: '0.5rem', background: 'var(--hover-bg)', color: 'var(--text-color)', border: '1px solid var(--glass-border)', borderRadius: '6px', resize: 'vertical' }}
                                />
                                <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                                    <Button variant="secondary" onClick={() => { setIsCancelModalOpen(false); setCancelComment(''); }}>Back</Button>
                                    <Button onClick={handleCancel} disabled={isCancelling}
                                        style={{ backgroundColor: '#ef4444', color: '#fff' }}>
                                        {isCancelling ? 'Cancelling...' : 'Confirm Cancel'}
                                    </Button>
                                </div>
                            </div>
                        </Modal>

                        {/* Finalize Modal */}
                        <Modal isOpen={isFinalizeModalOpen} onClose={() => setIsFinalizeModalOpen(false)} title="Finalize Case">
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: 0 }}>
                                    Select the outcome and confirm. The decision comment in the main panel is required.
                                </p>
                                <div style={{ display: 'flex', gap: '1rem' }}>
                                    <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', flex: 1,
                                        padding: '0.75rem', borderRadius: '6px', border: `2px solid ${finalizeDecision === 'APPROVE' ? '#22c55e' : 'var(--glass-border)'}`,
                                        background: finalizeDecision === 'APPROVE' ? 'rgba(34,197,94,0.1)' : 'var(--hover-bg)' }}>
                                        <input type="radio" name="finalize" value="APPROVE"
                                            checked={finalizeDecision === 'APPROVE'}
                                            onChange={() => setFinalizeDecision('APPROVE')} />
                                        <span style={{ fontWeight: 600, color: '#22c55e' }}>Approve</span>
                                    </label>
                                    <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', flex: 1,
                                        padding: '0.75rem', borderRadius: '6px', border: `2px solid ${finalizeDecision === 'REJECT' ? '#ef4444' : 'var(--glass-border)'}`,
                                        background: finalizeDecision === 'REJECT' ? 'rgba(239,68,68,0.1)' : 'var(--hover-bg)' }}>
                                        <input type="radio" name="finalize" value="REJECT"
                                            checked={finalizeDecision === 'REJECT'}
                                            onChange={() => setFinalizeDecision('REJECT')} />
                                        <span style={{ fontWeight: 600, color: '#ef4444' }}>Reject</span>
                                    </label>
                                </div>
                                {!commentInput.trim() && (
                                    <p style={{ color: '#f59e0b', fontSize: '0.82rem', margin: 0 }}>
                                        A decision comment is required. Please fill in the comment field before finalizing.
                                    </p>
                                )}
                                <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                                    <Button variant="secondary" onClick={() => setIsFinalizeModalOpen(false)}>Back</Button>
                                    <Button onClick={handleFinalize} disabled={isFinalizing || !commentInput.trim()}
                                        style={{ backgroundColor: finalizeDecision === 'APPROVE' ? '#22c55e' : '#ef4444', color: '#fff' }}>
                                        {isFinalizing ? 'Finalizing...' : `Confirm ${finalizeDecision === 'APPROVE' ? 'Approval' : 'Rejection'}`}
                                    </Button>
                                </div>
                            </div>
                        </Modal>

                        {/* Supporting Documents */}
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
                                            <td>
                                                <button
                                                    onClick={() => handleOpenViewer(d)}
                                                    className="doc-link"
                                                    style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, textAlign: 'left' }}
                                                >
                                                    {d.documentName}
                                                </button>
                                            </td>
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
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                                <h3 style={{ margin: 0 }}>Client Risk Pulse</h3>
                                <button
                                    onClick={handleRecalculateRisk}
                                    className={`btn-icon ${runningRisk ? 'spinning' : ''}`}
                                    title="Recalculate Risk"
                                    disabled={runningRisk}
                                    style={{
                                        background: 'rgba(255,255,255,0.1)',
                                        border: '1px solid rgba(255,255,255,0.2)',
                                        borderRadius: '4px',
                                        padding: '4px 8px',
                                        cursor: 'pointer',
                                        color: 'white'
                                    }}
                                >
                                    🔄
                                </button>
                            </div>

                            {riskHistory.length > 0 ? (() => {
                                const latest = riskHistory[0];
                                const levelColor = latest.overallRiskLevel === 'HIGH' ? '#ff4d4f' : latest.overallRiskLevel === 'MEDIUM' ? '#faad14' : '#52c41a';

                                // Build pillar data from assessment details
                                const PILLARS = ['ENTITY', 'INDUSTRY', 'GEOGRAPHIC', 'PRODUCT', 'CHANNEL'];
                                const pillarScores = PILLARS.map(pillar => {
                                    const matches = riskDetails.filter(d => (d.riskType || '').toUpperCase() === pillar);
                                    const avg = matches.length > 0
                                        ? Math.round(matches.reduce((s, d) => s + (d.riskScore || 0), 0) / matches.length)
                                        : 0;
                                    return { pillar: pillar.charAt(0) + pillar.slice(1).toLowerCase(), score: avg };
                                });
                                const hasDetails = riskDetails.length > 0;

                                return (
                                    <div>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem', marginBottom: '1rem' }}>
                                            <div style={{ width: '76px', height: '76px', borderRadius: '50%', flexShrink: 0, border: `5px solid ${levelColor}`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.7rem', fontWeight: 'bold', boxShadow: `inset 0 0 14px ${levelColor}33`, color: 'white' }}>
                                                {latest.overallRiskScore}
                                            </div>
                                            <div>
                                                <div style={{ fontSize: '1rem', fontWeight: 'bold', textTransform: 'uppercase', color: levelColor }}>{latest.overallRiskLevel} RISK</div>
                                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.78rem', marginTop: '0.2rem' }}>
                                                    Last assessed {new Date(latest.calculationDate || latest.createdAt).toLocaleDateString()}
                                                </div>
                                            </div>
                                        </div>

                                        {hasDetails && (
                                            <div style={{ marginTop: '0.5rem' }}>
                                                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.5rem' }}>Risk Pillar Breakdown</div>
                                                <ResponsiveContainer width="100%" height={180}>
                                                    <RadarChart data={pillarScores} margin={{ top: 0, right: 20, bottom: 0, left: 20 }}>
                                                        <PolarGrid stroke="rgba(255,255,255,0.1)" />
                                                        <PolarAngleAxis dataKey="pillar" tick={{ fill: 'rgba(255,255,255,0.6)', fontSize: 11 }} />
                                                        <PolarRadiusAxis angle={30} domain={[0, 100]} tick={false} axisLine={false} />
                                                        <Radar name="Score" dataKey="score" stroke={levelColor} fill={levelColor} fillOpacity={0.25} />
                                                        <Tooltip contentStyle={{ background: 'rgba(0,0,0,0.8)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', color: '#fff', fontSize: '0.8rem' }} formatter={(v) => [v, 'Score']} />
                                                    </RadarChart>
                                                </ResponsiveContainer>
                                            </div>
                                        )}
                                    </div>
                                );
                            })() : (
                                <div style={{ padding: '1rem 0', textAlign: 'center', color: '#666', fontSize: '0.85rem' }}>
                                    No risk assessments available for this client.
                                </div>
                            )}
                        </section>

                        <section className="glass-section">
                            <h3 style={{ marginBottom: '0.75rem' }}>Screening Verdicts</h3>
                            <ScreeningPanel clientId={kycCase.clientID} hasPermission={true} />
                        </section>

                    </div>
                )}

                {activeTab === 'timeline' && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
                        {/* Discretionary Actions */}
                        <section className="glass-section">
                            <CaseActions id={id} onActionTriggered={loadCaseData} />
                        </section>

                        {/* Active Tasks Section */}
                        <section className="glass-section highlight-border">
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                                    <div className="pulse-dot"></div>
                                    <h3 style={{ margin: 0 }}>Active Tasks</h3>
                                </div>
                                <span className="task-count-badge">{myTasks.length}</span>
                            </div>

                            <div className="task-list-simple">
                                {myTasks.length > 0 ? myTasks.map(task => (
                                    <div key={task.taskId} className="task-item-compact">
                                        <div className="task-info">
                                            <span className="task-type-tag">Workflow Task</span>
                                            <h4 className="task-name">{task.name}</h4>
                                            <p className="task-meta">Created on {new Date(task.createTime).toLocaleDateString()}</p>
                                        </div>
                                        <div className="task-actions">
                                            <button
                                                className="btn-primary-sm"
                                                onClick={() => handleOpenCompleteModal(task)}
                                            >
                                                Complete Task
                                            </button>
                                        </div>
                                    </div>
                                )) : (
                                    <div className="empty-state-tasks">
                                        <p>No active tasks assigned to you for this case.</p>
                                    </div>
                                )}
                            </div>
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

            <Modal isOpen={isDocModalOpen} onClose={() => { setIsDocModalOpen(false); setDragOver(false); }} title="Upload Documentation" maxWidth="450px">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                    <div className="form-group">
                        <label>Select File</label>
                        <div
                            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
                            onDragLeave={() => setDragOver(false)}
                            onDrop={(e) => {
                                e.preventDefault();
                                setDragOver(false);
                                const f = e.dataTransfer.files[0];
                                if (f) setUploadData(d => ({ ...d, file: f }));
                            }}
                            style={{
                                border: `2px dashed ${dragOver ? '#4facfe' : 'var(--glass-border)'}`,
                                borderRadius: '8px',
                                padding: '1.5rem',
                                textAlign: 'center',
                                background: dragOver ? 'rgba(79,172,254,0.08)' : 'rgba(255,255,255,0.03)',
                                cursor: 'pointer',
                                transition: 'all 0.2s'
                            }}
                            onClick={() => document.getElementById('doc-file-input').click()}
                        >
                            {uploadData.file ? (
                                <span style={{ color: '#4facfe', fontWeight: 600 }}>{uploadData.file.name}</span>
                            ) : (
                                <span style={{ color: 'var(--text-secondary)' }}>
                                    {dragOver ? 'Drop file here' : 'Drag & drop or click to select'}
                                </span>
                            )}
                            <input id="doc-file-input" type="file" style={{ display: 'none' }}
                                onChange={(e) => setUploadData(d => ({ ...d, file: e.target.files[0] }))} />
                        </div>
                        {uploading && (
                            <div style={{ marginTop: '0.75rem' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
                                    <span>Uploading...</span><span>{uploadProgress}%</span>
                                </div>
                                <div style={{ background: 'rgba(255,255,255,0.1)', borderRadius: '4px', overflow: 'hidden', height: '6px' }}>
                                    <div style={{ width: `${uploadProgress}%`, background: '#4facfe', height: '100%', transition: 'width 0.2s' }} />
                                </div>
                            </div>
                        )}
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

            {/* Document Viewer Modal */}
            <Modal
                isOpen={isViewerModalOpen}
                onClose={() => setIsViewerModalOpen(false)}
                title={viewerModalDoc ? `📄 ${viewerModalDoc.documentName}` : 'Document Viewer'}
                maxWidth="95vw"
                closeOnOutsideClick={false}
            >
                {viewerModalDoc && (
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: '20px', minHeight: '600px' }}>
                        {/* Left: Document Viewer */}
                        <DocumentViewer
                            docs={[viewerModalDoc]}
                            caseId={id}
                            currentUser={user?.username}
                            onUploadNew={() => { setIsViewerModalOpen(false); setIsDocModalOpen(true); }}
                            hideSelector
                        />
                        {/* Right: OCR + Fraud Signals */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', overflowY: 'auto', maxHeight: '680px' }}>
                            <div className="glass-section" style={{ padding: '14px 16px' }}>
                                <h4 style={{ margin: '0 0 12px 0', fontSize: '0.9rem', fontWeight: 700 }}>Extracted Document Data</h4>
                                <OcrDataPanel caseId={id} document={viewerModalDoc} />
                            </div>
                            <div className="glass-section" style={{ padding: '14px 16px' }}>
                                <h4 style={{ margin: '0 0 12px 0', fontSize: '0.9rem', fontWeight: 700 }}>Verification &amp; Fraud Signals</h4>
                                <FraudSignalsPanel caseId={id} document={viewerModalDoc} />
                            </div>
                        </div>
                    </div>
                )}
            </Modal>

            <Modal isOpen={isCompleteModalOpen} onClose={() => setIsCompleteModalOpen(false)} title="Confirm Task Completion" maxWidth="450px">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem' }}>
                        Are you sure you want to complete the task: <strong>{taskToComplete?.name}</strong>?
                    </p>
                    <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', marginTop: '1rem' }}>
                        <Button variant="secondary" onClick={() => setIsCompleteModalOpen(false)}>Cancel</Button>
                        <Button onClick={handleConfirmTaskCompletion} disabled={isCompleting}>
                            {isCompleting ? 'Completing...' : 'Complete Task'}
                        </Button>
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
