import { beforeEach, describe, expect, it, vi } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import React from 'react';
import { TenantProvider, useTenant } from './TenantContext';

describe('TenantContext', () => {
    beforeEach(() => {
        localStorage.clear();
        window.history.replaceState({}, '', '/');
        vi.restoreAllMocks();
    });

    it('uses initialTenant when explicitly provided', async () => {
        const wrapper = ({ children }: { children: React.ReactNode }) => (
            <TenantProvider initialTenant="enterprise-alpha">{children}</TenantProvider>
        );

        const { result } = renderHook(() => useTenant(), { wrapper });

        await waitFor(() => {
            expect(result.current.isLoading).toBe(false);
        });

        expect(result.current.currentTenant).toBe('enterprise-alpha');
    });

    it('resolves tenant from query string and persists it', async () => {
        window.history.replaceState({}, '', '/?tenant=alpha');

        const wrapper = ({ children }: { children: React.ReactNode }) => (
            <TenantProvider>{children}</TenantProvider>
        );

        const { result } = renderHook(() => useTenant(), { wrapper });

        await waitFor(() => {
            expect(result.current.isLoading).toBe(false);
        });

        expect(result.current.currentTenant).toBe('alpha');
        expect(localStorage.getItem('cf_tenant')).toBe('alpha');
    });

    it('updates both state and localStorage through setTenant', async () => {
        const wrapper = ({ children }: { children: React.ReactNode }) => (
            <TenantProvider initialTenant="anonymous_tenant">{children}</TenantProvider>
        );

        const { result } = renderHook(() => useTenant(), { wrapper });

        await waitFor(() => {
            expect(result.current.isLoading).toBe(false);
        });

        act(() => {
            result.current.setTenant('beta');
        });

        expect(result.current.currentTenant).toBe('beta');
        expect(localStorage.getItem('cf_tenant')).toBe('beta');
    });

    it('throws when useTenant is used outside TenantProvider', () => {
        expect(() => renderHook(() => useTenant())).toThrow(
            'useTenant must be used within a TenantProvider block'
        );
    });
});

