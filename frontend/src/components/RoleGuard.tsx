import React from 'react';
import { useAuth } from '../hooks/useAuth';

interface RoleGuardProps {
    allowedRoles: string[];
    children: React.ReactNode;
    fallback?: React.ReactNode;
}

export const RoleGuard = ({ allowedRoles, children, fallback = null }: RoleGuardProps) => {
    const { activeRole, activeTenantId } = useAuth();

    if (!activeTenantId || !activeRole || !allowedRoles.includes(activeRole)) {
        return <>{fallback}</>;
    }

    return <>{children}</>;
};