import axios, { AxiosHeaders } from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

axiosClient.interceptors.request.use(
    (config) => {
        const headers = AxiosHeaders.from(config.headers);
        const explicitAuthorization = headers.get('Authorization');

        // Login must be token-agnostic; stale tokens can make /auth/login fail with 401.
        if (config.url?.includes('/auth/login')) {
            headers.delete('Authorization');
            config.headers = headers;
            return config;
        }

        const token = localStorage.getItem('certifreight_token');

        // 1. Only inject the token if it exists AND the request hasn't explicitly cleared it
        if (token && explicitAuthorization !== '') {
            headers.set('Authorization', `Bearer ${token}`);
        }

        // 2. If Authorization was explicitly set to an empty string to simulate a breach,
        // strip it entirely so the request is truly unauthenticated.
        if (explicitAuthorization === '') {
            headers.delete('Authorization');
        }

        config.headers = headers;

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default axiosClient;