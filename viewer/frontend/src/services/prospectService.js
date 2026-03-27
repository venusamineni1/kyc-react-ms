import apiClient from './apiClient';

const API_BASE_URL = '/prospects';

export const prospectService = {
    getProspects: async (page = 0, size = 10) => {
        return apiClient.get(`${API_BASE_URL}?page=${page}&size=${size}`);
    },

    onboardProspect: async (formData) => {
        return apiClient.post(API_BASE_URL, formData);
    }
};
