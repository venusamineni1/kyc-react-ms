import React, { useEffect, useState } from 'react';
import { FiChevronDown, FiChevronUp } from 'react-icons/fi';
import { caseService } from '../services/caseService';
import WorkflowDiagram from './WorkflowDiagram';

const TERMINAL_STATES = new Set(['completed', 'terminated']);
const ACTIVE_STATES = new Set(['active']);

const LiveWorkflowDiagram = ({ caseId }) => {
    const [config, setConfig] = useState(null);
    const [currentStageKeys, setCurrentStageKeys] = useState(new Set());
    const [completedStageKeys, setCompletedStageKeys] = useState(new Set());
    const [availableActionKeys, setAvailableActionKeys] = useState(new Set());
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [expanded, setExpanded] = useState(false);

    useEffect(() => {
        let cancelled = false;

        const load = async () => {
            setLoading(true);
            setError(null);
            try {
                const [definition, timeline, actions] = await Promise.all([
                    caseService.getWorkflowDefinition(),
                    caseService.getCaseTimeline(caseId),
                    caseService.getCaseActions(caseId),
                ]);
                if (cancelled) return;

                setConfig(definition);
                setCurrentStageKeys(new Set(
                    (timeline || [])
                        .filter(item => ACTIVE_STATES.has((item.status || '').toLowerCase()))
                        .map(item => item.taskDefinitionKey)
                        .filter(Boolean)
                ));
                setCompletedStageKeys(new Set(
                    (timeline || [])
                        .filter(item => TERMINAL_STATES.has((item.status || '').toLowerCase()))
                        .map(item => item.taskDefinitionKey)
                        .filter(Boolean)
                ));
                setAvailableActionKeys(new Set(
                    (actions || []).map(a => a.definitionId).filter(Boolean)
                ));
            } catch (e) {
                if (!cancelled) setError(e.message || 'Failed to load workflow state');
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        if (caseId) load();
        return () => { cancelled = true; };
    }, [caseId]);

    if (loading) {
        return <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem', padding: '0.5rem 0' }}>Loading workflow…</div>;
    }
    if (error) {
        return <div style={{ color: 'var(--danger-color, #ef4444)', fontSize: '0.85rem', padding: '0.5rem 0' }}>Could not load workflow state: {error}</div>;
    }
    if (!config || !config.stages || config.stages.length === 0) {
        return null;
    }

    const currentStage = config.stages.find(s => currentStageKeys.has(s.taskDefinitionKey));
    const isDone = config.stages.length > 0 && config.stages.every(s => completedStageKeys.has(s.taskDefinitionKey));
    const summaryLabel = isDone ? 'Finalized' : currentStage ? currentStage.name : 'Not started';
    const availableCount = availableActionKeys.size;

    return (
        <div>
            <button
                onClick={() => setExpanded(e => !e)}
                style={{
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    width: '100%', background: 'rgba(59,130,246,0.08)', border: '1px solid var(--glass-border)',
                    borderRadius: '8px', padding: '0.75rem 1rem', cursor: 'pointer', color: 'var(--text-color)',
                }}
                aria-expanded={expanded}
            >
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                    <span style={{
                        display: 'inline-block', width: '8px', height: '8px', borderRadius: '50%',
                        background: isDone ? '#22c55e' : '#3b82f6',
                    }} />
                    <span style={{ fontWeight: 600 }}>Current stage: {summaryLabel}</span>
                    {availableCount > 0 && (
                        <span style={{ fontSize: '0.75rem', color: '#fbbf24', fontWeight: 600 }}>
                            · {availableCount} action{availableCount > 1 ? 's' : ''} available
                        </span>
                    )}
                </span>
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    {expanded ? 'Hide diagram' : 'Show diagram'}
                    {expanded ? <FiChevronUp /> : <FiChevronDown />}
                </span>
            </button>

            {expanded && (
                <div style={{ marginTop: '1rem' }}>
                    <WorkflowDiagram
                        config={config}
                        currentStageKeys={currentStageKeys}
                        completedStageKeys={completedStageKeys}
                        availableActionKeys={availableActionKeys}
                    />
                </div>
            )}
        </div>
    );
};

export default LiveWorkflowDiagram;
