import apiClient from './apiClient';

const API_BASE_URL = '/risk/batch';

export const riskBatchService = {
    // 1. Create Batch
    createBatch: async (clients) => {
        // Returns batchId string
        const response = await apiClient.post(`${API_BASE_URL}/create`, clients);
        return response;
    },

    // 2. Generate JSONL
    generateJsonl: async (batchId) => {
        return apiClient.post(`${API_BASE_URL}/${batchId}/generate-jsonl`);
    },

    // 3. Zip
    zipFiles: async (batchId) => {
        return apiClient.post(`${API_BASE_URL}/${batchId}/zip`);
    },

    // 4. Generate Control File
    generateControl: async (batchId) => {
        return apiClient.post(`${API_BASE_URL}/${batchId}/generate-control`);
    },

    // 5. Upload
    upload: async (batchId) => {
        return apiClient.post(`${API_BASE_URL}/${batchId}/upload`);
    },

    // Get File Content
    getFileContent: async (batchId, type) => {
        return apiClient.get(`${API_BASE_URL}/${batchId}/file-content?type=${type}`);
    },

    // Legacy / Test
    generateTestJson: async (client) => {
        return apiClient.post(`${API_BASE_URL}/test-generate`, client);
    },

    initiateBatch: async (clients) => {
        return apiClient.post(`${API_BASE_URL}/initiate`, clients);
    }
};
