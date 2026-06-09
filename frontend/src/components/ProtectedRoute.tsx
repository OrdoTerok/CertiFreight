// @ts-ignore
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

interface ProtectedRouteProps {
    allowedRoles?: string[];
    redirectTo?: string;
}

export const ProtectedRoute = ({ allowedRoles, redirectTo = "/" }: ProtectedRouteProps) => {
    const { token, activeRole } = useAuth();

    if (!token) {
        return <Navigate to={redirectTo} replace />;
    }

    if (allowedRoles && (!activeRole || !allowedRoles.includes(activeRole))) {
        return <Navigate to="/unauthorized" replace />;
    }

    return <Outlet />;
};