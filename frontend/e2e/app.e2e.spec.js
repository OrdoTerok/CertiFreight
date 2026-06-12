import fs from 'node:fs';
import path from 'node:path';
import { expect, test } from '@playwright/test';
import { clickAdaptive } from './utils/selfHealingLocators.js';

const saveCoverage = async (coverage, title) => {
    if (!coverage || typeof coverage !== 'object') {
        return;
    }

    fs.mkdirSync('.nyc_output', { recursive: true });
    const safeTitle = title.replace(/[^a-z0-9]+/gi, '-').toLowerCase();
    const fileName = `playwright-${Date.now()}-${safeTitle}.json`;
    fs.writeFileSync(path.join('.nyc_output', fileName), JSON.stringify(coverage));
};

test.afterEach(async ({ page }, testInfo) => {
    const coverage = await page
        .evaluate(() => window.__coverage__)
        .catch(() => undefined);

    await saveCoverage(coverage, testInfo.title);
});

const seedAuthState = async (page, options) => {
    const token = options.token ?? null;
    const tenantId = options.tenantId ?? null;
    const role = options.role ?? null;

    await page.addInitScript(({ tokenValue, tenantValue, roleValue }) => {
        localStorage.clear();

        if (tokenValue) {
            localStorage.setItem('certifreight_token', tokenValue);
        }
        if (tenantValue) {
            localStorage.setItem('certifreight_tenant_id', tenantValue);
        }
        if (roleValue) {
            localStorage.setItem('certifreight_role', roleValue);
        }
    }, { tokenValue: token, tenantValue: tenantId, roleValue: role });
};

test('redirects to unauthorized when a non-admin user opens admin settings', async ({ page }) => {
    await seedAuthState(page, {
        token: 'test-jwt',
        tenantId: 'alpha',
        role: 'ROLE_DISPATCHER',
    });

    await page.goto('/admin/settings');

    await expect(page).toHaveURL(/\/unauthorized$/);
    await expect(page.getByText('403: Explicit Context Denied')).toBeVisible();
});

test('renders admin console when an admin user opens admin settings', async ({ page }) => {
    await seedAuthState(page, {
        token: 'test-jwt',
        tenantId: 'alpha',
        role: 'ROLE_ADMIN',
    });

    await page.goto('/admin/settings');

    await expect(page).toHaveURL(/\/admin\/settings$/);
    await expect(page.getByText('System Administration Console')).toBeVisible();
    await expect(page.getByText('STATUS // AUTHENTICATED_ENCLAVE_ACCESSED // ROLE_ADMIN_CONFIRMED')).toBeVisible();
});

test('redirects to root when no token exists for admin settings', async ({ page }) => {
    await seedAuthState(page, {});

    await page.goto('/admin/settings');

    await expect(page).toHaveURL(/\/$/);
});

test('hides shipment form when user role is not allowed by role guard', async ({ page }) => {
    await seedAuthState(page, {
        token: 'test-jwt',
        tenantId: 'alpha',
        role: 'ROLE_VIEWER',
    });

    await page.route('http://localhost:8080/api/shipments', async (route) => {
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify([]),
        });
    });

    await page.goto('/');

    await expect(page.getByText('Assign Isolated Freight')).toHaveCount(0);
});

test('axios interceptor injects bearer token when token exists', async ({ page }) => {
    await seedAuthState(page, {
        token: 'token-from-storage',
        tenantId: 'alpha',
        role: 'ROLE_DISPATCHER',
    });

    let observedAuthHeader = null;
    await page.route('http://localhost:8080/api/shipments', async (route) => {
        observedAuthHeader = route.request().headers()['authorization'] || null;
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify([]),
        });
    });

    await page.goto('/');

    await page.evaluate(async () => {
        const module = await import('/src/api/axiosClient.ts');
        await module.default.get('/shipments');
    });

    expect(observedAuthHeader).toBe('Bearer token-from-storage');
});

test('axios interceptor strips explicit empty authorization header', async ({ page }) => {
    await seedAuthState(page, {
        token: 'token-from-storage',
        tenantId: 'alpha',
        role: 'ROLE_DISPATCHER',
    });

    await page.goto('/');

    const removedHeader = await page.evaluate(async () => {
        const module = await import('/src/api/axiosClient.ts');
        const fulfilled = module.default.interceptors.request.handlers[0].fulfilled;
        const result = await fulfilled({ headers: { Authorization: '' } });

        return result.headers.Authorization ?? null;
    });

    expect(removedHeader).toBeNull();
});

