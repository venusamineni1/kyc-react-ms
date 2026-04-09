import React from 'react';

const ROLE_SHORT = {
    'ROLE_KYC_ANALYST':   'KYC Analyst',
    'ROLE_KYC_REVIEWER':  'KYC Reviewer',
    'ROLE_AFC_REVIEWER':  'AFC Reviewer',
    'ROLE_ACO_REVIEWER':  'ACO Reviewer',
};

const formatRole = (group) => {
    if (!group) return '';
    if (ROLE_SHORT[group]) return ROLE_SHORT[group];
    return group.replace('ROLE_', '').replace(/_/g, ' ')
        .toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
};

const wrapText = (text, maxLen = 14) => {
    const words = text.split(' ');
    const lines = [];
    let current = '';
    for (const w of words) {
        if ((current + ' ' + w).trim().length > maxLen && current) {
            lines.push(current.trim());
            current = w;
        } else {
            current = (current + ' ' + w).trim();
        }
    }
    if (current) lines.push(current);
    return lines;
};

const WorkflowDiagram = ({ config }) => {
    if (!config || !config.stages || config.stages.length === 0) return null;

    const stages = config.stages;
    const N = stages.length;
    const enabledActions = (config.discretionaryActions || []).filter(a => a.enabled);

    // ── Layout constants ──
    const BOX_W  = 130;
    const BOX_H  = 56;
    const BOX_RX = 10;
    const GAP    = 44;

    // Main flow sits at MAIN_Y. Rework arcs go BELOW at REWORK_Y.
    // Cancel arc goes above at CANCEL_Y.
    const MAIN_Y    = 110;
    const REWORK_Y  = MAIN_Y + BOX_H / 2 + 55;  // bottom of rework arc
    const CANCEL_Y  = MAIN_Y - BOX_H / 2 - 50;  // top of cancel arc

    const START_CX = 24;
    const START_R  = 14;

    const stageLeft = (i) => START_CX + START_R + GAP + i * (BOX_W + GAP);
    const stageCX   = (i) => stageLeft(i) + BOX_W / 2;

    // Diamond after last stage
    const DIAM_W = 80;
    const DIAM_H = 54;
    const diamCX = stageLeft(N) + DIAM_W / 2;
    const diamCY = MAIN_Y;

    // Terminals
    const TERM_W  = 100;
    const TERM_H  = 40;
    const TERM_RX = 20;
    const appCX   = diamCX + DIAM_W / 2 + GAP + TERM_W / 2;
    const appCY   = MAIN_Y;
    const rejCX   = diamCX;
    const rejCY   = MAIN_Y + DIAM_H / 2 + 70;

    // Cancel terminal (off first stage, going up)
    const cancelCX = stageCX(0);
    const cancelCY = CANCEL_Y - 20;
    const CANCEL_TERM_W = 90;
    const CANCEL_TERM_H = 36;

    // Discretionary section
    const ACT_ROW_Y  = rejCY + TERM_H / 2 + 70;
    const ACT_BOX_W  = 140;
    const ACT_BOX_H  = 50;
    const ACT_GAP    = 24;
    const ACT_START_X = 20;

    // SVG size
    const svgW = appCX + TERM_W / 2 + 20;
    const svgH = enabledActions.length > 0
        ? ACT_ROW_Y + ACT_BOX_H / 2 + 30
        : rejCY + TERM_H / 2 + 30;

    const diamondPoints = (cx, cy, w, h) =>
        `${cx},${cy - h/2} ${cx + w/2},${cy} ${cx},${cy + h/2} ${cx - w/2},${cy}`;

    // Colors
    const C_STAGE   = '#3b82f6';
    const C_FINAL   = '#6366f1';
    const C_DIAM    = '#f59e0b';
    const C_APPROVE = '#22c55e';
    const C_REJECT  = '#ef4444';
    const C_REWORK  = '#f59e0b';
    const C_CANCEL  = '#6b7280';
    const C_DISC    = '#8b5cf6';
    const C_TEXT    = '#f1f5f9';
    const C_SUB     = '#94a3b8';
    const C_ARROW   = '#64748b';

    // IDs for marker defs (unique per instance to avoid SVG conflicts)
    const uid = Math.random().toString(36).slice(2, 7);
    const M_MAIN   = `am-${uid}`;
    const M_OK     = `ao-${uid}`;
    const M_FAIL   = `af-${uid}`;
    const M_REWORK = `ar-${uid}`;
    const M_CANCEL = `ac-${uid}`;
    const M_DISC   = `ad-${uid}`;

    const marker = (id, color) => (
        <marker key={id} id={id} markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
            <path d="M0,0 L0,6 L8,3 z" fill={color} />
        </marker>
    );

    return (
        <div style={{ overflowX: 'auto', padding: '0.5rem 0' }}>
            <svg
                viewBox={`0 0 ${svgW} ${svgH}`}
                width={svgW}
                height={svgH}
                style={{ display: 'block', minWidth: svgW, fontFamily: 'inherit' }}
            >
                <defs>
                    {marker(M_MAIN,   C_ARROW)}
                    {marker(M_OK,     C_APPROVE)}
                    {marker(M_FAIL,   C_REJECT)}
                    {marker(M_REWORK, C_REWORK)}
                    {marker(M_CANCEL, C_CANCEL)}
                    {marker(M_DISC,   C_DISC)}
                    <filter id={`sh-${uid}`} x="-10%" y="-10%" width="120%" height="120%">
                        <feDropShadow dx="0" dy="2" stdDeviation="3" floodColor="rgba(0,0,0,0.4)" />
                    </filter>
                </defs>

                {/* ── Start circle ── */}
                <circle cx={START_CX} cy={MAIN_Y} r={START_R}
                    fill={C_STAGE} filter={`url(#sh-${uid})`} />
                <text x={START_CX} y={MAIN_Y + 1} textAnchor="middle"
                    dominantBaseline="middle" fontSize="9" fontWeight="700" fill={C_TEXT}>
                    START
                </text>

                {/* Start → first stage */}
                <line x1={START_CX + START_R} y1={MAIN_Y}
                    x2={stageLeft(0) - 4} y2={MAIN_Y}
                    stroke={C_ARROW} strokeWidth="1.5" markerEnd={`url(#${M_MAIN})`} />

                {/* ── Stage boxes ── */}
                {stages.map((stage, i) => {
                    const bx    = stageLeft(i);
                    const by    = MAIN_Y - BOX_H / 2;
                    const cx    = stageCX(i);
                    const color = i === N - 1 ? C_FINAL : C_STAGE;
                    const nameLines = wrapText(stage.name, 15);
                    const roleLabel = formatRole(stage.candidateGroup);

                    return (
                        <g key={stage.taskDefinitionKey}>
                            <rect x={bx} y={by} width={BOX_W} height={BOX_H}
                                rx={BOX_RX} fill={color} filter={`url(#sh-${uid})`}
                                stroke={i === N - 1 ? '#a5b4fc' : 'none'} strokeWidth="1.5" />

                            {nameLines.map((line, li) => {
                                const total = nameLines.length;
                                return (
                                    <text key={li} x={cx} y={MAIN_Y + (li - (total - 1) / 2) * 14}
                                        textAnchor="middle" dominantBaseline="middle"
                                        fontSize="11" fontWeight="600" fill={C_TEXT}>
                                        {line}
                                    </text>
                                );
                            })}

                            <text x={cx} y={by + BOX_H + 13} textAnchor="middle"
                                dominantBaseline="middle" fontSize="9" fill={C_SUB}>
                                {roleLabel}
                            </text>

                            {/* Arrow to next stage */}
                            {i < N - 1 && (
                                <line x1={bx + BOX_W} y1={MAIN_Y}
                                    x2={stageLeft(i + 1) - 4} y2={MAIN_Y}
                                    stroke={C_ARROW} strokeWidth="1.5"
                                    markerEnd={`url(#${M_MAIN})`} />
                            )}
                        </g>
                    );
                })}

                {/* Last stage → diamond */}
                <line x1={stageLeft(N - 1) + BOX_W} y1={MAIN_Y}
                    x2={diamCX - DIAM_W / 2 - 4} y2={MAIN_Y}
                    stroke={C_ARROW} strokeWidth="1.5" markerEnd={`url(#${M_MAIN})`} />

                {/* ── Decision diamond ── */}
                <polygon points={diamondPoints(diamCX, diamCY, DIAM_W, DIAM_H)}
                    fill={C_DIAM} filter={`url(#sh-${uid})`} />
                <text x={diamCX} y={diamCY - 6} textAnchor="middle"
                    dominantBaseline="middle" fontSize="9" fontWeight="700" fill="#1c1917">
                    Finalize
                </text>
                <text x={diamCX} y={diamCY + 7} textAnchor="middle"
                    dominantBaseline="middle" fontSize="8" fill="#1c1917">
                    {stages[N - 1]?.name}
                </text>

                {/* Diamond → Approved */}
                <line x1={diamCX + DIAM_W / 2} y1={diamCY}
                    x2={appCX - TERM_W / 2 - 4} y2={appCY}
                    stroke={C_APPROVE} strokeWidth="1.8"
                    markerEnd={`url(#${M_OK})`} />
                <text x={(diamCX + DIAM_W / 2 + appCX - TERM_W / 2) / 2}
                    y={appCY - 9} textAnchor="middle"
                    fontSize="9" fill={C_APPROVE} fontWeight="600">
                    APPROVE
                </text>

                {/* Diamond → Rejected */}
                <line x1={diamCX} y1={diamCY + DIAM_H / 2}
                    x2={rejCX} y2={rejCY - TERM_H / 2 - 4}
                    stroke={C_REJECT} strokeWidth="1.8"
                    markerEnd={`url(#${M_FAIL})`} />
                <text x={diamCX + 8} y={(diamCY + DIAM_H / 2 + rejCY - TERM_H / 2) / 2}
                    fontSize="9" fill={C_REJECT} fontWeight="600"
                    dominantBaseline="middle">
                    REJECT
                </text>

                {/* ── Approved terminal ── */}
                <rect x={appCX - TERM_W / 2} y={appCY - TERM_H / 2}
                    width={TERM_W} height={TERM_H} rx={TERM_RX}
                    fill={C_APPROVE} filter={`url(#sh-${uid})`} />
                <text x={appCX} y={appCY - 5} textAnchor="middle"
                    dominantBaseline="middle" fontSize="11" fontWeight="700" fill="#fff">
                    ✓ Approved
                </text>
                <text x={appCX} y={appCY + 8} textAnchor="middle"
                    dominantBaseline="middle" fontSize="8" fill="rgba(255,255,255,0.7)">
                    Case closed
                </text>

                {/* ── Rejected terminal ── */}
                <rect x={rejCX - TERM_W / 2} y={rejCY - TERM_H / 2}
                    width={TERM_W} height={TERM_H} rx={TERM_RX}
                    fill={C_REJECT} filter={`url(#sh-${uid})`} />
                <text x={rejCX} y={rejCY - 5} textAnchor="middle"
                    dominantBaseline="middle" fontSize="11" fontWeight="700" fill="#fff">
                    ✗ Rejected
                </text>
                <text x={rejCX} y={rejCY + 8} textAnchor="middle"
                    dominantBaseline="middle" fontSize="8" fill="rgba(255,255,255,0.7)">
                    Case closed
                </text>

                {/* ── Rework arcs (stages 1..N-1 + diamond back to stage 0) ──
                    Curved path below the main flow row.
                    For stages i=1..N-1: bottom of stage i → bottom of stage 0
                    Also from diamond bottom → bottom of stage 0 */}
                {N > 1 && (() => {
                    const analystBX = stageLeft(0);
                    const analystBY = MAIN_Y + BOX_H / 2;   // bottom of analyst box
                    const analystCX = stageCX(0);

                    const arcs = [];

                    // From each non-analyst sequential stage
                    for (let i = 1; i < N; i++) {
                        const fromCX = stageCX(i);
                        const fromBY = MAIN_Y + BOX_H / 2;
                        // Depth of arc varies to prevent overlaps
                        const depth = REWORK_Y + (i - 1) * 14;

                        const d = `M ${fromCX} ${fromBY}
                                   L ${fromCX} ${depth}
                                   L ${analystCX} ${depth}
                                   L ${analystCX} ${analystBY + 4}`;

                        arcs.push(
                            <g key={`rework-${i}`}>
                                <path d={d} fill="none" stroke={C_REWORK}
                                    strokeWidth="1.5" strokeDasharray="5 3"
                                    markerEnd={`url(#${M_REWORK})`} />
                                <text
                                    x={(fromCX + analystCX) / 2}
                                    y={depth + 11}
                                    textAnchor="middle" fontSize="8"
                                    fill={C_REWORK} fontWeight="600">
                                    Rework → {stages[0].name}
                                </text>
                            </g>
                        );
                    }

                    // From diamond (any of reviewer/AFC/ACO can rework, shown via ACO)
                    const diamBY = diamCY + DIAM_H / 2;
                    const diamDepth = REWORK_Y + N * 14;
                    arcs.push(
                        <g key="rework-diamond">
                            <path
                                d={`M ${diamCX - DIAM_W / 2} ${diamCY}
                                    L ${diamCX - DIAM_W / 2 - 12} ${diamCY}
                                    L ${diamCX - DIAM_W / 2 - 12} ${diamDepth}
                                    L ${analystCX} ${diamDepth}
                                    L ${analystCX} ${analystBY + 4}`}
                                fill="none" stroke={C_REWORK}
                                strokeWidth="1.5" strokeDasharray="5 3"
                                markerEnd={`url(#${M_REWORK})`} />
                            <text
                                x={(diamCX - DIAM_W / 2 - 12 + analystCX) / 2}
                                y={diamDepth + 11}
                                textAnchor="middle" fontSize="8"
                                fill={C_REWORK} fontWeight="600">
                                Rework from Finalize
                            </text>
                        </g>
                    );

                    return arcs;
                })()}

                {/* ── Cancel arc (Analyst only, goes up then to Cancel terminal) ── */}
                {(() => {
                    const analystTopY = MAIN_Y - BOX_H / 2;
                    const analystCX   = stageCX(0);
                    return (
                        <g>
                            {/* path: top of analyst box → up → cancel terminal */}
                            <path
                                d={`M ${analystCX} ${analystTopY}
                                    L ${analystCX} ${cancelCY + CANCEL_TERM_H / 2 + 4}`}
                                fill="none" stroke={C_CANCEL}
                                strokeWidth="1.5" strokeDasharray="4 3"
                                markerEnd={`url(#${M_CANCEL})`} />

                            {/* Cancel terminal */}
                            <rect
                                x={cancelCX - CANCEL_TERM_W / 2}
                                y={cancelCY - CANCEL_TERM_H / 2}
                                width={CANCEL_TERM_W} height={CANCEL_TERM_H}
                                rx={TERM_RX / 2}
                                fill={C_CANCEL} filter={`url(#sh-${uid})`} />
                            <text x={cancelCX} y={cancelCY - 4} textAnchor="middle"
                                dominantBaseline="middle" fontSize="10" fontWeight="700" fill="#fff">
                                ⊘ Cancelled
                            </text>
                            <text x={cancelCX} y={cancelCY + 9} textAnchor="middle"
                                dominantBaseline="middle" fontSize="8" fill="rgba(255,255,255,0.7)">
                                Analyst only
                            </text>

                            {/* Label on the cancel arrow */}
                            <text x={analystCX + 5}
                                y={(analystTopY + cancelCY + CANCEL_TERM_H / 2) / 2}
                                fontSize="8" fill={C_CANCEL} fontWeight="600"
                                dominantBaseline="middle">
                                Cancel
                            </text>
                        </g>
                    );
                })()}

                {/* ── Discretionary actions section ── */}
                {enabledActions.length > 0 && (
                    <g>
                        <line x1={ACT_START_X} y1={rejCY + TERM_H / 2 + 20}
                            x2={svgW - 20} y2={rejCY + TERM_H / 2 + 20}
                            stroke="rgba(100,116,139,0.3)" strokeWidth="1" strokeDasharray="4 4" />
                        <text x={ACT_START_X} y={rejCY + TERM_H / 2 + 34}
                            fontSize="9" fill={C_SUB} fontStyle="italic">
                            Discretionary actions — can be triggered at any stage
                        </text>

                        {enabledActions.map((action, i) => {
                            const ax = ACT_START_X + i * (ACT_BOX_W + ACT_GAP);
                            const ay = ACT_ROW_Y;
                            const evtLines  = wrapText(action.eventListenerName || action.eventListenerKey, 16);
                            const taskLines = wrapText(action.name, 16);
                            const EVT_W  = 60;
                            const EVT_H  = 30;
                            const TASK_W = ACT_BOX_W - EVT_W - 16;

                            return (
                                <g key={action.eventListenerKey}>
                                    <rect x={ax} y={ay - EVT_H / 2}
                                        width={EVT_W} height={EVT_H} rx="15"
                                        fill="none" stroke={C_DISC} strokeWidth="1.5"
                                        strokeDasharray="4 3" />
                                    {evtLines.map((line, li) => (
                                        <text key={li}
                                            x={ax + EVT_W / 2}
                                            y={ay + (li - (evtLines.length - 1) / 2) * 11}
                                            textAnchor="middle" dominantBaseline="middle"
                                            fontSize="8" fill={C_DISC}>
                                            {line}
                                        </text>
                                    ))}
                                    <line x1={ax + EVT_W} y1={ay}
                                        x2={ax + EVT_W + 8} y2={ay}
                                        stroke={C_DISC} strokeWidth="1.2"
                                        strokeDasharray="3 2"
                                        markerEnd={`url(#${M_DISC})`} />
                                    <rect x={ax + EVT_W + 16} y={ay - ACT_BOX_H / 2}
                                        width={TASK_W} height={ACT_BOX_H} rx="6"
                                        fill={C_DISC} fillOpacity="0.15"
                                        stroke={C_DISC} strokeWidth="1.5"
                                        strokeDasharray="4 3" />
                                    {taskLines.map((line, li) => (
                                        <text key={li}
                                            x={ax + EVT_W + 16 + TASK_W / 2}
                                            y={ay + (li - (taskLines.length - 1) / 2) * 12}
                                            textAnchor="middle" dominantBaseline="middle"
                                            fontSize="9" fontWeight="600" fill={C_DISC}>
                                            {line}
                                        </text>
                                    ))}
                                    {action.candidateGroup && action.taskKey !== 'ht_clientComm' && (
                                        <text x={ax + EVT_W + 16 + TASK_W / 2}
                                            y={ay + ACT_BOX_H / 2 + 11}
                                            textAnchor="middle" fontSize="8" fill={C_SUB}>
                                            {formatRole(action.candidateGroup.split(',')[0])}
                                        </text>
                                    )}
                                    {action.taskKey === 'ht_clientComm' && (
                                        <text x={ax + EVT_W + 16 + TASK_W / 2}
                                            y={ay + ACT_BOX_H / 2 + 11}
                                            textAnchor="middle" fontSize="8" fill={C_SUB}>
                                            Assigned to initiator
                                        </text>
                                    )}
                                </g>
                            );
                        })}
                    </g>
                )}
            </svg>

            {/* Legend */}
            <div style={{ display: 'flex', gap: '1.25rem', flexWrap: 'wrap', marginTop: '0.75rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                {[
                    { color: '#3b82f6', label: 'Review stage' },
                    { color: '#6366f1', label: 'Final stage (Finalize)' },
                    { color: '#f59e0b', label: 'Finalize decision / Rework', dashed: false },
                    { color: '#22c55e', label: 'Approved outcome' },
                    { color: '#ef4444', label: 'Rejected outcome' },
                    { color: '#f59e0b', label: 'Rework → Analyst', dashed: true },
                    { color: '#6b7280', label: 'Cancel (Analyst only)', dashed: true },
                    ...(enabledActions.length > 0 ? [{ color: '#8b5cf6', label: 'Discretionary action', dashed: true }] : []),
                ].map(({ color, label, dashed }) => (
                    <span key={label} style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                        <span style={{
                            display: 'inline-block', width: '20px', height: '10px',
                            background: dashed ? 'none' : color,
                            border: dashed ? `2px dashed ${color}` : 'none',
                            borderRadius: '3px',
                        }} />
                        {label}
                    </span>
                ))}
            </div>
        </div>
    );
};

export default WorkflowDiagram;
