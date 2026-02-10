import apiClient from './apiClient';

const API_BASE_URL = '/clients';

export const clientService = {
    getClients: async (page = 0, query = '') => {
        const url = query
            ? `${API_BASE_URL}/search?query=${encodeURIComponent(query)}&page=${page}`
            : `${API_BASE_URL}?page=${page}`;
        return apiClient.get(url);
    },

    getClientDetails: async (id) => {
        return apiClient.get(`${API_BASE_URL}/${id}`);
    },

    exportClients: async (startDate, endDate) => {
        let url = `${API_BASE_URL}/changes/export`;
        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        if (params.toString()) url += `?${params.toString()}`;
        return apiClient.get(url);
    },

    addRelatedParty: async (id, partyData) => {
        return apiClient.post(`${API_BASE_URL}/${id}/related-parties`, partyData);
    },

    getRelatedPartyDetails: async (partyId) => {
        return apiClient.get(`${API_BASE_URL}/related-parties/${partyId}`);
    },

    ingestClientChange: async (id, newData) => {
        return apiClient.post(`${API_BASE_URL}/${id}/ingest`, newData);
    },

    getClientChanges: async (id) => {
        return apiClient.get(`${API_BASE_URL}/${id}/changes`);
    },

    triggerRisk: async (changeId) => {
        return apiClient.post(`${API_BASE_URL}/changes/${changeId}/trigger-risk`);
    },

    triggerScreening: async (changeId) => {
        return apiClient.post(`${API_BASE_URL}/changes/${changeId}/trigger-screening`);
    },

    reviewChange: async (changeId) => {
        return apiClient.post(`${API_BASE_URL}/changes/${changeId}/review`);
    },

    getAdminConfigs: async () => {
        return apiClient.get(`${API_BASE_URL}/admin/configs`);
    },

    saveAdminConfig: async (config) => {
        return apiClient.post(`${API_BASE_URL}/admin/configs`, config);
    },

    getMaterialChanges: async (page = 0, size = 10) => {
        return apiClient.get(`${API_BASE_URL}/changes?page=${page}&size=${size}`);
    }
};
