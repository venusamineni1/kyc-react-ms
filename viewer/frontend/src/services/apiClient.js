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

        if (config.body instanceof FormData) {
            delete config.headers['Content-Type'];
        }

        try {
            const response = await fetch(url, config);
            const text = await response.text();

            // Handle non-2xx responses
            if (!response.ok) {
                if (response.status === 401) {
                    const hadToken = !!localStorage.getItem('token');
                    localStorage.removeItem('token');
                    if (hadToken && !window.location.pathname.includes('/login')) {
                        window.location.href = '/login';
                        return;
                    }
                }

                let errorMessage = `API Error ${response.status}`;
                try {
                    const errorData = JSON.parse(text);
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    } else if (errorData.error) {
                        errorMessage = `${errorData.status} ${errorData.error}`;
                    }
                } catch (e) {
                    // text is already read
                    if (text) errorMessage = text;
                }

                throw new Error(errorMessage);
            }

            // Check if response has content
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1 && text) {
                return JSON.parse(text);
            } else {
                return text;
            }
        } catch (error) {
            console.error(`API Request Failed: ${endpoint}`, error);
            throw error; // Re-throw for component handling
        }
    },

    get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    },

    post(endpoint, body, options = {}) {
        const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;
        return this.request(endpoint, {
            method: 'POST',
            body: isFormData ? body : JSON.stringify(body),
            ...options
        });
    },

    put(endpoint, body, options = {}) {
        const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;
        return this.request(endpoint, {
            method: 'PUT',
            body: isFormData ? body : JSON.stringify(body),
            ...options
        });
    },

    delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }
};

export default apiClient;
