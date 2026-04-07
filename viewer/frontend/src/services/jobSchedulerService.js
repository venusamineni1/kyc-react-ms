import apiClient from './apiClient';

const API_BASE = '/internal/scheduler';

export const jobSchedulerService = {
    /** Dashboard statistics */
    getStats: () =>
        apiClient.get(`${API_BASE}/stats`),

    /** All recurring job definitions */
    getRecurringJobs: () =>
        apiClient.get(`${API_BASE}/recurring`),

    /** Delete a recurring job */
    deleteRecurringJob: (id) =>
        apiClient.delete(`${API_BASE}/recurring/${encodeURIComponent(id)}`),

    /** Trigger a recurring job immediately */
    triggerRecurringJob: (id) =>
        apiClient.post(`${API_BASE}/recurring/${encodeURIComponent(id)}/trigger`),

    /** Paginated jobs by state */
    getJobs: (state = 'ENQUEUED', offset = 0, limit = 20) =>
        apiClient.get(`${API_BASE}/jobs?state=${state}&offset=${offset}&limit=${limit}`),

    /** Active jobs — ENQUEUED + SCHEDULED + PROCESSING combined */
    getActiveJobs: (offset = 0, limit = 20) =>
        apiClient.get(`${API_BASE}/jobs/active?offset=${offset}&limit=${limit}`),

    /** Requeue a failed job */
    requeueJob: (id) =>
        apiClient.post(`${API_BASE}/jobs/${id}/requeue`),

    /** Delete a job permanently */
    deleteJob: (id) =>
        apiClient.delete(`${API_BASE}/jobs/${id}`),

    /** Enqueue (or schedule) a one-off screening batch job for the given client IDs */
    enqueueBatchScreeningJob: (clientIds, createdBy, scheduledAt = null) =>
        apiClient.post(`${API_BASE}/jobs/enqueue-batch-screening`, { clientIds, createdBy, scheduledAt }),

    /** Enqueue (or schedule) a one-off periodic review case creation job for the given client IDs */
    enqueuePeriodicReviewJob: (clientIds, createdBy, scheduledAt = null) =>
        apiClient.post(`${API_BASE}/jobs/enqueue-periodic-review`, { clientIds, createdBy, scheduledAt }),
};
