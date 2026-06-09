import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { AuthProvider } from './AuthContext';
import { useAuth } from '../hooks/useAuth';
import axiosClient from '../api/axiosClient';
import React from 'react';

// 1. Mock the Axios network client to completely isolate the unit test framework
vi.mock('../api/axiosClient', () => ({
    default: {
        post: vi.fn(),
    },
}));

describe('AuthContext Component Hook Unit Matrix', () => {
    beforeEach(() => {
        localStorage.clear();
        vi.clearAllMocks();
    });

    const wrapper = ({ children }: { children: React.ReactNode }) => (
        <AuthProvider>{children}</AuthProvider>
    );

    it('should initialize with default empty/null state vectors', () => {
        const { result } = renderHook(() => useAuth(), { wrapper });

        expect(result.current.token).toBeNull();
        expect(result.current.activeTenantId).toBeNull();
        expect(result.current.activeRole).toBeNull();
        expect(result.current.isLoading).toBe(false);
    });

    // FIXED: Added async/await and mocked the network resolution payload
    it('should correctly commit auth tokens and state signatures during an active login vector', async () => {
        const mockToken = 'mock-jwt-token';

        // Intercept the Axios post call and return a clean mock token response
        vi.mocked(axiosClient.post).mockResolvedValueOnce({
            data: { accessToken: mockToken }
        });

        const { result } = renderHook(() => useAuth(), { wrapper });

        // Execute and await the asynchronous state changes inside act()
        await act(async () => {
            await result.current.login('enterprise-alpha', 'ROLE_DISPATCHER');
        });

        // Assert that the context successfully unpacked the mock payload into state
        expect(result.current.token).toBe(mockToken);
        expect(result.current.activeTenantId).toBe('enterprise-alpha');
        expect(result.current.activeRole).toBe('ROLE_DISPATCHER');
        expect(localStorage.getItem('certifreight_token')).toBe(mockToken);
    });

    it('should update local tracking states clean during active logout cycles', () => {
        localStorage.setItem('certifreight_token', 'stale-token');
        localStorage.setItem('certifreight_tenant_id', 'enterprise-alpha');
        localStorage.setItem('certifreight_role', 'ROLE_DISPATCHER');

        const { result } = renderHook(() => useAuth(), { wrapper });

        act(() => {
            result.current.logout();
        });

        expect(result.current.token).toBeNull();
        expect(result.current.activeTenantId).toBeNull();
        expect(result.current.activeRole).toBeNull();
        expect(localStorage.getItem('certifreight_token')).toBeNull();
    });

    // FIXED: Aligned assertion string to match your exact production guard message
    it('should throw a strict boundary error when consumer hook is initialized outside of an AuthProvider context', () => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        expect(() => renderHook(() => useAuth())).toThrow(
            'useAuth must be executed within an active AuthProvider scope'
        );

        consoleSpy.mockRestore();
    });

    it('should handle login execution rejections gracefully and flip the loading vectors back to false', async () => {
        // Force the mock network client to throw an explicit authentication rejection
        vi.mocked(axiosClient.post).mockRejectedValueOnce(new Error('ERR_NETWORK'));

        const { result } = renderHook(() => useAuth(), { wrapper });

        await act(async () => {
            // Execute login with parameters that will trigger the rejection path
            await expect(
                result.current.login('bad-tenant', 'ROLE_DISPATCHER')
            ).rejects.toThrow();
        });

        // Verify that even on failure, the system turns off the loading state cleanly
        expect(result.current.token).toBeNull();
        expect(result.current.isLoading).toBe(false);
    });
});