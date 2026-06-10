import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

interface ProtectedRouteProps {
    children: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
    const { token, isLoading } = useAuth();

    // 1. If the authentication context is initializing, hold the rendering frame
    if (isLoading) {
        return null;
    }

    // 2. If no valid authorization vector exists, force intercept and redirect
    if (!token) {
        return <Navigate to="/login" replace />;
    }

    // 3. Identity verified safely, render the enclosed component stream cleanly
    return <>{children}</>;
};