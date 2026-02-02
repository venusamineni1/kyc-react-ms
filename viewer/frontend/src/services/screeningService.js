import apiClient from './apiClient';

export const screeningService = {
    async initiateScreening(clientId) {
        return apiClient.post(`/screening/initiate/${clientId}`);
    },

    async getScreeningStatus(requestId) {
        return apiClient.get(`/screening/status/${requestId}`);
    },

    async getHistory(clientId) {
        return apiClient.get(`/screening/history/${clientId}`);
    }
};
