import React, { createContext, useContext, useState, useEffect } from 'react';

interface TenantContextType {
    currentTenant: string;
    setTenant: (tenantId: string) => void;
    isLoading: boolean;
}

const TenantContext = createContext<TenantContextType | undefined>(undefined);

export const TenantProvider: React.FC<{ children: React.ReactNode; initialTenant?: string }> = ({
                                                                                                    children,
                                                                                                    initialTenant
                                                                                                }) => {
    const [currentTenant, setCurrentTenant] = useState<string>(initialTenant || 'anonymous_tenant');
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        // If an explicit testing tenant was passed as a prop, bypass discovery
        if (initialTenant) {
            setCurrentTenant(initialTenant);
            setIsLoading(false);
            return;
        }

        // Production/Dev Discovery Logic Flow:
        // 1. Check URL query string (?tenant=alpha)
        // 2. Check hostname subdomain (alpha.certifreight.com)
        // 3. Fall back to localStorage or anonymous
        const urlParams = new URLSearchParams(window.location.search);
        const queryTenant = urlParams.get('tenant');

        const host = window.location.hostname; // e.g., "alpha.certifreight.local"
        const parts = host.split('.');
        const subdomainTenant = parts.length > 2 && parts[0] !== 'www' ? parts[0] : null;

        const savedTenant = localStorage.getItem('cf_tenant');

        const resolvedTenant = queryTenant || subdomainTenant || savedTenant || 'anonymous_tenant';

        setCurrentTenant(resolvedTenant);
        localStorage.setItem('cf_tenant', resolvedTenant);
        setIsLoading(false);
    }, [initialTenant]);

    const setTenant = (tenantId: string) => {
        setCurrentTenant(tenantId);
        localStorage.setItem('cf_tenant', tenantId);
    };

    return (
        <TenantContext.Provider value={{ currentTenant, setTenant, isLoading }}>
            {children}
        </TenantContext.Provider>
    );
};

// Custom hook for components to easily consume the active tenant state
export const useTenant = (): TenantContextType => {
    const context = useContext(TenantContext);
    if (context === undefined) {
        throw new Error('useTenant must be used within a TenantProvider block');
    }
    return context;
};