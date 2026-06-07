import { createContext, useState, type ReactNode } from 'react';
import axiosClient from '../api/axiosClient';
import type {AuthResponse} from '../types';

interface AuthContextType {
    token: string | null;
    activeTenantId: string | null;
    login: (tenantId: string) => Promise<void>;
    logout: () => void;
    isLoading: boolean;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [token, setToken] = useState<string | null>(localStorage.getItem('certifreight_token'));
    const [activeTenantId, setActiveTenantId] = useState<string | null>(localStorage.getItem('certifreight_tenant_id'));
    const [isLoading, setIsLoading] = useState<boolean>(false);

    const login = async (tenantId: string) => {
        setIsLoading(true);
        try {
            // Hit our decoupled backend authentication controller
            const response = await axiosClient.post<AuthResponse>(`/auth/login?tenantId=${tenantId}`);
            const { accessToken } = response.data;

            // Commit the cryptographic credentials to local storage and state
            localStorage.setItem('certifreight_token', accessToken);
            localStorage.setItem('certifreight_tenant_id', tenantId);

            setToken(accessToken);
            setActiveTenantId(tenantId);
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
        setToken(null);
        setActiveTenantId(null);
    };

    return (
        <AuthContext.Provider value={{ token, activeTenantId, login, logout, isLoading }}>
            {children}
        </AuthContext.Provider>
    );
};