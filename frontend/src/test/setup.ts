import '@testing-library/jest-dom';
import { cleanup } from '@testing-library/react';
import { beforeAll, afterEach, afterAll } from 'vitest';
import { server } from './mocks/server';
import axios from 'axios';

// CRITICAL FIX: Forces Axios to route through XHR so MSW can watch it in Node/jsdom
axios.defaults.adapter = 'xhr';

// Establish API mocking before all tests
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

// Automatically unmount React trees after every single test run to prevent memory leaks
afterEach(() => {
    server.resetHandlers();
    cleanup();
});

// Clean up after the suite finishes
afterAll(() => server.close());