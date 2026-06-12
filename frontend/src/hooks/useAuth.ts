import { useContext } from 'react';
import { AuthContext } from '../context/authContext.shared';

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be executed within an active AuthProvider scope');
    }
    return context;
};