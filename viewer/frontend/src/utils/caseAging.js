const TERMINAL_STATUSES = new Set(['APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED']);

const WARN_DAYS = 5;
const OVERDUE_DAYS = 10;

/**
 * Aging info for a case based on createdDate. Resolved cases are never flagged
 * as warn/overdue regardless of age — only open cases accrue aging risk.
 */
export const getAgingInfo = (createdDate, status) => {
    if (!createdDate) return { days: null, level: 'ok' };

    const created = new Date(createdDate);
    const days = Math.floor((Date.now() - created.getTime()) / (1000 * 60 * 60 * 24));

    if (TERMINAL_STATUSES.has(status)) {
        return { days, level: 'ok' };
    }

    let level = 'ok';
    if (days >= OVERDUE_DAYS) level = 'overdue';
    else if (days >= WARN_DAYS) level = 'warn';

    return { days, level };
};

export const AGING_BADGE_VARIANT = {
    ok: 'active',
    warn: 'pending',
    overdue: 'rejected',
};
