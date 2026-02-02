const apiClient = {
    // Base URL could come from env vars in the future
    BASE_URL: '/api',

    /**
     * Generic fetch wrapper
     * @param {string} endpoint - Relative API endpoint (e.g. '/screening/initiate')
     * @param {object} options - Fetch options (method, body, etc.)
     * @returns {Promise<any>} - JSON response or throws Error
     */
    async request(endpoint, options = {}) {
        const url = `${this.BASE_URL}${endpoint}`;

        const token = localStorage.getItem('token');
        const defaultHeaders = {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        };

        const config = {
            ...options,
            headers: {
                ...defaultHeaders,
                ...options.headers,
            },
        };

        try {
            const response = await fetch(url, config);

            // Handle non-2xx responses
            if (!response.ok) {
                // Try to parse JSON error from backend (using our new ErrorDTO structure)
                let errorMessage = `API Error ${response.status}`;
                try {
                    const errorData = await response.json();
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    } else if (errorData.error) {
                        errorMessage = `${errorData.status} ${errorData.error}`;
                    }
                } catch (e) {
                    // Fallback to text if JSON parse fails
                    const text = await response.text();
                    if (text) errorMessage = text;
                }

                throw new Error(errorMessage);
            }

            // Check if response has content
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1) {
                return await response.json();
            } else {
                return await response.text();
            }
        } catch (error) {
            console.error(`API Request Failed: ${endpoint}`, error);
            throw error; // Re-throw for component handling
        }
    },

    get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    },

    post(endpoint, body) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(body),
        });
    },

    put(endpoint, body) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(body),
        });
    },

    delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }
};

export default apiClient;
