import { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import axiosClient from '../api/axiosClient';
import type {Shipment} from '../types';

export const Dashboard = () => {
    const { activeTenantId, login, logout, isLoading } = useAuth();
    const [shipments, setShipments] = useState<Shipment[]>([]);
    const [apiError, setApiError] = useState<string | null>(null);

    // Automatically synchronize shipment panels whenever the logged-in tenant changes
    useEffect(() => {
        if (activeTenantId) {
            fetchShipments();
        } else {
            setShipments([]);
            setApiError(null);
        }
    }, [activeTenantId]);

    const fetchShipments = async () => {
        setApiError(null);
        try {
            const response = await axiosClient.get<Shipment[]>('/shipments');
            setShipments(response.data);
        } catch (error: any) {
            console.error('API Error:', error);
            // Catch and display our structured backend RFC 7807 error details if available
            const errorDetail = error.response?.data?.detail || 'HTTP Error: Context Violation';
            setApiError(errorDetail);
            setShipments([]);
        }
    };

    const handleSeedShipment = async () => {
        try {
            await axiosClient.post('/shipments/seed');
            fetchShipments(); // Refresh the list
        } catch (error: any) {
            setApiError(error.response?.data?.detail || 'Failed to seed record');
        }
    };

    // Explicitly bypasses our Axios interceptor token to force a real backend security breach test
    const handleSimulateBreach = async () => {
        setApiError(null);
        try {
            const unauthenticatedResponse = await axiosClient.get('/shipments', {
                headers: { Authorization: '' } // Wipe the token header completely
            });
            setShipments(unauthenticatedResponse.data);
        } catch (error: any) {
            const detail = error.response?.data?.detail || 'Access Denied';
            setApiError(`Breach Successfully Blocked by Backend! Error: "${detail}"`);
            setShipments([]);
        }
    };

    return (
        <div style={{ padding: '2rem', fontFamily: 'sans-serif', maxWidth: '900px', margin: '0 auto' }}>
            <header style={{ borderBottom: '2px solid #eaeaea', paddingBottom: '1rem', marginBottom: '2rem' }}>
                <h1>CertiFreight Logistics Management Portal</h1>
                <p>Status: <strong>{activeTenantId ? `Authenticated as [${activeTenantId}]` : 'Unauthenticated'}</strong></p>
            </header>

            {/* Authentication Matrix Controls */}
            <section style={{ marginBottom: '2rem', background: '#f9f9f9', padding: '1.5rem', borderRadius: '8px' }}>
                <h3>Select Active Identity Profile</h3>
                <div style={{ display: 'flex', gap: '1rem' }}>
                    <button disabled={isLoading} onClick={() => login('enterprise-alpha')} style={{ padding: '0.5rem 1rem', cursor: 'pointer' }}>
                        Login: Tenant Alpha
                    </button>
                    <button disabled={isLoading} onClick={() => login('enterprise-beta')} style={{ padding: '0.5rem 1rem', cursor: 'pointer' }}>
                        Login: Tenant Beta
                    </button>
                    <button onClick={logout} style={{ padding: '0.5rem 1rem', cursor: 'pointer', background: '#ffebeb', border: '1px solid #ffcccc' }}>
                        Clear Session
                    </button>
                </div>
            </section>

            {/* Error & Security Alert Logging Box */}
            {apiError && (
                <div style={{ background: '#fff5f5', color: '#cc0000', padding: '1rem', borderRadius: '6px', border: '1px solid #ffcccc', marginBottom: '2rem' }}>
                    <strong>Security Event Alert:</strong> {apiError}
                </div>
            )}

            {/* Main Application Ledger Views */}
            {activeTenantId && (
                <section>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                        <h2>Data Isolation Ledger Display</h2>
                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                            <button onClick={handleSeedShipment} style={{ padding: '0.5rem 1rem', cursor: 'pointer', background: '#e6f4ea', border: '1px solid #ceead6' }}>
                                + Seed Isolated Shipment
                            </button>
                            <button onClick={handleSimulateBreach} style={{ padding: '0.5rem 1rem', cursor: 'pointer', background: '#feefe3', border: '1px solid #fde2cd', color: '#b06000' }}>
                                Simulate Security Breach
                            </button>
                        </div>
                    </div>

                    {shipments.length === 0 ? (
                        <p style={{ color: '#666', fontStyle: 'italic' }}>No shipments found matching this organizational domain visibility matrix.</p>
                    ) : (
                        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                            <thead>
                            <tr style={{ background: '#f4f4f4' }}>
                                <th style={{ padding: '0.75rem', borderBottom: '1px solid #ddd' }}>ID</th>
                                <th style={{ padding: '0.75rem', borderBottom: '1px solid #ddd' }}>Tracking Number</th>
                                <th style={{ padding: '0.75rem', borderBottom: '1px solid #ddd' }}>Status</th>
                                <th style={{ padding: '0.75rem', borderBottom: '1px solid #ddd' }}>Tenant Scope Owner</th>
                            </tr>
                            </thead>
                            <tbody>
                            {shipments.map((shipment) => (
                                <tr key={shipment.id}>
                                    <td style={{ padding: '0.75rem', borderBottom: '1px solid #ddd' }}>{shipment.id}</td>
                                    <td style={{ padding: '0.75rem', borderBottom: '1px solid #ddd' }}><code>{shipment.trackingNumber}</code></td>
                                    <td style={{ padding: '0.75rem', borderBottom: '1px solid #ddd' }}>
                                            <span style={{ background: '#e8f0fe', color: '#1a73e8', padding: '0.25rem 0.5rem', borderRadius: '4px', fontSize: '0.85rem' }}>
                                                {shipment.status}
                                            </span>
                                    </td>
                                    <td style={{ padding: '0.75rem', borderBottom: '1px solid #ddd', color: '#666' }}>{shipment.tenantId}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    )}
                </section>
            )}
        </div>
    );
};