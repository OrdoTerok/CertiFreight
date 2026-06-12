import { expect } from '@playwright/test';

/**
 * Try multiple locator strategies and return the first visible match.
 * This gives E2E tests a self-healing path when text or roles drift.
 */
export const adaptiveLocator = async (page, strategies, timeoutMs = 1200) => {
    for (const strategy of strategies) {
        const locator = strategy(page);
        const count = await locator.count();
        if (count === 0) {
            continue;
        }

        const first = locator.first();
        const visible = await first.isVisible().catch(() => false);
        if (visible) {
            return first;
        }

        // Keep trying if this candidate exists but is not interactable.
        await expect(first).toBeVisible({ timeout: timeoutMs }).catch(() => undefined);
        if (await first.isVisible().catch(() => false)) {
            return first;
        }
    }

    throw new Error('adaptiveLocator failed: no strategy produced a visible element.');
};

export const clickAdaptive = async (page, strategies, timeoutMs = 1200) => {
    const locator = await adaptiveLocator(page, strategies, timeoutMs);
    await locator.click();
};

