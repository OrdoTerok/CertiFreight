import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { RoleGuard } from './RoleGuard';
import { useAuth } from '../hooks/useAuth';
import React from 'react';

// Intercept your authentication hook to inject controlled states manually
vi.mock('../hooks/useAuth', () => ({
    useAuth: vi.fn()
}));

describe('RoleGuard Component Tier Verification Matrix', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should cleanly render child components when user possesses a permitted role vector', () => {
        // Mock a valid authenticated dispatcher session
        vi.mocked(useAuth).mockReturnValue({
            token: 'valid-jwt-token',
            activeTenantId: 'enterprise-alpha',
            activeRole: 'ROLE_DISPATCHER',
            isLoading: false,
            login: vi.fn(),
            logout: vi.fn()
        });

        render(
            <RoleGuard allowedRoles={['ROLE_DISPATCHER', 'ROLE_ADMIN']}>
                <div data-testid="restricted-view">Secure Dispatch Panel</div>
            </RoleGuard>
        );

        // Verify child layout elements successfully mount to the virtual DOM
        expect(screen.getByTestId('restricted-view')).toBeInTheDocument();
        expect(screen.getByText('Secure Dispatch Panel')).toBeInTheDocument();
    });

    it('should block child execution and render nothing or fallback elements if role profile checks fail', () => {
        // Mock an authenticated dispatcher attempting to access an administrator boundary
        vi.mocked(useAuth).mockReturnValue({
            token: 'valid-jwt-token',
            activeTenantId: 'enterprise-alpha',
            activeRole: 'ROLE_DISPATCHER', // Unauthorized role
            isLoading: false,
            login: vi.fn(),
            logout: vi.fn()
        });

        render(
            <RoleGuard allowedRoles={['ROLE_ADMIN']}>
                <div data-testid="restricted-view">Secure Admin Console</div>
            </RoleGuard>
        );

        // Assert that the restricted content was entirely omitted from the render stream
        expect(screen.queryByTestId('restricted-view')).toBeNull();
    });

    it('should display loading frames or bypass evaluation entirely while security context is initializing', () => {
        // Mock an unresolved promise/loading state from your context
        vi.mocked(useAuth).mockReturnValue({
            token: null,
            activeTenantId: null,
            activeRole: null,
            isLoading: true, // System is loading
            login: vi.fn(),
            logout: vi.fn()
        });

        render(
            <RoleGuard allowedRoles={['ROLE_DISPATCHER']}>
                <div data-testid="restricted-view">Secure Dispatch Panel</div>
            </RoleGuard>
        );

        expect(screen.queryByTestId('restricted-view')).toBeNull();
    });
});