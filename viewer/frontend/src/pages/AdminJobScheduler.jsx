import React, { useState, useEffect, useCallback, useRef } from 'react';
import { jobSchedulerService } from '../services/jobSchedulerService';
import { clientService } from '../services/clientService';
import Button from '../components/Button';

/* ── Helpers ──────────────────────────────────────────────────── */

const fmt = (iso) => {
    if (!iso) return '--';
    return new Date(iso).toLocaleString(undefined, {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
    });
};

const cronToHuman = (cron) => {
    if (!cron) return cron;
    const parts = cron.split(' ');
    if (parts.length < 5) return cron;
    const [min, hr, dom, , dow] = parts;
    if (dom === '1' && min === '0' && hr === '0') return 'Monthly (1st at midnight)';
    if (dow === '0' && dom === '*') return `Weekly (Sun ${hr}:${min.padStart(2, '0')})`;
    if (dom === '*' && dow === '*') return `Daily at ${hr}:${min.padStart(2, '0')}`;
    return cron;
};

const STATE_COLORS = {
    ENQUEUED:   { bg: 'rgba(245,158,11,0.15)', color: '#fbbf24' },
    PROCESSING: { bg: 'rgba(59,130,246,0.15)',  color: '#60a5fa' },
    SUCCEEDED:  { bg: 'rgba(16,185,129,0.15)',  color: '#34d399' },
    FAILED:     { bg: 'rgba(239,68,68,0.15)',   color: '#f87171' },
    SCHEDULED:  { bg: 'rgba(168,85,247,0.15)',  color: '#c084fc' },
    DELETED:    { bg: 'rgba(100,116,139,0.15)', color: '#94a3b8' },
};

const stateBadge = (state) => {
    const c = STATE_COLORS[state] ?? { bg: 'rgba(255,255,255,0.07)', color: 'var(--text-secondary)' };
    return (
        <span style={{
            padding: '2px 9px', borderRadius: '10px', fontSize: '0.68rem',
            fontWeight: 700, background: c.bg, color: c.color, textTransform: 'uppercase',
        }}>
            {state?.replace(/_/g, ' ')}
        </span>
    );
};

const TABS = [
    { id: 'recurring', label: 'Recurring Jobs',  icon: '🔁' },
    { id: 'queue',     label: 'Queue',            icon: '📥' },
    { id: 'failed',    label: 'Failed',           icon: '❌' },
    { id: 'history',   label: 'History',           icon: '📜' },
];

/* ── Main component ───────────────────────────────────────────── */

