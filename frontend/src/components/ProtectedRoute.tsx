import React, { useContext } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext'; // Direct import of your exported context

interface ProtectedRouteProps {
    allowedRoles: string[];
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ allowedRoles }) => {
    const authContext = useContext(AuthContext);

    // Safety fallback if context is consumed outside of provider bounds
    if (!authContext) {
        throw new Error('ProtectedRoute must be used within an AuthProvider block.');
    }

    // Extract variables explicitly typed in your AuthContextType
    const { token, activeRole, isLoading } = authContext;

    if (isLoading) {
        return <div className="text-white p-6">Loading security context...</div>;
    }

    // 1. Authentication Check: Is there a token string present?
    if (!token) {
        return <Navigate to="/" replace />;
    }

    // 2. Authorization Check: Is the active single string role inside the allowed array?
    const hasRequiredRole = activeRole && allowedRoles.includes(activeRole);

    if (!hasRequiredRole) {
        return <Navigate to="/unauthorized" replace />;
    }

    // If both matches clear, render sub-route elements
    return <Outlet />;
};