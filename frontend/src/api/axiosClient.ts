import axios from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

axiosClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('certifreight_token');

        // 1. Only inject the token if it exists AND the request hasn't explicitly cleared it
        if (token && config.headers && config.headers.Authorization !== '') {
            config.headers.Authorization = `Bearer ${token}`;
        }

        // 2. If Authorization was explicitly set to an empty string to simulate a breach,
        // delete the key entirely so it transmits a raw, unauthenticated network call.
        if (config.headers && config.headers.Authorization === '') {
            delete config.headers.Authorization;
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default axiosClient;