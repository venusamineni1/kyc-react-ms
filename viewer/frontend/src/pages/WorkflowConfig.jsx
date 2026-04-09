import React, { useState, useEffect } from 'react';
import Button from '../components/Button';
import WorkflowDiagram from '../components/WorkflowDiagram';
import { caseService } from '../services/caseService';
import { useNotification } from '../contexts/NotificationContext';

const AVAILABLE_ROLES = [
    { value: 'ROLE_KYC_ANALYST', label: 'KYC Analyst' },
    { value: 'ROLE_KYC_REVIEWER', label: 'KYC Reviewer' },
    { value: 'ROLE_AFC_REVIEWER', label: 'AFC Reviewer' },
    { value: 'ROLE_ACO_REVIEWER', label: 'ACO Reviewer' },
];

const WorkflowConfig = () => {
    const { notify } = useNotification();
    const [config, setConfig] = useState(null);
    const [loading, setLoading] = useState(true);
    const [deploying, setDeploying] = useState(false);
    const [error, setError] = useState(null);
    const [diagramOpen, setDiagramOpen] = useState(true);

    const fetchConfig = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await caseService.getWorkflowDefinition();
            setConfig(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchConfig();
    }, []);

    const handleDeploy = async () => {
        if (!window.confirm(
            'Deploy the updated workflow? In-flight cases continue with the current version. ' +
            'New cases will use this configuration.'
        )) return;

        setDeploying(true);
        try {
            await caseService.deployWorkflowConfig(config);
            notify('Workflow deployed successfully. New cases will use the updated definition.', 'success');
            await fetchConfig(); // Reload to show new version number
        } catch (err) {
            notify('Deployment failed: ' + err.message, 'error');
        } finally {
            setDeploying(false);
        }
    };

    // --- Stage handlers ---

    const updateStage = (index, field, value) => {
        setConfig(prev => {
            const stages = [...prev.stages];
            stages[index] = { ...stages[index], [field]: value };
            return { ...prev, stages };
        });
    };

    const moveStage = (index, direction) => {
        setConfig(prev => {
            const stages = [...prev.stages];
            const target = index + direction;
            if (target < 0 || target >= stages.length) return prev;
            [stages[index], stages[target]] = [stages[target], stages[index]];
            return {
                ...prev,
                stages: stages.map((s, i) => ({ ...s, order: i }))
            };
        });
    };

    const addStage = () => {
        setConfig(prev => {
            const newKey = 'ht_stage' + (prev.stages.length + 1);
            const newStage = {
                taskDefinitionKey: newKey,
                name: 'New Stage',
                candidateGroup: 'ROLE_KYC_ANALYST',
                order: prev.stages.length,
            };
            return { ...prev, stages: [...prev.stages, newStage] };
        });
    };

    const removeStage = (index) => {
        setConfig(prev => {
            const stages = prev.stages.filter((_, i) => i !== index)
                .map((s, i) => ({ ...s, order: i }));
            return { ...prev, stages };
        });
    };

    // --- Discretionary action handlers ---

    const updateAction = (index, field, value) => {
        setConfig(prev => {
            const actions = [...prev.discretionaryActions];
            actions[index] = { ...actions[index], [field]: value };
            return { ...prev, discretionaryActions: actions };
        });
    };

    if (loading) return <div className="glass-section"><p className="loading">Loading workflow definition...</p></div>;
    if (error) return <div className="glass-section"><p className="error">Error: {error}</p></div>;
    if (!config) return null;

    const builtInKeys = new Set([
        'ht_analystReview', 'ht_reviewerReview', 'ht_afcStandardReview', 'ht_acoReview'
    ]);

    return (
        <div className="glass-section">
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.5rem' }}>
                <div>
                    <h2 style={{ margin: 0 }}>Workflow Configuration</h2>
                    <p style={{ margin: '0.25rem 0 0', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                        {config.caseName}
                        {config.version > 0 && (
                            <span style={{ marginLeft: '0.75rem' }}>
                                <span style={{
                                    background: 'var(--hover-bg)', border: '1px solid var(--glass-border)',
                                    borderRadius: '4px', padding: '1px 6px', fontSize: '0.75rem'
                                }}>
                                    v{config.version}
                                </span>
                            </span>
                        )}
                    </p>
                </div>
                <Button onClick={handleDeploy} disabled={deploying}>
                    {deploying ? 'Deploying...' : 'Deploy Changes'}
                </Button>
            </div>

            {/* Warning banner */}
            <div style={{
                background: 'rgba(234, 179, 8, 0.1)', border: '1px solid rgba(234, 179, 8, 0.3)',
                borderRadius: '6px', padding: '0.6rem 1rem', marginBottom: '1.5rem',
                fontSize: '0.82rem', color: 'var(--text-color)'
            }}>
                <strong>Note:</strong> Built-in stage keys are locked to preserve status tracking.
                You can rename stages and change role assignments freely.
                Adding or reordering stages requires corresponding updates to the status transition logic.
            </div>

            {/* Workflow Diagram */}
            <section style={{ marginBottom: '2rem' }}>
                <button
                    onClick={() => setDiagramOpen(o => !o)}
                    style={{
                        display: 'flex', alignItems: 'center', gap: '0.5rem',
                        background: 'none', border: 'none', cursor: 'pointer',
                        color: 'var(--text-color)', fontSize: '1rem', fontWeight: 600,
                        padding: 0, marginBottom: '0.75rem'
                    }}
                >
                    <span style={{
                        display: 'inline-block', transition: 'transform 0.2s',
                        transform: diagramOpen ? 'rotate(90deg)' : 'rotate(0deg)'
                    }}>▶</span>
                    Workflow Diagram
                </button>

                {diagramOpen && (
                    <div style={{
                        background: 'var(--hover-bg)', border: '1px solid var(--glass-border)',
                        borderRadius: '8px', padding: '1.25rem'
                    }}>
                        <WorkflowDiagram config={config} />
                    </div>
                )}
            </section>

            {/* Sequential Stages */}
            <section style={{ marginBottom: '2rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                    <h3 style={{ margin: 0 }}>Review Stages</h3>
                    <Button variant="secondary" onClick={addStage} style={{ fontSize: '0.8rem', padding: '0.3rem 0.8rem' }}>
                        + Add Stage
                    </Button>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                    {config.stages.map((stage, index) => (
                        <div key={stage.taskDefinitionKey} style={{
                            display: 'grid', gridTemplateColumns: '2rem 1fr 1fr auto auto',
                            gap: '0.75rem', alignItems: 'center',
                            background: 'var(--hover-bg)', border: '1px solid var(--glass-border)',
                            borderRadius: '6px', padding: '0.75rem 1rem'
                        }}>
                            {/* Order number */}
                            <span style={{
                                width: '24px', height: '24px', borderRadius: '50%',
                                background: 'var(--primary-color)', color: '#fff',
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                                fontSize: '0.75rem', fontWeight: 700, flexShrink: 0
                            }}>
                                {index + 1}
                            </span>

                            {/* Stage name */}
                            <div>
                                <label style={{ display: 'block', fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: '2px' }}>
                                    Stage Name
                                </label>
                                <input
                                    type="text"
                                    value={stage.name}
                                    onChange={e => updateStage(index, 'name', e.target.value)}
                                    style={{ width: '100%', padding: '0.3rem 0.5rem', background: 'var(--bg-color)', color: 'var(--text-color)', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                                />
                                <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                                    key: {stage.taskDefinitionKey}
                                </span>
                            </div>

                            {/* Role assignment */}
                            <div>
                                <label style={{ display: 'block', fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: '2px' }}>
                                    Assigned Role
                                </label>
                                <select
                                    value={stage.candidateGroup}
                                    onChange={e => updateStage(index, 'candidateGroup', e.target.value)}
                                    style={{ width: '100%', padding: '0.3rem 0.5rem', background: 'var(--hover-bg)', color: 'var(--text-color)', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                                >
                                    {AVAILABLE_ROLES.map(r => (
                                        <option key={r.value} value={r.value}>{r.label}</option>
                                    ))}
                                </select>
                            </div>

                            {/* Reorder buttons */}
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                                <button
                                    onClick={() => moveStage(index, -1)}
                                    disabled={index === 0}
                                    title="Move up"
                                    style={{ background: 'none', border: '1px solid var(--glass-border)', borderRadius: '3px', color: 'var(--text-color)', cursor: 'pointer', padding: '1px 5px', opacity: index === 0 ? 0.3 : 1 }}
                                >▲</button>
                                <button
                                    onClick={() => moveStage(index, 1)}
                                    disabled={index === config.stages.length - 1}
                                    title="Move down"
                                    style={{ background: 'none', border: '1px solid var(--glass-border)', borderRadius: '3px', color: 'var(--text-color)', cursor: 'pointer', padding: '1px 5px', opacity: index === config.stages.length - 1 ? 0.3 : 1 }}
                                >▼</button>
                            </div>

                            {/* Remove button (only for non-built-in stages) */}
                            <div>
                                {!builtInKeys.has(stage.taskDefinitionKey) ? (
                                    <Button
                                        variant="danger"
                                        onClick={() => removeStage(index)}
                                        style={{ fontSize: '0.75rem', padding: '0.25rem 0.6rem' }}
                                    >
                                        Remove
                                    </Button>
                                ) : (
                                    <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>built-in</span>
                                )}
                            </div>
                        </div>
                    ))}
                </div>

            </section>

            {/* Discretionary Actions */}
            {config.discretionaryActions && config.discretionaryActions.length > 0 && (
                <section>
                    <h3 style={{ marginBottom: '1rem' }}>Discretionary Actions</h3>
                    <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
                        These actions can be triggered manually at any point during case review.
                    </p>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                        {config.discretionaryActions.map((action, index) => (
                            <div key={action.eventListenerKey} style={{
                                background: 'var(--hover-bg)', border: `1px solid ${action.enabled ? 'var(--primary-color)' : 'var(--glass-border)'}`,
                                borderRadius: '6px', padding: '0.75rem 1rem',
                                opacity: action.enabled ? 1 : 0.6
                            }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem' }}>
                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', flex: 1 }}>
                                        {/* Task name */}
                                        <div>
                                            <label style={{ display: 'block', fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: '2px' }}>
                                                Task Name
                                            </label>
                                            <input
                                                type="text"
                                                value={action.name}
                                                onChange={e => updateAction(index, 'name', e.target.value)}
                                                disabled={!action.enabled}
                                                style={{ width: '100%', padding: '0.3rem 0.5rem', background: 'var(--bg-color)', color: 'var(--text-color)', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                                            />
                                            <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                                                trigger: {action.eventListenerKey}
                                            </span>
                                        </div>

                                        {/* Role */}
                                        <div>
                                            <label style={{ display: 'block', fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: '2px' }}>
                                                Assigned Role(s)
                                            </label>
                                            <input
                                                type="text"
                                                value={action.candidateGroup}
                                                onChange={e => updateAction(index, 'candidateGroup', e.target.value)}
                                                disabled={!action.enabled || action.taskKey === 'ht_clientComm'}
                                                placeholder={action.taskKey === 'ht_clientComm' ? 'Assigned to initiator' : 'e.g. ROLE_AFC_REVIEWER'}
                                                style={{ width: '100%', padding: '0.3rem 0.5rem', background: 'var(--bg-color)', color: 'var(--text-color)', border: '1px solid var(--glass-border)', borderRadius: '4px' }}
                                            />
                                            {action.taskKey === 'ht_clientComm' && (
                                                <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>
                                                    Automatically assigned to the requesting user
                                                </span>
                                            )}
                                        </div>
                                    </div>

                                    {/* Enable/disable toggle */}
                                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px', flexShrink: 0 }}>
                                        <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                                            {action.enabled ? 'Enabled' : 'Disabled'}
                                        </span>
                                        <button
                                            onClick={() => updateAction(index, 'enabled', !action.enabled)}
                                            style={{
                                                width: '36px', height: '20px', borderRadius: '10px',
                                                border: 'none', cursor: 'pointer', position: 'relative',
                                                background: action.enabled ? 'var(--primary-color)' : 'var(--glass-border)',
                                                transition: 'background 0.2s'
                                            }}
                                        >
                                            <span style={{
                                                position: 'absolute', top: '2px',
                                                left: action.enabled ? '18px' : '2px',
                                                width: '16px', height: '16px',
                                                background: '#fff', borderRadius: '50%',
                                                transition: 'left 0.2s'
                                            }} />
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </section>
            )}

            {/* Deploy footer */}
            <div style={{ marginTop: '2rem', paddingTop: '1rem', borderTop: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'flex-end' }}>
                <Button onClick={handleDeploy} disabled={deploying}>
                    {deploying ? 'Deploying...' : 'Deploy Changes'}
                </Button>
            </div>
        </div>
    );
};

export default WorkflowConfig;
