import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ProtectedRoute } from './ProtectedRoute';
import { useAuth } from '../hooks/useAuth';
import React from 'react';

// 1. Intercept the authentication hook state
vi.mock('../hooks/useAuth', () => ({
    useAuth: vi.fn()
}));

// 2. Mock react-router-dom's Navigate component to intercept routing targets
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return {
        ...actual,
        Navigate: vi.fn(({ to }) => <div data-testid="mock-navigate" data-to={to} />)
    };
});

describe('ProtectedRoute Component Gate Matrix', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should cleanly render children when an active authorization token exists', () => {
        // Mock an active, verified security session
        vi.mocked(useAuth).mockReturnValue({
            token: 'active-valid-jwt-token',
            activeTenantId: 'enterprise-alpha',
            activeRole: 'ROLE_DISPATCHER',
            isLoading: false,
            login: vi.fn(),
            logout: vi.fn()
        });

        render(
            <ProtectedRoute>
                <div data-testid="protected-content">Secure Freight Dashboard</div>
            </ProtectedRoute>
        );

        // Assert that the interior layout mounts cleanly to the screen hierarchy
        expect(screen.getByTestId('protected-content')).toBeInTheDocument();
        expect(screen.queryByTestId('mock-navigate')).toBeNull();
    });

    it('should intercept execution and force redirect to /login when token vector is null', () => {
        // Mock an unauthenticated guest user session
        vi.mocked(useAuth).mockReturnValue({
            token: null, // No credentials
            activeTenantId: null,
            activeRole: null,
            isLoading: false,
            login: vi.fn(),
            logout: vi.fn()
        });

        render(
            <ProtectedRoute>
                <div data-testid="protected-content">Secure Freight Dashboard</div>
            </ProtectedRoute>
        );

        // Verify the layout was withheld and the user was booted to the login vector
        expect(screen.queryByTestId('protected-content')).toBeNull();
        const navigateElement = screen.getByTestId('mock-navigate');
        expect(navigateElement).toBeInTheDocument();
        expect(navigateElement.getAttribute('data-to')).toBe('/login');
    });

    it('should hold rendering frame while the authentication context initializes', () => {
        // Mock the context while it pulls values out of localStorage async
        vi.mocked(useAuth).mockReturnValue({
            token: null,
            activeTenantId: null,
            activeRole: null,
            isLoading: true, // App is loading auth state
            login: vi.fn(),
            logout: vi.fn()
        });

        render(
            <ProtectedRoute>
                <div data-testid="protected-content">Secure Freight Dashboard</div>
            </ProtectedRoute>
        );

        // Ensure nothing mounts to the DOM during state resolution
        expect(screen.queryByTestId('protected-content')).toBeNull();
        expect(screen.queryByTestId('mock-navigate')).toBeNull();
    });
});