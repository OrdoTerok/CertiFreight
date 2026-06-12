import { within, type ByRoleOptions } from '@testing-library/react';

const normalize = (value: string) => value.toLowerCase().replace(/\s+/g, ' ').trim();

/**
 * AI-inspired fuzzy role query for unit/component tests.
 * Falls back to nearest text match when exact role-name lookup drifts.
 */
export const getByRoleSelfHealing = (
    container: HTMLElement,
    role: string,
    options: ByRoleOptions & { fallbackNames?: string[] } = {}
): HTMLElement => {
    const scoped = within(container);

    try {
        return scoped.getByRole(role as any, options);
    } catch {
        const fallbackNames = options.fallbackNames ?? [];
        for (const name of fallbackNames) {
            try {
                return scoped.getByRole(role as any, { ...options, name });
            } catch {
                // Try next fallback.
            }
        }

        // Last resort: fuzzy text search.
        if (typeof options.name === 'string') {
            const wanted = normalize(options.name);
            const candidate = scoped
                .queryAllByText(/.+/)
                .find((node) => normalize(node.textContent ?? '').includes(wanted));
            if (candidate) {
                return candidate as HTMLElement;
            }
        }

        throw new Error(`getByRoleSelfHealing: role '${role}' not found with provided strategies.`);
    }
};