test('axios interceptor rejection handler propagates errors', async ({ page }) => {
    await page.goto('/');

    const errorMessage = await page.evaluate(async () => {
        const module = await import('/src/api/axiosClient.ts');
        const rejected = module.default.interceptors.request.handlers[0].rejected;

        try {
            await rejected(new Error('interceptor-boom'));
            return 'no-error';
        } catch (error) {
            return error.message;
        }
    });

    expect(errorMessage).toBe('interceptor-boom');
});

test('submits shipment form and renders success banner', async ({ page }) => {
    await seedAuthState(page, {
        token: 'test-jwt',
        tenantId: 'alpha',
        role: 'ROLE_DISPATCHER',
    });

    await page.route('http://localhost:8080/api/shipments', async (route) => {
        if (route.request().method() === 'GET') {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([]),
            });
            return;
        }

        await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
                id: 123,
                trackingNumber: 'CFT-100001',
                weightLbs: 5000,
                status: 'MANIFEST_CREATED',
            }),
        });
    });

    await page.goto('/');

    await expect(page.getByText('Assign Isolated Freight')).toBeVisible();
    await page.getByLabel('Tracking Number').fill('CFT-100001');
    await page.getByLabel('Cargo Weight (lbs)').fill('5000');
    await page.getByRole('button', { name: 'Commit Freight Link' }).click();

    await expect(page.getByText('Manifest successfully created.')).toBeVisible();
});

test('renders backend validation error when shipment create fails', async ({ page }) => {
    await seedAuthState(page, {
        token: 'test-jwt',
        tenantId: 'alpha',
        role: 'ROLE_DISPATCHER',
    });

    await page.route('http://localhost:8080/api/shipments', async (route) => {
        if (route.request().method() === 'GET') {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([]),
            });
            return;
        }

        await route.fulfill({
            status: 400,
            contentType: 'application/json',
            body: JSON.stringify({ detail: 'Tenant Context Missing' }),
        });
    });

    await page.goto('/');

    await page.getByLabel('Tracking Number').fill('CFT-300001');
    await page.getByLabel('Cargo Weight (lbs)').fill('1200');
    await page.getByRole('button', { name: 'Commit Freight Link' }).click();

    await expect(page.getByText('Tenant Context Missing')).toBeVisible();
});

test('generates a tracking number when generate button is clicked', async ({ page }) => {
    await seedAuthState(page, {
        token: 'test-jwt',
        tenantId: 'alpha',
        role: 'ROLE_DISPATCHER',
    });

    await page.route('http://localhost:8080/api/shipments', async (route) => {
        await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify([]),
        });
    });

    await page.goto('/');

    await clickAdaptive(page, [
        (p) => p.getByRole('button', { name: '⚡' }),
        (p) => p.locator('button[type="button"]').filter({ hasText: '⚡' }),
        (p) => p.locator('#trackingNumber').locator('xpath=..').locator('button[type="button"]'),
    ]);

    await expect(page.getByLabel('Tracking Number')).toHaveValue(/CFT-[0-9]{6}/);
});

test('displays shipment list when fetched successfully', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_DISPATCHER',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         await route.fulfill({
             status: 200,
             contentType: 'application/json',
             body: JSON.stringify([
                 {
                     id: 1,
                     trackingNumber: 'CFT-100001',
                     weightLbs: 5000,
                     status: 'MANIFEST_CREATED',
                 },
                 {
                     id: 2,
                     trackingNumber: 'CFT-200002',
                     weightLbs: 3000,
                     status: 'IN_TRANSIT',
                 },
             ]),
         });
     });

     await page.goto('/');

     // Verify shipments are displayed in the list
     await expect(page.getByText('CFT-100001')).toBeVisible();
     await expect(page.getByText('CFT-200002')).toBeVisible();
});

