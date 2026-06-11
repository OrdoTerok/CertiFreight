import { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import axiosClient from '../api/axiosClient';
import { RoleGuard } from './RoleGuard';
import type { Shipment } from '../types';
import {ShipmentForm} from "./ShipmentForm";

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

    const handleGenerateTracking = () => {
        const randomHex = Math.random().toString(36).substring(2, 8).toUpperCase();
        setTrackingNumber(`CFT-${randomHex}`);
    };

    const handleCreateShipment = async (e: React.FormEvent) => {
        e.preventDefault();
        setApiError(null);
        setIsSubmitting(true);

        try {
            await axiosClient.post('/shipments', {
                trackingNumber,
                weightLbs: weightLbs === '' ? null : Number(weightLbs)
            });
            setTrackingNumber('');
            setWeightLbs('');
            fetchShipments();
        } catch (error: any) {
            const detail = error.response?.data?.detail || 'Server side parsing error';
            setApiError(detail);
        } finally {
            setIsSubmitting(false);
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
            {/* Nav Header stays exactly identical */}

            <div className="flex-1 grid grid-cols-1 md:grid-cols-4 gap-6 p-6 max-w-7xl w-full mx-auto">
                <aside className="space-y-6 md:col-span-1">
                    {/* Identity Profile Matrix Buttons Block */}

                    {/* Cleaned Freight Assignment Component Integration */}
                    {activeTenantId && (
                        <RoleGuard allowedRoles={['ROLE_ADMIN', 'ROLE_DISPATCHER']}>
                            {/* Pass fetchShipments as the onSuccess callback.
                              When the child completes its post, this updates the parent table automatically.
                            */}
                            <ShipmentForm onSuccess={fetchShipments} />
                        </RoleGuard>
                    )}
                </aside>

                {/* Right Metrics Ledger Workspace Table remains exactly identical */}
            </div>
        </div>
    );
};