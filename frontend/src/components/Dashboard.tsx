import { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import axiosClient from '../api/axiosClient';
import { RoleGuard } from './RoleGuard';
import { ShipmentForm } from './ShipmentForm';
import type { Shipment } from '../types';

export const Dashboard = () => {
    const { activeTenantId, activeRole, login, logout, isLoading } = useAuth();
    const [shipments, setShipments] = useState<Shipment[]>([]);
    const [apiError, setApiError] = useState<string | null>(null);


    useEffect(() => {
        if (activeTenantId) {
            fetchShipments();
        } else {
            setShipments([]);
            setApiError(null);
        }
    }, [activeTenantId, activeRole]);

    const fetchShipments = async () => {
        setApiError(null);
        try {
            const response = await axiosClient.get<Shipment[]>('/shipments');
            setShipments(response.data);
        } catch (error: any) {
            console.error('API Error:', error);
            const errorDetail = error.response?.data?.detail || 'HTTP Error: Context Violation';
            setApiError(errorDetail);
            setShipments([]);
        }
    };

    const handleSeedShipment = async () => {
        setApiError(null);
        try {
            await axiosClient.post('/shipments/seed');
            fetchShipments(); // Refresh table state matrix
        } catch (error: any) {
            const detail = error.response?.data?.detail || 'Failed to trigger remote seed routing';
            setApiError(`Seeding Failure: ${detail}`);
        }
    };

    const handleLogin = async (tenantId: string, role: string) => {
        setApiError(null);
        try {
            await login(tenantId, role);
        } catch (error: any) {
            const detail = error.response?.data?.detail || error.message || 'Authentication failed';
            setApiError(`Login failed: ${detail}`);
        }
    };


    const handleDeleteShipment = async (id: number) => {
        setApiError(null);
        try {
            await axiosClient.delete(`/shipments/${id}`);
            fetchShipments();
        } catch (error: any) {
            console.error('Deletion Blocked:', error);
            const detail = error.response?.data?.detail || 'HTTP 403 Forbidden: Insufficient Authority';
            setApiError(`RBAC Violation: ${detail}`);
        }
    };

    const handleSimulateBreach = async () => {
        setApiError(null);
        try {
            const unauthenticatedResponse = await axiosClient.get('/shipments', {
                headers: { Authorization: '' }
            });
            setShipments(unauthenticatedResponse.data);
        } catch (error: any) {
            const detail = error.response?.data?.detail || 'Access Denied';
            setApiError(`Breach Blocked by Gateway: "${detail}"`);
            setShipments([]);
        }
    };

    return (
        <div className="min-h-screen flex flex-col bg-slate-950 font-sans text-slate-100">
            {/* Top Enterprise Banner Nav */}
            <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-md px-8 py-4 flex justify-between items-center sticky top-0 z-50">
                <div>
                    <h1 className="text-xl font-bold tracking-tight bg-gradient-to-r from-blue-400 to-indigo-400 bg-clip-text text-transparent">
                        CertiFreight Portal
                    </h1>
                    <p className="text-xs text-slate-400">Zero-Trust Multi-Tenant Freight Management</p>
                </div>
                <div className="flex items-center gap-3">
                    <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
                    <span className="text-xs font-mono text-slate-300 bg-slate-800 px-2.5 py-1 rounded-md border border-slate-700">
                        {activeTenantId ? `CONTEXT // ${activeTenantId.toUpperCase()} // ${activeRole}` : 'NO_AUTHENTICATED_CONTEXT'}
                    </span>
                </div>
            </header>

            {/* Main Asymmetric Grid Split */}
            <div className="flex-1 grid grid-cols-1 md:grid-cols-4 gap-6 p-6 max-w-7xl w-full mx-auto">

                {/* Left Control Sidebar */}
                <aside className="space-y-6 md:col-span-1">
                    <div className="bg-slate-900 rounded-xl border border-slate-800 p-5 space-y-4">
                        <h2 className="text-sm font-semibold tracking-wider text-slate-400 uppercase">
                            Identity Profile Matrix
                        </h2>

                        <div className="space-y-4">
                            <div>
                                <label className="block text-xs font-semibold text-slate-500 mb-1.5 uppercase tracking-wide">Tenant: Alpha</label>
                                <div className="flex flex-col gap-1.5">
                                    <button
                                        disabled={isLoading}
                                        onClick={() => void handleLogin('enterprise-alpha', 'ROLE_DISPATCHER')}
                                        className={`w-full py-2 px-3 text-xs font-medium rounded-lg border text-left transition-all ${
                                            activeTenantId === 'enterprise-alpha' && activeRole === 'ROLE_DISPATCHER'
                                                ? 'bg-blue-600/10 border-blue-500 text-blue-400 font-semibold'
                                                : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                                        }`}
                                    >
                                        Alpha: Dispatcher
                                    </button>
                                    <button
                                        disabled={isLoading}
                                        onClick={() => void handleLogin('enterprise-alpha', 'ROLE_ADMIN')}
                                        className={`w-full py-2 px-3 text-xs font-medium rounded-lg border text-left transition-all ${
                                            activeTenantId === 'enterprise-alpha' && activeRole === 'ROLE_ADMIN'
                                                ? 'bg-purple-600/10 border-purple-500 text-purple-400 font-semibold'
                                                : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                                        }`}
                                    >
                                        Alpha: Manager Admin
                                    </button>
                                </div>
                            </div>

                            <div className="border-t border-slate-800/60 pt-3">
                                <label className="block text-xs font-semibold text-slate-500 mb-1.5 uppercase tracking-wide">Tenant: Beta</label>
                                <div className="flex flex-col gap-1.5">
                                    <button
                                        disabled={isLoading}
                                        onClick={() => void handleLogin('enterprise-beta', 'ROLE_DISPATCHER')}
                                        className={`w-full py-2 px-3 text-xs font-medium rounded-lg border text-left transition-all ${
                                            activeTenantId === 'enterprise-beta' && activeRole === 'ROLE_DISPATCHER'
                                                ? 'bg-blue-600/10 border-blue-500 text-blue-400 font-semibold'
                                                : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                                        }`}
                                    >
                                        Beta: Dispatcher
                                    </button>
                                    <button
                                        disabled={isLoading}
                                        onClick={() => void handleLogin('enterprise-beta', 'ROLE_ADMIN')}
                                        className={`w-full py-2 px-3 text-xs font-medium rounded-lg border text-left transition-all ${
                                            activeTenantId === 'enterprise-beta' && activeRole === 'ROLE_ADMIN'
                                                ? 'bg-purple-600/10 border-purple-500 text-purple-400 font-semibold'
                                                : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                                        }`}
                                    >
                                        Beta: Manager Admin
                                    </button>
                                </div>
                            </div>

                            {activeTenantId && (
                                <button
                                    onClick={logout}
                                    className="w-full mt-2 py-2 px-4 text-xs font-medium bg-slate-950 hover:bg-rose-950/20 text-rose-400 rounded-lg border border-slate-800 hover:border-rose-900/50 transition-colors duration-150 text-center"
                                >
                                    Terminate Active Session
                                </button>
                            )}
                        </div>
                    </div>

                    {/* Freight Assignment Form Component */}
                    {activeTenantId && (
                        <RoleGuard allowedRoles={['ROLE_ADMIN', 'ROLE_DISPATCHER']}>
                            <ShipmentForm onSuccess={fetchShipments} />
                        </RoleGuard>
                    )}
                </aside>

                {/* Right Metrics Ledger Workspace */}
                <main className="md:col-span-3 space-y-6">
                    {/* Error & Security Alert Logging Box */}
                    {apiError && (
                        <div className="bg-rose-950/20 border border-rose-900/50 rounded-xl p-4 flex gap-3 text-sm text-rose-300 shadow-lg shadow-rose-950/10">
                            <div className="font-semibold select-none">SYSTEM_ALERT:</div>
                            <div className="font-mono">{apiError}</div>
                        </div>
                    )}

                    {activeTenantId ? (
                        <div className="bg-slate-900 rounded-xl border border-slate-800 overflow-hidden shadow-xl">
                            {/* Ledger Table Controls Panel */}
                            <div className="p-5 border-b border-slate-800 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-slate-900/50">
                                <div>
                                    <h2 className="text-lg font-bold">Isolated Freight Ledger</h2>
                                    <p className="text-xs text-slate-400">Database queries natively scoped via Hibernate @TenantId boundaries</p>
                                </div>
                                {/* RESTORED: Symmetrical Control Button Actions Array */}
                                <div className="flex gap-2 w-full sm:w-auto">
                                    <button
                                        onClick={handleSeedShipment}
                                        className="flex-1 sm:flex-none py-2 px-3.5 text-xs font-bold bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg shadow-md shadow-emerald-950/20 transition-all duration-150"
                                    >
                                        + Seed Test Cargo
                                    </button>
                                    <button
                                        onClick={handleSimulateBreach}
                                        className="flex-1 sm:flex-none py-2 px-3.5 text-xs font-semibold bg-amber-600/10 hover:bg-amber-600/20 border border-amber-500/30 hover:border-amber-500/50 text-amber-400 rounded-lg transition-all duration-150"
                                    >
                                        Simulate System Breach Test
                                    </button>
                                </div>
                            </div>

                            {/* Data Rendering Matrix */}
                            <div className="overflow-x-auto">
                                {shipments.length === 0 ? (
                                    <div className="p-8 text-center text-sm text-slate-500 italic">
                                        No active manifest payloads indexed under this organizational scope footprint.
                                    </div>
                                ) : (
                                    <table className="w-full text-left border-collapse">
                                        <thead>
                                        <tr className="bg-slate-950/50 text-slate-400 uppercase text-xs font-semibold tracking-wider border-b border-slate-800">
                                            <th className="p-4">Cargo ID</th>
                                            <th className="p-4">Tracking Reference</th>
                                            <th className="p-4">Cargo Weight</th>
                                            <th className="p-4">Logistics Status</th>
                                            <th className="p-4 text-right">Actions Matrix</th>
                                        </tr>
                                        </thead>
                                        <tbody className="divide-y divide-slate-800/60 text-sm">
                                        {shipments.map((shipment) => (
                                            <tr key={shipment.id} className="hover:bg-slate-800/30 transition-colors duration-100">
                                                <td className="p-4 font-mono font-bold text-slate-400">#{shipment.id}</td>
                                                <td className="p-4"><code className="text-blue-400 font-mono text-xs">{shipment.trackingNumber}</code></td>
                                                <td className="p-4 font-mono text-slate-300">
                                                    {shipment.weightLbs ? `${Number(shipment.weightLbs).toLocaleString()} lbs` : '-'}
                                                </td>
                                                <td className="p-4">
                                                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-500/10 border border-blue-500/20 text-blue-400">
                                                            {shipment.status}
                                                        </span>
                                                </td>
                                                <td className="p-4 text-right">
                                                    <RoleGuard
                                                        allowedRoles={['ROLE_ADMIN']}
                                                        fallback={
                                                            <span className="text-xs text-slate-600 font-mono italic select-none">
                                                                READ_ONLY // LOCKED
                                                            </span>
                                                        }>
                                                        <button
                                                            onClick={() => handleDeleteShipment(shipment.id)}
                                                            className="text-xs font-semibold py-1 px-2.5 bg-rose-950/30 hover:bg-rose-600 border border-rose-900/50 hover:border-rose-500 text-rose-400 hover:text-white rounded-md transition-all duration-150"
                                                        >
                                                            Purge Record
                                                        </button>
                                                    </RoleGuard>
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        </div>
                    ) : (
                        <div className="h-64 flex flex-col items-center justify-center border border-dashed border-slate-800 rounded-xl text-center p-6 bg-slate-900/20">
                            <div className="text-slate-400 text-sm font-semibold mb-1">Gateway Authentication Required</div>
                            <p className="text-xs text-slate-500 max-w-xs">Select an active corporate tenant identity core from the sidebar matrix to initialize the cryptographic pipeline and retrieve records.</p>
                        </div>
                    )}
                </main>
            </div>
        </div>
    );
};

export default Dashboard;
