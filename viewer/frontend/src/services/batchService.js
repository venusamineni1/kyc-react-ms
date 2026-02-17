import apiClient from './apiClient';

const API_BASE_URL = '/internal/screening/batch';

export const batchService = {
    createBatch: async (clients) => {
        return apiClient.post(`${API_BASE_URL}/create`, clients);
    },

    generateXml: async (batchId) => {
        return apiClient.post(`${API_BASE_URL}/${batchId}/generate-xml`);
    },

    generateChecksum: async (batchId) => {
        return apiClient.post(`${API_BASE_URL}/${batchId}/generate-checksum`);
    },

    zipFiles: async (batchId) => {
        return apiClient.post(`${API_BASE_URL}/${batchId}/zip`);
    },

    encryptFile: async (batchId) => {
        return apiClient.post(`${API_BASE_URL}/${batchId}/encrypt`);
    },

    uploadToSftp: async (batchId) => {
        return apiClient.post(`${API_BASE_URL}/${batchId}/upload`);
    },

    getFileContent: async (batchId, type) => {
        return apiClient.get(`${API_BASE_URL}/${batchId}/file-content?type=${type}`);
    },

    // For legacy or test
    generateTestXml: async (client) => {
        return apiClient.post(`${API_BASE_URL}/test-generate`, client);
    },
    // History
    getRiskHistory: async () => {
        return apiClient.get('/risk/batch/history');
    },

    getScreeningHistory: async () => {
        return apiClient.get('/screening/batch/history');
    }
};
