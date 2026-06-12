import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { getByRoleSelfHealing } from './selfHealingQuery';

describe('getByRoleSelfHealing', () => {
    it('finds by primary role/name lookup', () => {
        const { container } = render(<button type="button">Commit Freight Link</button>);

        const button = getByRoleSelfHealing(container, 'button', { name: 'Commit Freight Link' });
        expect(button).toBeTruthy();
    });

    it('recovers using fallback names', () => {
        const { container } = render(<button type="button">Submit Manifest</button>);

        const button = getByRoleSelfHealing(container, 'button', {
            name: 'Commit Freight Link',
            fallbackNames: ['Submit Manifest'],
        });

        expect(button.textContent).toContain('Submit Manifest');
    });

    it('recovers using fuzzy text match', () => {
        const { container } = render(
            <div>
                <button type="button">Registering Manifest...</button>
            </div>
        );

        const button = getByRoleSelfHealing(container, 'button', {
            name: 'registering manifest',
            fallbackNames: [],
        });

        expect(button.textContent).toContain('Registering Manifest');
    });
});