const AdminJobScheduler = () => {
    const [activeTab, setActiveTab] = useState('recurring');
    const [stats, setStats] = useState(null);
    const [recurring, setRecurring] = useState([]);
    const [jobs, setJobs] = useState({ items: [], total: 0 });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [page, setPage] = useState(0);
    const PAGE_SIZE = 20;

    // ── Create Job modal state ──────────────────────────────────────
    const [showJobModal, setShowJobModal] = useState(false);
    const [jobType, setJobType] = useState('batch-screening');
    const [clientSearch, setClientSearch] = useState('');
    const [clientPage, setClientPage] = useState(0);
    const [clientList, setClientList] = useState([]);
    const [clientListPages, setClientListPages] = useState(0);
    const [selectedClientIds, setSelectedClientIds] = useState(new Set());
    const [batchCreatedBy, setBatchCreatedBy] = useState('');
    const [scheduleType, setScheduleType] = useState('now');
    const [scheduledAt, setScheduledAt] = useState('');
    const [jobSubmitting, setJobSubmitting] = useState(false);
    const [jobSuccess, setJobSuccess] = useState(null);
    const [jobModalError, setJobModalError] = useState(null);
    const searchDebounceRef = useRef(null);

    /* ── Data loading ──────────────────────────────────────────── */

    const loadStats = useCallback(async () => {
        try {
            const data = await jobSchedulerService.getStats();
            setStats(data);
        } catch (e) { console.error('Stats error', e); }
    }, []);

    const loadRecurring = useCallback(async () => {
        setLoading(true);
        try {
            const data = await jobSchedulerService.getRecurringJobs();
            setRecurring(data);
            setError(null);
        } catch (e) { setError(e.message); }
        finally { setLoading(false); }
    }, []);

    const loadJobs = useCallback(async (state, pg = 0) => {
        setLoading(true);
        try {
            const data = state === 'ACTIVE'
                ? await jobSchedulerService.getActiveJobs(pg * PAGE_SIZE, PAGE_SIZE)
                : await jobSchedulerService.getJobs(state, pg * PAGE_SIZE, PAGE_SIZE);
            setJobs(data);
            setError(null);
        } catch (e) { setError(e.message); }
        finally { setLoading(false); }
    }, []);

    const loadModalClients = useCallback(async (pg, query) => {
        try {
            const data = await clientService.getClients(pg, query);
            setClientList(data.content ?? []);
            setClientListPages(data.totalPages ?? 0);
        } catch (e) { console.error('Client load error', e); }
    }, []);

    const openJobModal = () => {
        setShowJobModal(true);
        setJobType('batch-screening');
        setClientSearch('');
        setClientPage(0);
        setSelectedClientIds(new Set());
        setBatchCreatedBy('');
        setScheduleType('now');
        setScheduledAt('');
        setJobSuccess(null);
        setJobModalError(null);
        loadModalClients(0, '');
    };

    const closeJobModal = () => setShowJobModal(false);

    const handleClientSearchChange = (val) => {
        setClientSearch(val);
        setClientPage(0);
        if (searchDebounceRef.current) clearTimeout(searchDebounceRef.current);
        searchDebounceRef.current = setTimeout(() => loadModalClients(0, val), 300);
    };

    const handleModalPageChange = (pg) => {
        setClientPage(pg);
        loadModalClients(pg, clientSearch);
    };

    const toggleClientSelect = (id) => {
        setSelectedClientIds(prev => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    };

    const toggleSelectAll = () => {
        const allIds = clientList.map(c => c.clientID);
        const allSelected = allIds.every(id => selectedClientIds.has(id));
        setSelectedClientIds(prev => {
            const next = new Set(prev);
            allIds.forEach(id => allSelected ? next.delete(id) : next.add(id));
            return next;
        });
    };

    const handleJobSubmit = async () => {
        if (selectedClientIds.size === 0) return;
        if (scheduleType === 'later' && !scheduledAt) {
            setJobModalError('Please select a date and time to schedule the job.');
            return;
        }
        setJobSubmitting(true);
        setJobModalError(null);
        try {
            const ids = [...selectedClientIds];
            const at = scheduleType === 'later' ? new Date(scheduledAt).toISOString() : null;
            if (jobType === 'batch-screening') {
                await jobSchedulerService.enqueueBatchScreeningJob(ids, batchCreatedBy || 'SYSTEM', at);
            } else {
                await jobSchedulerService.enqueuePeriodicReviewJob(ids, batchCreatedBy || 'SYSTEM', at);
            }
            const label = jobType === 'batch-screening' ? 'Screening Batch' : 'Periodic Review';
            const when = scheduleType === 'later' ? `scheduled for ${new Date(scheduledAt).toLocaleString()}` : 'enqueued';
            setJobSuccess(`${label} job ${when} for ${ids.length} client(s).`);
            setSelectedClientIds(new Set());
            loadStats();
            // Close modal and switch to Queue tab after a brief pause
            setTimeout(() => {
                setShowJobModal(false);
                setActiveTab('queue');
                loadJobs('ACTIVE', 0);
                setPage(0);
            }, 1200);
        } catch (e) {
            setJobModalError(e.message || 'Failed to submit job. Check console for details.');
            console.error('Job submit error:', e);
        } finally {
            setJobSubmitting(false);
        }
    };

    useEffect(() => {
        loadStats();
    }, [loadStats]);

    useEffect(() => {
        setPage(0);
        if (activeTab === 'recurring') loadRecurring();
        else if (activeTab === 'queue') loadJobs('ACTIVE', 0);
        else if (activeTab === 'failed') loadJobs('FAILED', 0);
        else if (activeTab === 'history') loadJobs('SUCCEEDED', 0);
    }, [activeTab, loadRecurring, loadJobs]);

    const handlePageChange = (newPage) => {
        setPage(newPage);
        const stateMap = { queue: 'ACTIVE', failed: 'FAILED', history: 'SUCCEEDED' };
        loadJobs(stateMap[activeTab], newPage);
    };

    /* ── Actions ───────────────────────────────────────────────── */

    const handleTrigger = async (id) => {
        try {
            await jobSchedulerService.triggerRecurringJob(id);
            loadStats();
        } catch (e) { setError(e.message); }
    };

    const handleDeleteRecurring = async (id) => {
        if (!window.confirm(`Delete recurring job "${id}"?`)) return;
        try {
            await jobSchedulerService.deleteRecurringJob(id);
            loadRecurring();
            loadStats();
        } catch (e) { setError(e.message); }
    };

    const handleRequeue = async (id) => {
        try {
            await jobSchedulerService.requeueJob(id);
            loadJobs('FAILED', page);
            loadStats();
        } catch (e) { setError(e.message); }
    };

    const handleDeleteJob = async (id) => {
        try {
            await jobSchedulerService.deleteJob(id);
            const stateMap = { queue: 'ACTIVE', failed: 'FAILED', history: 'SUCCEEDED' };
            loadJobs(stateMap[activeTab], page);
            loadStats();
        } catch (e) { setError(e.message); }
    };

    /* ── Stat cards ────────────────────────────────────────────── */

    const statCards = stats ? [
        { label: 'Recurring',  value: stats.recurringJobs,  color: '#a78bfa' },
        { label: 'Enqueued',   value: stats.enqueued,       color: '#fbbf24' },
        { label: 'Processing', value: stats.processing,     color: '#60a5fa' },
        { label: 'Succeeded',  value: stats.succeeded,      color: '#34d399' },
        { label: 'Failed',     value: stats.failed,         color: '#f87171' },
        { label: 'Servers',    value: stats.backgroundJobServers, color: '#94a3b8' },
    ] : [];

    /* ── Render ─────────────────────────────────────────────────── */

    return (
        <div>
            {/* Header */}
            <div style={{ marginBottom: '1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <h1 style={{ fontSize: '2rem', margin: '0 0 0.5rem 0', background: 'var(--header-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                        Job Scheduler
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', margin: 0 }}>
                        Monitor and manage scheduled background jobs
                    </p>
                </div>
                <Button variant="primary" onClick={openJobModal} style={{ whiteSpace: 'nowrap', marginTop: '0.25rem' }}>
                    + Create Job
                </Button>
            </div>

            {/* Stat cards */}
            {stats && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
                    {statCards.map((s, i) => (
                        <div key={i} className="glass-section" style={{
                            padding: '1rem 1.25rem', marginBottom: 0,
                            borderLeft: `4px solid ${s.color}`,
                        }}>
                            <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{s.label}</div>
                            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-color)' }}>{s.value}</div>
                        </div>
                    ))}
                </div>
            )}

            {/* Tabs */}
            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
                {TABS.map(tab => (
                    <button
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        style={{
                            padding: '0.5rem 1rem', borderRadius: '8px', cursor: 'pointer',
                            border: activeTab === tab.id ? '1px solid var(--primary-color)' : '1px solid var(--glass-border)',
                            background: activeTab === tab.id ? 'rgba(59,130,246,0.15)' : 'transparent',
                            color: activeTab === tab.id ? 'var(--primary-color)' : 'var(--text-secondary)',
                            fontWeight: activeTab === tab.id ? 600 : 400,
                            fontSize: '0.85rem', fontFamily: 'inherit',
                            transition: 'all 0.15s',
                        }}
                    >
                        {tab.icon} {tab.label}
                        {tab.id === 'failed' && stats?.failed > 0 && (
                            <span style={{ marginLeft: '0.4rem', background: '#ef4444', color: '#fff', padding: '1px 6px', borderRadius: '10px', fontSize: '0.7rem' }}>
                                {stats.failed}
                            </span>
                        )}
                    </button>
                ))}
            </div>

            {/* Error banner */}
            {error && (
                <div style={{ padding: '0.75rem 1rem', marginBottom: '1rem', background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '8px', color: '#f87171', fontSize: '0.85rem' }}>
                    {error}
                    <button onClick={() => setError(null)} style={{ float: 'right', background: 'none', border: 'none', color: '#f87171', cursor: 'pointer', fontWeight: 700 }}>x</button>
                </div>
            )}

            {/* Tab content */}
            <div className="glass-section" style={{ padding: 0 }}>

                {/* ── Recurring tab ─────────────────────────────────── */}
                {activeTab === 'recurring' && (
                    <>
                        <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h3 style={{ margin: 0 }}>Recurring Jobs</h3>
                            <button onClick={loadRecurring} style={{ background: 'none', border: '1px solid var(--glass-border)', borderRadius: '6px', color: 'var(--text-secondary)', padding: '4px 10px', cursor: 'pointer', fontSize: '0.8rem' }}>
                                ↻ Refresh
                            </button>
                        </div>
                        {loading ? (
                            <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Loading...</div>
                        ) : recurring.length === 0 ? (
                            <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>No recurring jobs registered.</div>
                        ) : (
                            <div style={{ overflowX: 'auto' }}>
                                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                    <thead>
                                        <tr style={{ background: 'var(--hover-bg)' }}>
                                            {['Job ID', 'Name', 'Schedule', 'Next Run', 'Created', 'Actions'].map((h, i) => (
                                                <th key={i} style={{ padding: '0.75rem 1.25rem', textAlign: i === 5 ? 'right' : 'left', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', whiteSpace: 'nowrap' }}>{h}</th>
                                            ))}
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {recurring.map(rj => (
                                            <tr key={rj.id} className="table-row-hover" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                                                <td style={{ padding: '0.85rem 1.25rem' }}>
                                                    <span style={{ fontFamily: 'monospace', fontSize: '0.8rem', color: 'var(--text-color)' }}>{rj.id}</span>
                                                </td>
                                                <td style={{ padding: '0.85rem 1.25rem', fontWeight: 500, color: 'var(--text-color)' }}>{rj.jobName || rj.id}</td>
                                                <td style={{ padding: '0.85rem 1.25rem' }}>
                                                    <div style={{ color: 'var(--text-color)', fontSize: '0.85rem' }}>{cronToHuman(rj.cronExpression)}</div>
                                                    <div style={{ fontFamily: 'monospace', fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '2px' }}>{rj.cronExpression}</div>
                                                </td>
                                                <td style={{ padding: '0.85rem 1.25rem', color: 'var(--text-secondary)', fontSize: '0.82rem', whiteSpace: 'nowrap' }}>{fmt(rj.nextRun)}</td>
                                                <td style={{ padding: '0.85rem 1.25rem', color: 'var(--text-secondary)', fontSize: '0.82rem', whiteSpace: 'nowrap' }}>{fmt(rj.createdAt)}</td>
                                                <td style={{ padding: '0.85rem 1.25rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                                                    <Button variant="primary" onClick={() => handleTrigger(rj.id)} style={{ fontSize: '0.78rem', padding: '0.3rem 0.7rem', marginRight: '0.4rem' }}>
                                                        Run Now
                                                    </Button>
                                                    <Button variant="secondary" onClick={() => handleDeleteRecurring(rj.id)} style={{ fontSize: '0.78rem', padding: '0.3rem 0.7rem', color: '#f87171', borderColor: 'rgba(239,68,68,0.3)' }}>
                                                        Delete
                                                    </Button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </>
                )}

                {/* ── Queue / Failed / History tabs ─────────────────── */}
                {(activeTab === 'queue' || activeTab === 'failed' || activeTab === 'history') && (
                    <>
                        <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h3 style={{ margin: 0 }}>{TABS.find(t => t.id === activeTab)?.label}</h3>
                            <button onClick={() => { const m = { queue: 'ACTIVE', failed: 'FAILED', history: 'SUCCEEDED' }; loadJobs(m[activeTab], page); }} style={{ background: 'none', border: '1px solid var(--glass-border)', borderRadius: '6px', color: 'var(--text-secondary)', padding: '4px 10px', cursor: 'pointer', fontSize: '0.8rem' }}>
                                ↻ Refresh
                            </button>
                        </div>
                        {loading ? (
                            <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Loading...</div>
                        ) : jobs.items?.length === 0 ? (
                            <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                                <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>{activeTab === 'failed' ? '✅' : '📭'}</div>
                                No {activeTab === 'failed' ? 'failed' : activeTab === 'queue' ? 'enqueued' : 'completed'} jobs.
                            </div>
                        ) : (
                            <>
                                <div style={{ overflowX: 'auto' }}>
                                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                        <thead>
                                            <tr style={{ background: 'var(--hover-bg)' }}>
                                                {['Job ID', 'Name', 'State', 'Created', 'Updated', 'Actions'].map((h, i) => (
                                                    <th key={i} style={{ padding: '0.75rem 1.25rem', textAlign: i === 5 ? 'right' : 'left', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', whiteSpace: 'nowrap' }}>{h}</th>
                                                ))}
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {jobs.items.map(job => (
                                                <tr key={job.id} className="table-row-hover" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                                                    <td style={{ padding: '0.85rem 1.25rem' }}>
                                                        <span style={{ fontFamily: 'monospace', fontSize: '0.75rem', color: 'var(--text-muted)' }}>{String(job.id).slice(0, 8)}...</span>
                                                    </td>
                                                    <td style={{ padding: '0.85rem 1.25rem', fontWeight: 500, color: 'var(--text-color)', maxWidth: '250px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{job.jobName || '--'}</td>
                                                    <td style={{ padding: '0.85rem 1.25rem' }}>{stateBadge(job.state)}</td>
                                                    <td style={{ padding: '0.85rem 1.25rem', color: 'var(--text-secondary)', fontSize: '0.82rem', whiteSpace: 'nowrap' }}>{fmt(job.createdAt)}</td>
                                                    <td style={{ padding: '0.85rem 1.25rem', color: 'var(--text-secondary)', fontSize: '0.82rem', whiteSpace: 'nowrap' }}>{fmt(job.updatedAt)}</td>
                                                    <td style={{ padding: '0.85rem 1.25rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                                                        {activeTab === 'failed' && (
                                                            <Button variant="primary" onClick={() => handleRequeue(job.id)} style={{ fontSize: '0.78rem', padding: '0.3rem 0.7rem', marginRight: '0.4rem' }}>
                                                                Requeue
                                                            </Button>
                                                        )}
                                                        <Button variant="secondary" onClick={() => handleDeleteJob(job.id)} style={{ fontSize: '0.78rem', padding: '0.3rem 0.7rem', color: '#f87171', borderColor: 'rgba(239,68,68,0.3)' }}>
                                                            Delete
                                                        </Button>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>

                                {/* Pagination */}
                                {jobs.totalPages > 1 && (
                                    <div style={{ padding: '1rem 1.25rem', borderTop: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                                            Showing {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, jobs.total)} of {jobs.total}
                                        </span>
                                        <div style={{ display: 'flex', gap: '0.4rem' }}>
                                            <button disabled={page === 0} onClick={() => handlePageChange(page - 1)}
                                                style={{ padding: '0.3rem 0.7rem', borderRadius: '6px', border: '1px solid var(--glass-border)', background: 'none', color: page === 0 ? 'var(--text-muted)' : 'var(--text-color)', cursor: page === 0 ? 'default' : 'pointer', fontSize: '0.8rem' }}>
                                                Prev
                                            </button>
                                            <button disabled={page >= jobs.totalPages - 1} onClick={() => handlePageChange(page + 1)}
                                                style={{ padding: '0.3rem 0.7rem', borderRadius: '6px', border: '1px solid var(--glass-border)', background: 'none', color: page >= jobs.totalPages - 1 ? 'var(--text-muted)' : 'var(--text-color)', cursor: page >= jobs.totalPages - 1 ? 'default' : 'pointer', fontSize: '0.8rem' }}>
                                                Next
                                            </button>
                                        </div>
                                    </div>
                                )}
                            </>
                        )}
                    </>
                )}
            </div>
            {/* ── Create Job Modal ──────────────────────────────────── */}
            {showJobModal && (
                <div style={{
                    position: 'fixed', inset: 0, zIndex: 1000,
                    background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                    <div className="glass-section" style={{
                        width: '680px', maxWidth: '95vw', maxHeight: '90vh',
                        display: 'flex', flexDirection: 'column', padding: 0, margin: 0,
                    }}>
                        {/* Modal header */}
                        <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h3 style={{ margin: 0 }}>Create Ad-Hoc Job</h3>
                            <button onClick={closeJobModal} style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: '1.2rem', lineHeight: 1 }}>✕</button>
                        </div>

                        {/* Success banner */}
                        {jobSuccess && (
                            <div style={{ margin: '1rem 1.25rem 0', padding: '0.75rem 1rem', background: 'rgba(16,185,129,0.12)', border: '1px solid rgba(16,185,129,0.3)', borderRadius: '8px', color: '#34d399', fontSize: '0.85rem' }}>
                                {jobSuccess}
                            </div>
                        )}

                        {/* Error banner */}
                        {jobModalError && (
                            <div style={{ margin: '1rem 1.25rem 0', padding: '0.75rem 1rem', background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: '8px', color: '#f87171', fontSize: '0.85rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span>{jobModalError}</span>
                                <button onClick={() => setJobModalError(null)} style={{ background: 'none', border: 'none', color: '#f87171', cursor: 'pointer', fontWeight: 700, marginLeft: '0.5rem' }}>✕</button>
                            </div>
                        )}

                        {/* Job type selector */}
                        <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--glass-border)' }}>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.5rem' }}>Job Type</div>
                            <div style={{ display: 'flex', gap: '0.5rem' }}>
                                {[
                                    { id: 'batch-screening', label: '🔍 Screening Batch' },
                                    { id: 'periodic-review', label: '📋 Periodic Review' },
                                ].map(jt => (
                                    <button key={jt.id} onClick={() => setJobType(jt.id)} style={{
                                        padding: '0.5rem 1rem', borderRadius: '8px', cursor: 'pointer', fontFamily: 'inherit', fontSize: '0.85rem',
                                        border: jobType === jt.id ? '1px solid var(--primary-color)' : '1px solid var(--glass-border)',
                                        background: jobType === jt.id ? 'rgba(59,130,246,0.15)' : 'transparent',
                                        color: jobType === jt.id ? 'var(--primary-color)' : 'var(--text-secondary)',
                                        fontWeight: jobType === jt.id ? 600 : 400,
                                    }}>
                                        {jt.label}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Scheduling options */}
                        <div style={{ padding: '0.85rem 1.25rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', whiteSpace: 'nowrap' }}>Run</div>
                            <div style={{ display: 'flex', gap: '0.5rem' }}>
                                {[{ id: 'now', label: '▶ Immediately' }, { id: 'later', label: '🕐 Schedule for Later' }].map(opt => (
                                    <button key={opt.id} onClick={() => { setScheduleType(opt.id); setJobModalError(null); }} style={{
                                        padding: '0.4rem 0.85rem', borderRadius: '8px', cursor: 'pointer', fontFamily: 'inherit', fontSize: '0.82rem',
                                        border: scheduleType === opt.id ? '1px solid var(--primary-color)' : '1px solid var(--glass-border)',
                                        background: scheduleType === opt.id ? 'rgba(59,130,246,0.15)' : 'transparent',
                                        color: scheduleType === opt.id ? 'var(--primary-color)' : 'var(--text-secondary)',
                                        fontWeight: scheduleType === opt.id ? 600 : 400,
                                    }}>{opt.label}</button>
                                ))}
                            </div>
                            {scheduleType === 'later' && (
                                <input
                                    type="datetime-local"
                                    value={scheduledAt}
                                    min={new Date(Date.now() + 60000).toISOString().slice(0, 16)}
                                    onChange={e => setScheduledAt(e.target.value)}
                                    style={{
                                        padding: '0.4rem 0.7rem', borderRadius: '6px', fontFamily: 'inherit', fontSize: '0.82rem',
                                        background: 'var(--input-bg, rgba(255,255,255,0.07))',
                                        border: '1px solid var(--glass-border)', color: 'var(--text-color)', outline: 'none',
                                    }}
                                />
                            )}
                        </div>

                        {/* Client search */}
                        <div style={{ padding: '0.75rem 1.25rem', borderBottom: '1px solid var(--glass-border)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                            <input
                                type="text"
                                placeholder="Search clients by name..."
                                value={clientSearch}
                                onChange={e => handleClientSearchChange(e.target.value)}
                                style={{
                                    flex: 1, padding: '0.45rem 0.75rem', borderRadius: '6px',
                                    background: 'var(--input-bg, rgba(255,255,255,0.07))',
                                    border: '1px solid var(--glass-border)', color: 'var(--text-color)',
                                    fontFamily: 'inherit', fontSize: '0.85rem', outline: 'none',
                                }}
                            />
                            {selectedClientIds.size > 0 && (
                                <span style={{ fontSize: '0.8rem', color: '#60a5fa', whiteSpace: 'nowrap' }}>
                                    {selectedClientIds.size} selected
                                </span>
                            )}
                        </div>

                        {/* Client table */}
                        <div style={{ flex: 1, overflowY: 'auto' }}>
                            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                <thead>
                                    <tr style={{ background: 'var(--hover-bg)' }}>
                                        <th style={{ padding: '0.6rem 1.25rem', width: '2.5rem' }}>
                                            <input type="checkbox"
                                                checked={clientList.length > 0 && clientList.every(c => selectedClientIds.has(c.clientID))}
                                                onChange={toggleSelectAll}
                                                style={{ cursor: 'pointer' }}
                                            />
                                        </th>
                                        {['ID', 'Name', 'Status'].map((h, i) => (
                                            <th key={i} style={{ padding: '0.6rem 1.25rem', textAlign: 'left', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{h}</th>
                                        ))}
                                    </tr>
                                </thead>
                                <tbody>
                                    {clientList.length === 0 ? (
                                        <tr><td colSpan={4} style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>No clients found.</td></tr>
                                    ) : clientList.map(c => (
                                        <tr key={c.clientID} className="table-row-hover"
                                            style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', cursor: 'pointer' }}
                                            onClick={() => toggleClientSelect(c.clientID)}>
                                            <td style={{ padding: '0.65rem 1.25rem' }}>
                                                <input type="checkbox" readOnly
                                                    checked={selectedClientIds.has(c.clientID)}
                                                    style={{ cursor: 'pointer' }}
                                                />
                                            </td>
                                            <td style={{ padding: '0.65rem 1.25rem', fontFamily: 'monospace', fontSize: '0.8rem', color: 'var(--text-muted)' }}>{c.clientID}</td>
                                            <td style={{ padding: '0.65rem 1.25rem', fontWeight: 500, color: 'var(--text-color)' }}>
                                                {[c.firstName, c.middleName, c.lastName].filter(Boolean).join(' ')}
                                            </td>
                                            <td style={{ padding: '0.65rem 1.25rem' }}>
                                                <span style={{ fontSize: '0.72rem', padding: '2px 8px', borderRadius: '10px', fontWeight: 600,
                                                    background: c.status === 'ACTIVE' ? 'rgba(16,185,129,0.15)' : 'rgba(245,158,11,0.15)',
                                                    color: c.status === 'ACTIVE' ? '#34d399' : '#fbbf24' }}>
                                                    {c.status}
                                                </span>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        {/* Pagination */}
                        {clientListPages > 1 && (
                            <div style={{ padding: '0.6rem 1.25rem', borderTop: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'flex-end', gap: '0.4rem' }}>
                                <button disabled={clientPage === 0} onClick={() => handleModalPageChange(clientPage - 1)}
                                    style={{ padding: '0.25rem 0.6rem', borderRadius: '6px', border: '1px solid var(--glass-border)', background: 'none', color: clientPage === 0 ? 'var(--text-muted)' : 'var(--text-color)', cursor: clientPage === 0 ? 'default' : 'pointer', fontSize: '0.78rem' }}>
                                    Prev
                                </button>
                                <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', alignSelf: 'center' }}>
                                    {clientPage + 1} / {clientListPages}
                                </span>
                                <button disabled={clientPage >= clientListPages - 1} onClick={() => handleModalPageChange(clientPage + 1)}
                                    style={{ padding: '0.25rem 0.6rem', borderRadius: '6px', border: '1px solid var(--glass-border)', background: 'none', color: clientPage >= clientListPages - 1 ? 'var(--text-muted)' : 'var(--text-color)', cursor: clientPage >= clientListPages - 1 ? 'default' : 'pointer', fontSize: '0.78rem' }}>
                                    Next
                                </button>
                            </div>
                        )}

                        {/* Footer */}
                        <div style={{ padding: '0.9rem 1.25rem', borderTop: '1px solid var(--glass-border)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                            <input
                                type="text"
                                placeholder="Created by (optional)"
                                value={batchCreatedBy}
                                onChange={e => setBatchCreatedBy(e.target.value)}
                                style={{
                                    flex: 1, padding: '0.4rem 0.7rem', borderRadius: '6px',
                                    background: 'var(--input-bg, rgba(255,255,255,0.07))',
                                    border: '1px solid var(--glass-border)', color: 'var(--text-color)',
                                    fontFamily: 'inherit', fontSize: '0.82rem', outline: 'none',
                                }}
                            />
                            <Button variant="secondary" onClick={closeJobModal} style={{ fontSize: '0.82rem', padding: '0.4rem 0.9rem' }}>
                                Cancel
                            </Button>
                            <Button variant="primary"
                                onClick={handleJobSubmit}
                                disabled={selectedClientIds.size === 0 || jobSubmitting}
                                style={{ fontSize: '0.82rem', padding: '0.4rem 0.9rem', opacity: selectedClientIds.size === 0 ? 0.5 : 1 }}>
                                {jobSubmitting ? 'Enqueueing...' : `Enqueue (${selectedClientIds.size})`}
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminJobScheduler;
