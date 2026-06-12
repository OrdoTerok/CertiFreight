import { AuthProvider } from './context/AuthContext';
// 1. Import the missing multi-tenant context engine
import { TenantProvider } from './context/TenantContext';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Dashboard } from './components/Dashboard';
import { AdminConsole } from './components/AdminConsole';
import { ProtectedRoute } from './components/ProtectedRoute';

export const App = () => {
    return (
        // 2. Wrap the tree as the outermost boundary to drive sub-contexts
        <TenantProvider>
            <AuthProvider>
                <BrowserRouter>
                    <Routes>
                        {/* Public / Entry Gateway Path */}
                        <Route path="/" element={<Dashboard />} />

                        {/* Secure Structural Routing Boundary */}
                        <Route element={<ProtectedRoute allowedRoles={['ROLE_ADMIN']} />}>
                            <Route path="/admin/settings" element={<AdminConsole />} />
                        </Route>

                        {/* Fallback Unauthorized Notice */}
                        <Route path="/unauthorized" element={<div className="text-white p-6">403: Explicit Context Denied</div>} />
                    </Routes>
                </BrowserRouter>
            </AuthProvider>
        </TenantProvider>
    );
};

export default App;