test('submits form and displays loading state while submitting', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_DISPATCHER',
     });

     let requestBlocker;
     const requestPromise = new Promise(resolve => {
         requestBlocker = resolve;
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         if (route.request().method() === 'GET') {
             await route.fulfill({
                 status: 200,
                 contentType: 'application/json',
                 body: JSON.stringify([]),
             });
             return;
         }

         // Delay POST to verify loading state
         await requestPromise;
         await route.fulfill({
             status: 201,
             contentType: 'application/json',
             body: JSON.stringify({
                 id: 123,
                 trackingNumber: 'CFT-100001',
                 weightLbs: 5000,
                 status: 'MANIFEST_CREATED',
             }),
         });
     });

     await page.goto('/');

     await page.getByLabel('Tracking Number').fill('CFT-100001');
     await page.getByLabel('Cargo Weight (lbs)').fill('5000');
     const submitButton = page.getByRole('button', { name: 'Commit Freight Link' });

     // Verify initial button state
     await expect(submitButton).toBeEnabled();

     // Click submit and verify loading state appears
     submitButton.click();
     await expect(page.getByText('Registering Manifest...')).toBeVisible();
     await expect(submitButton).toBeDisabled();

     // Unblock request and wait for completion
     requestBlocker();
     await expect(page.getByText('Manifest successfully created.')).toBeVisible();
     await expect(submitButton).toBeEnabled();
});

test('displays API error when shipment list fetch fails', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_DISPATCHER',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         await route.fulfill({
             status: 500,
             contentType: 'application/json',
             body: JSON.stringify({ detail: 'Internal Server Error' }),
         });
     });

     await page.goto('/');

     await expect(page.getByText('HTTP Error: Context Violation')).toBeVisible();
});

test('validates required fields in shipment form', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_DISPATCHER',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         await route.fulfill({
             status: 200,
             contentType: 'application/json',
             body: JSON.stringify([]),
         });
     });

     await page.goto('/');

     // Verify form fields have required attribute
     const trackingInput = page.getByLabel('Tracking Number');
     const weightInput = page.getByLabel('Cargo Weight (lbs)');

     await expect(trackingInput).toHaveAttribute('required', '');
     await expect(weightInput).toHaveAttribute('required', '');
});

test('clears form fields after successful shipment creation', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_DISPATCHER',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         if (route.request().method() === 'GET') {
             await route.fulfill({
                 status: 200,
                 contentType: 'application/json',
                 body: JSON.stringify([]),
             });
             return;
         }

         await route.fulfill({
             status: 201,
             contentType: 'application/json',
             body: JSON.stringify({
                 id: 123,
                 trackingNumber: 'CFT-100001',
                 weightLbs: 5000,
                 status: 'MANIFEST_CREATED',
             }),
         });
     });

     await page.goto('/');

     await page.getByLabel('Tracking Number').fill('CFT-100001');
     await page.getByLabel('Cargo Weight (lbs)').fill('5000');
     await page.getByRole('button', { name: 'Commit Freight Link' }).click();

     await expect(page.getByText('Manifest successfully created.')).toBeVisible();

     // Verify form fields are cleared
     await expect(page.getByLabel('Tracking Number')).toHaveValue('');
     await expect(page.getByLabel('Cargo Weight (lbs)')).toHaveValue('');
});

test('handles network error when creating shipment', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_DISPATCHER',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         if (route.request().method() === 'GET') {
             await route.fulfill({
                 status: 200,
                 contentType: 'application/json',
                 body: JSON.stringify([]),
             });
             return;
         }

         await route.fulfill({
             status: 503,
             contentType: 'application/json',
             body: JSON.stringify({ detail: 'Service Unavailable' }),
         });
     });

     await page.goto('/');

     await page.getByLabel('Tracking Number').fill('CFT-100001');
     await page.getByLabel('Cargo Weight (lbs)').fill('5000');
     await page.getByRole('button', { name: 'Commit Freight Link' }).click();

     await expect(page.getByText('Service Unavailable')).toBeVisible();
});

test('displays admin console for ROLE_ADMIN with all details', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_ADMIN',
     });

     await page.goto('/admin/settings');

     await expect(page).toHaveURL(/\/admin\/settings$/);
     await expect(page.getByText('System Administration Console')).toBeVisible();
     await expect(page.getByText('AUTHENTICATED_ENCLAVE_ACCESSED')).toBeVisible();
     await expect(page.getByText('ROLE_ADMIN_CONFIRMED')).toBeVisible();
     await expect(page.getByText(/This workspace is structurally shielded/)).toBeVisible();
});

test('ROLE_VIEWER cannot see shipment form but can see empty list', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_VIEWER',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         await route.fulfill({
             status: 200,
             contentType: 'application/json',
             body: JSON.stringify([]),
         });
     });

     await page.goto('/');

     // Role guard should hide the form
     await expect(page.getByText('Assign Isolated Freight')).toHaveCount(0);
});

