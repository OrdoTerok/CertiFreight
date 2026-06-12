import { defineConfig } from '@playwright/test';

export default defineConfig({
    testDir: './e2e',
    timeout: 30000,
    fullyParallel: true,
    retries: 0,
    reporter: [['list']],
    use: {
        baseURL: 'http://127.0.0.1:4173',
        trace: 'on-first-retry',
        headless: true,
    },
    projects: [
        {
            name: 'chromium',
            use: { browserName: 'chromium' },
        },
    ],
    webServer: {
        command: 'cross-env E2E_COVERAGE=true vite --host 127.0.0.1 --port 4173',
        url: 'http://127.0.0.1:4173',
        reuseExistingServer: true,
        timeout: 120000,
    },
});

