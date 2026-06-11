import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { AuthContext } from '../context/AuthContext';

const renderRoute = (authValue: {
    token: string | null;
    activeTenantId: string | null;
    activeRole: string | null;
    isLoading: boolean;
}) => {
    render(
        <AuthContext.Provider
            value={{
                ...authValue,
                login: vi.fn(),
                logout: vi.fn(),
            }}
        >
            <MemoryRouter initialEntries={['/admin/settings']}>
                <Routes>
                    <Route path="/" element={<div data-testid="home-page">Home</div>} />
                    <Route path="/unauthorized" element={<div data-testid="unauthorized-page">Unauthorized</div>} />
                    <Route element={<ProtectedRoute allowedRoles={['ROLE_ADMIN']} />}>
                        <Route path="/admin/settings" element={<div data-testid="protected-content">Admin Console</div>} />
                    </Route>
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>
    );
};

describe('ProtectedRoute', () => {
    it('renders loading state while auth context initializes', () => {
        renderRoute({
            token: null,
            activeTenantId: null,
            activeRole: null,
            isLoading: true,
        });

        expect(screen.getByText('Loading security context...')).toBeInTheDocument();
    });

    it('redirects to the home route when no token exists', () => {
        renderRoute({
            token: null,
            activeTenantId: 'enterprise-alpha',
            activeRole: 'ROLE_ADMIN',
            isLoading: false,
        });

        expect(screen.getByTestId('home-page')).toBeInTheDocument();
        expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    });

    it('redirects to unauthorized when role is not allowed', () => {
        renderRoute({
            token: 'valid-jwt-token',
            activeTenantId: 'enterprise-alpha',
            activeRole: 'ROLE_DISPATCHER',
            isLoading: false,
        });

        expect(screen.getByTestId('unauthorized-page')).toBeInTheDocument();
        expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    });

    it('renders protected outlet when token and role are valid', () => {
        renderRoute({
            token: 'valid-jwt-token',
            activeTenantId: 'enterprise-alpha',
            activeRole: 'ROLE_ADMIN',
            isLoading: false,
        });

        expect(screen.getByTestId('protected-content')).toBeInTheDocument();
        expect(screen.queryByTestId('home-page')).not.toBeInTheDocument();
    });
});