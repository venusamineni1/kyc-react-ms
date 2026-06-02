import apiClient from './apiClient';

export const screeningService = {
    /**
     * Initiate NRTS screening for a client
     * @param {Object} clientDetails - Full client details required for screening
     * @param {number} clientDetails.clientId - Client ID
     * @param {string} clientDetails.firstName - Client first name
     * @param {string} clientDetails.lastName - Client last name
     * @param {string} clientDetails.dateOfBirth - Client DOB (YYYY-MM-DD)
     * @param {string} clientDetails.citizenship - Client citizenship
     * @param {number} [clientDetails.statusCheckDelayMs=0] - Delay before status check
     * @returns {Promise<Object>} {result, processId, reqId, alertContexts}
     */
    async initiateScreening(clientDetails) {
        return apiClient.post('/screening/initiate', clientDetails);
    },

    /**
     * Get screening status by process ID
     * @param {string|number} processId - NRTS process ID from initiate response
     * @returns {Promise<Object>} {requestId, overallStatus, finalized, reqId, results}
     */
    async getScreeningStatus(processId) {
        return apiClient.get(`/screening/status/${processId}`);
    },

    /**
     * Get screening history for a client
     * @param {number} clientId - Client ID
     * @returns {Promise<Array>} Array of screening logs
     */
    async getHistory(clientId) {
        return apiClient.get(`/screening/history/${clientId}`);
    }
};
