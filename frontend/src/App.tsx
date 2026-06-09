import { AuthProvider } from './context/AuthContext';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Dashboard } from './components/Dashboard';
import { AdminConsole } from './components/AdminConsole';
import { ProtectedRoute } from './components/ProtectedRoute';

export const App = () => {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    {/* Public / Entry Gateway Path */}
                    <Route path="/" element={<Dashboard />} />

                    {/* Secure Structural Routing Boundary */}
                    <Route element={<ProtectedRoute allowedRoles={['ROLE_ADMIN']} />}>
                        <Route path="/admin/settings" element={<AdminConsole />} />
                    </Route>

                    {/* Fallback Unauthorized Matrix Notice */}
                    <Route path="/unauthorized" element={<div className="text-white p-6">403: Explicit Context Denied</div>} />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
};

export default App;