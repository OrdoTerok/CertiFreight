import '@testing-library/jest-dom';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Automatically unmount React trees after every single test run to prevent memory leaks
afterEach(() => {
    cleanup();
});