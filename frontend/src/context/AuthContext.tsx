import { useState, type ReactNode } from 'react';
import axiosClient from '../api/axiosClient';
import type { AuthResponse } from '../types';
import { AuthContext } from './authContext.shared';

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [token, setToken] = useState<string | null>(localStorage.getItem('certifreight_token'));
    const [activeTenantId, setActiveTenantId] = useState<string | null>(localStorage.getItem('certifreight_tenant_id'));
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [activeRole, setActiveRole] = useState<string | null>(localStorage.getItem('certifreight_role'));

    const login = async (tenantId: string, role: string) => {
        setIsLoading(true);
        try {
            const response = await axiosClient.post<AuthResponse>(`/auth/login?tenantId=${tenantId}&role=${role}`);
            const { accessToken } = response.data;

            localStorage.setItem('certifreight_token', accessToken);
            localStorage.setItem('certifreight_tenant_id', tenantId);
            localStorage.setItem('certifreight_role', role);

            setToken(accessToken);
            setActiveTenantId(tenantId);
            setActiveRole(role);
        } catch (error) {
            console.error('Authentication gatekeeper rejected login:', error);
            throw error;
        } finally {
            setIsLoading(false);
        }
    };

    const logout = () => {
        localStorage.removeItem('certifreight_token');
        localStorage.removeItem('certifreight_tenant_id');
        localStorage.removeItem('certifreight_role');
        setToken(null);
        setActiveTenantId(null);
        setActiveRole(null);
    };

    return (
        <AuthContext.Provider value={{ token, activeTenantId, activeRole, login, logout, isLoading }}>
            {children}
        </AuthContext.Provider>
    );
};