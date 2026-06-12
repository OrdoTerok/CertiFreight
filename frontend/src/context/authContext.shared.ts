import { createContext } from 'react';

export interface AuthContextType {
    token: string | null;
    activeTenantId: string | null;
    activeRole: string | null;
    login: (tenantId: string, role: string) => Promise<void>;
    logout: () => void;
    isLoading: boolean;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