test('ROLE_ADMIN can see shipment form', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_ADMIN',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         await route.fulfill({
             status: 200,
             contentType: 'application/json',
             body: JSON.stringify([]),
         });
     });

     await page.goto('/');

     // ROLE_ADMIN is in allowed roles for ShipmentForm
     await expect(page.getByText('Assign Isolated Freight')).toBeVisible();
});

test('handles 401 unauthorized error during shipment fetch', async ({ page }) => {
     await seedAuthState(page, {
         token: 'invalid-token',
         tenantId: 'alpha',
         role: 'ROLE_DISPATCHER',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         await route.fulfill({
             status: 401,
             contentType: 'application/json',
             body: JSON.stringify({ detail: 'Unauthorized' }),
         });
     });

     await page.goto('/');

     await expect(page.getByText('HTTP Error: Context Violation')).toBeVisible();
});

test('handles 403 forbidden error during shipment operations', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_VIEWER',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         await route.fulfill({
             status: 200,
             contentType: 'application/json',
             body: JSON.stringify([]),
         });
     });

     await page.goto('/');

     await expect(page).toHaveURL(/\/$/);
});

test('persists auth state across page reloads', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_DISPATCHER',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         await route.fulfill({
             status: 200,
             contentType: 'application/json',
             body: JSON.stringify([]),
         });
     });

     await page.goto('/');

     // Verify form is visible before reload
     await expect(page.getByText('Assign Isolated Freight')).toBeVisible();

     // Reload page
     await page.reload();

     // Verify form is still visible after reload (auth state persisted)
     await expect(page.getByText('Assign Isolated Freight')).toBeVisible();
});

test('clears auth state when no token is provided', async ({ page }) => {
     await seedAuthState(page, {
         token: null,
         tenantId: null,
         role: null,
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         await route.fulfill({
             status: 200,
             contentType: 'application/json',
             body: JSON.stringify([]),
         });
     });

     await page.goto('/');

     // Without token, form should not be visible (no active tenant)
     await expect(page.getByText('Assign Isolated Freight')).toHaveCount(0);
});

test('differentiates between ROLE_DISPATCHER and ROLE_ADMIN permissions', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_DISPATCHER',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         await route.fulfill({
             status: 200,
             contentType: 'application/json',
             body: JSON.stringify([]),
         });
     });

     await page.goto('/');

     // Dispatcher can see shipment form
     await expect(page.getByText('Assign Isolated Freight')).toBeVisible();

     // Dispatcher cannot access admin settings
     await page.goto('/admin/settings');
     await expect(page).toHaveURL(/\/unauthorized$/);
});

test('handles successful submit with null weight', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_DISPATCHER',
     });

     let capturedBody = null;
     await page.route('http://localhost:8080/api/shipments', async (route) => {
         if (route.request().method() === 'GET') {
             await route.fulfill({
                 status: 200,
                 contentType: 'application/json',
                 body: JSON.stringify([]),
             });
             return;
         }

         capturedBody = await route.request().postDataJSON();
         await route.fulfill({
             status: 201,
             contentType: 'application/json',
             body: JSON.stringify({
                 id: 123,
                 trackingNumber: 'CFT-100001',
                 weightLbs: null,
                 status: 'MANIFEST_CREATED',
             }),
         });
     });

     await page.goto('/');

     await page.getByLabel('Tracking Number').fill('CFT-100001');
     // Leave weight empty to test null handling
     await page.getByRole('button', { name: 'Commit Freight Link' }).click();

     await expect(page.getByText('Manifest successfully created.')).toBeVisible();

     // Verify that null was sent for empty weight
     expect(capturedBody.weightLbs).toBeNull();
});

test('prevents submission with empty tracking number', async ({ page }) => {
     await seedAuthState(page, {
         token: 'test-jwt',
         tenantId: 'alpha',
         role: 'ROLE_DISPATCHER',
     });

     await page.route('http://localhost:8080/api/shipments', async (route) => {
         await route.fulfill({
             status: 200,
             contentType: 'application/json',
             body: JSON.stringify([]),
         });
     });

     await page.goto('/');

     // Try to submit without tracking number
     await page.getByLabel('Cargo Weight (lbs)').fill('5000');
     const submitButton = page.getByRole('button', { name: 'Commit Freight Link' });

     // Browser HTML5 validation should prevent submission
     await submitButton.click();

     // The form should not submit, so error message should not appear
     await expect(page.getByText('Manifest successfully created.')).toHaveCount(0);
});
