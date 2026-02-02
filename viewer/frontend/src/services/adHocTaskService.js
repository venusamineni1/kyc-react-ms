import apiClient from './apiClient';

const API_URL = '/adhoc-tasks';

export const adHocTaskService = {
    createTask: async (payload) => {
        // apiClient auto-parses JSON. If response is text, it handles it?
        // Let's check apiClient implementation.
        // It checks content-type: application/json. If not, returns text.
        // So this is safe.
        return apiClient.post(API_URL, payload);
    },

    getMyTasks: async () => {
        return apiClient.get(API_URL);
    },

    getTask: async (id) => {
        return apiClient.get(`${API_URL}/${id}`);
    },

    respondTask: async (id, responseText) => {
        return apiClient.post(`${API_URL}/${id}/respond`, { responseText });
    },

    reassignTask: async (id, assignee) => {
        return apiClient.post(`${API_URL}/${id}/reassign`, { assignee });
    },

    completeTask: async (id) => {
        return apiClient.post(`${API_URL}/${id}/complete`, {});
    }
};
