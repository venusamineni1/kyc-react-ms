import apiClient from './apiClient';

const API_BASE_URL = '/questionnaire';

export const questionnaireService = {
    getTemplate: async () => {
        return apiClient.get(`${API_BASE_URL}/template`);
    },

    getResponses: async (caseId) => {
        return apiClient.get(`${API_BASE_URL}/case/${caseId}`);
    },

    saveResponses: async (caseId, responses) => {
        return apiClient.post(`${API_BASE_URL}/case/${caseId}`, responses);
    }
};
