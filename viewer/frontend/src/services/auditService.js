import apiClient from './apiClient';

export const auditService = {
    async getAudits() {
        return apiClient.get('/admin/audits');
    }
};
