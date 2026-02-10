import apiClient from './apiClient';

const API_BASE_URL = '/cases';

export const caseService = {
    getCases: async (page = 0) => {
        const data = await apiClient.get(API_BASE_URL);
        // Backend returns a plain list for now, wrap it to match expected pagination structure
        return {
            content: data,
            totalPages: 1,
            totalElements: data.length,
            size: data.length,
            number: 0
        };
    },

    getCasesByClient: async (clientID) => {
        return apiClient.get(`${API_BASE_URL}/client/${clientID}`);
    },

    getCaseDetails: async (id) => {
        return apiClient.get(`${API_BASE_URL}/${id}`);
    },

    getCaseComments: async (id) => {
        return apiClient.get(`${API_BASE_URL}/${id}/comments`);
    },

    getCaseTimeline: async (id) => {
        return apiClient.get(`${API_BASE_URL}/${id}/timeline`);
    },

    getCaseActions: async (id) => {
        return apiClient.get(`${API_BASE_URL}/${id}/actions`);
    },

    triggerCaseAction: async (id, actionId, variables = {}) => {
        return apiClient.post(`${API_BASE_URL}/${id}/actions/${actionId}`, variables);
    },

    getCaseDocuments: async (id) => {
        return apiClient.get(`${API_BASE_URL}/${id}/documents`);
    },

    getDocumentVersions: async (id, name) => {
        return apiClient.get(`${API_BASE_URL}/${id}/documents/versions?name=${encodeURIComponent(name)}`);
    },

    getCaseEvents: async (id) => {
        return apiClient.get(`${API_BASE_URL}/${id}/events`);
    },

    transitionCase: async (id, action, comment) => {
        try {
            return await apiClient.post(`${API_BASE_URL}/${id}/transition`, { action, comment });
        } catch (error) {
            // Basic error handling wrapper if needed, or let apiClient throw
            // If we need specific 400 handling:
            if (error.message && error.message.includes('400')) {
                throw new Error('Case validation failed. Ensure all mandatory requirements are met.');
            }
            throw error;
        }
    },

    createCase: async (clientID, reason, comment) => {
        return apiClient.post(API_BASE_URL, { clientID, reason, comment });
    },

    uploadDocument: async (caseId, formData) => {
        const token = localStorage.getItem('token');
        // Manually prepend /api since we are bypassing apiClient which handles the base URL
        const response = await fetch(`/api${API_BASE_URL}/${caseId}/documents`, {
            method: 'POST',
            body: formData,
            headers: {
                ...(token ? { 'Authorization': `Bearer ${token}` } : {})
            }
        });
        if (!response.ok) throw new Error('Failed to upload document');
    },

    getUserTasks: async () => {
        return apiClient.get(`${API_BASE_URL}/tasks`);
    },

    assignCase: async (id, assignee) => {
        return apiClient.post(`${API_BASE_URL}/${id}/assign`, { assignee });
    },

    getUsersByRole: async (role) => {
        return apiClient.get(`/users/role/${role}`);
    },

    getAllUsers: async () => {
        return apiClient.get('/users');
    },

    // Admin Workflow
    getAdminTasks: async () => {
        return apiClient.get(`${API_BASE_URL}/admin/tasks`);
    },

    getAdminProcesses: async () => {
        return apiClient.get(`${API_BASE_URL}/admin/processes`);
    },

    terminateProcess: async (id) => {
        return apiClient.delete(`${API_BASE_URL}/admin/processes/${id}`);
    },

    deleteAllTasks: async () => {
        return apiClient.delete(`${API_BASE_URL}/tasks`);
    },

    completeTask: async (taskId) => {
        return apiClient.post(`${API_BASE_URL}/tasks/${taskId}/complete`, {});
    }
};
