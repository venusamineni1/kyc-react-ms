import React, { useState, useEffect } from 'react';
import { caseService } from '../services/caseService';
import Button from './Button';
import Modal from './Modal';
import { useNotification } from '../contexts/NotificationContext';

const CaseActions = ({ id, onActionTriggered }) => {
    const [actions, setActions] = useState([]);
    const [loading, setLoading] = useState(false);
    const { notify } = useNotification();

    // Modal states
    const [activeAction, setActiveAction] = useState(null);
    const [formData, setFormData] = useState({});
    const [submitting, setSubmitting] = useState(false);

    const loadActions = async () => {
        try {
            const data = await caseService.getCaseActions(id);
            setActions(data || []);
        } catch (err) {
            console.error("Failed to load discretionary actions:", err);
        }
    };

    useEffect(() => {
        loadActions();
    }, [id]);

    const handleOpenModal = (action) => {
        setActiveAction(action);
        setFormData({});
    };

    const handleTrigger = async () => {
        setSubmitting(true);
        try {
            await caseService.triggerCaseAction(id, activeAction.id, formData);
            notify(`Action "${activeAction.name}" triggered successfully`, 'success');
            setActiveAction(null);
            loadActions();
            if (onActionTriggered) onActionTriggered();
        } catch (err) {
            notify(err.message || 'Failed to trigger action', 'error');
        } finally {
            setSubmitting(false);
        }
    };

    // Defined Discretionary Actions (from CMMN)
    const ALL_ACTIONS = [
        { definitionId: 'evtInitiateCommunication', name: 'Request Additional Documentation' },
        { definitionId: 'evtChallengeScreening', name: 'Challenge Screening Hit' },
        { definitionId: 'evtOverrideRisk', name: 'Override Risk Assessment' }
    ];

    if (loading && actions.length === 0) return <p>Loading actions...</p>;

    return (
        <div className="case-actions-container" style={{ marginTop: '1.5rem' }}>
            <h3 style={{ marginBottom: '1rem' }}>Discretionary Actions</h3>
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
                {ALL_ACTIONS.map(defAction => {
                    // Check if this action is currently available
                    // The backend returns actions with their runtime ID and definitionId
                    const availableAction = actions.find(a => a.definitionId === defAction.definitionId);
                    const isAvailable = !!availableAction;

                    return (
                        <Button
                            key={defAction.definitionId}
                            variant={isAvailable ? "secondary" : "disabled"} // Assuming Button supports variant or we style it
                            disabled={!isAvailable}
                            onClick={() => isAvailable && handleOpenModal(availableAction)}
                            style={{
                                opacity: isAvailable ? 1 : 0.6,
                                cursor: isAvailable ? 'pointer' : 'not-allowed',
                                border: isAvailable ? '1px solid var(--glass-border)' : '1px dashed rgba(255,255,255,0.2)',
                                background: isAvailable ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.2)',
                                color: isAvailable ? 'var(--text-color)' : 'rgba(255,255,255,0.4)',
                                boxShadow: isAvailable ? 'none' : 'inset 0 0 5px rgba(0,0,0,0.2)'
                            }}
                            title={!isAvailable ? "This action is not currently available or has already been triggered" : ""}
                        >
                            {defAction.name}
                        </Button>
                    );
                })}
            </div>

            {activeAction && (
                <Modal
                    isOpen={!!activeAction}
                    onClose={() => setActiveAction(null)}
                    title={activeAction.name}
                    maxWidth="500px"
                >
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        {activeAction.definitionId === 'evtInitiateCommunication' ? (
                            <>
                                <p style={{ color: 'var(--text-secondary)' }}>Request additional information from the client.</p>
                                <div>
                                    <label style={{ display: 'block', marginBottom: '0.5rem' }}>Information Requested</label>
                                    <textarea
                                        value={formData.requestedInfo || ''}
                                        onChange={(e) => setFormData({ ...formData, requestedInfo: e.target.value })}
                                        placeholder="e.g., Proof of Address, UBO Passport"
                                        style={{ width: '100%', height: '80px', padding: '0.5rem', background: 'var(--glass-bg)', color: 'var(--text-color)', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                                    />
                                </div>
                                <div>
                                    <label style={{ display: 'block', marginBottom: '0.5rem' }}>Due Date</label>
                                    <input
                                        type="date"
                                        value={formData.dueDate || ''}
                                        onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
                                        style={{ width: '100%', padding: '0.5rem', background: 'var(--glass-bg)', color: 'var(--text-color)', border: '1px solid var(--glass-border)', borderRadius: '4px', colorScheme: 'dark' }}
                                    />
                                </div>
                            </>
                        ) : (
                            <p style={{ color: 'var(--text-secondary)' }}>Are you sure you want to trigger <strong>{activeAction.name}</strong>?</p>
                        )}

                        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', marginTop: '1rem' }}>
                            <Button variant="secondary" onClick={() => setActiveAction(null)}>Cancel</Button>
                            <Button onClick={handleTrigger} disabled={submitting}>
                                {submitting ? 'Triggering...' : 'Confirm Action'}
                            </Button>
                        </div>
                    </div>
                </Modal>
            )}
        </div>
    );
};

export default CaseActions;
