import apiClient from './apiClient';

const API_BASE_URL = '/risk';

export const riskService = {
    calculateRisk: async (clientId) => {
        console.log('Requesting Risk Evaluation for client:', clientId);

        if (!clientId) {
            console.error('Missing Client ID');
            throw new Error('Client ID is missing');
        }

        return apiClient.post(`${API_BASE_URL}/evaluate/${clientId}`);
    },

    getRiskHistory: async (clientId) => {
        return apiClient.get(`${API_BASE_URL}/assessments/${clientId}`);
    },

    getAssessmentDetails: async (assessmentId) => {
        return apiClient.get(`${API_BASE_URL}/assessment-details/${assessmentId}`);
    }
};
