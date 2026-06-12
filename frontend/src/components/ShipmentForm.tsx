import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import axiosClient from '../api/axiosClient';

interface ShipmentFormProps {
    onSuccess?: () => void;
}

export const ShipmentForm: React.FC<ShipmentFormProps> = ({ onSuccess }) => {
    const { activeTenantId } = useAuth();
    const [trackingNumber, setTrackingNumber] = useState('');
    const [weightLbs, setWeightLbs] = useState<number | ''>('');
    const [statusMessage, setStatusMessage] = useState<string | null>(null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleGenerateTracking = () => {
        const sixDigits = String(Math.floor(Math.random() * 1_000_000)).padStart(6, '0');
        setTrackingNumber(`CFT-${sixDigits}`);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSubmitting(true);
        setErrorMessage(null);
        setStatusMessage(null);

        try {
            // Leveraging your configured axiosClient instead of raw fetch
            await axiosClient.post('/shipments', {
                trackingNumber,
                weightLbs: weightLbs === '' ? null : Number(weightLbs),
            }, {
                headers: {
                    // Explicit fallback safety if your global interceptor isn't catching it yet
                    'X-Tenant-ID': activeTenantId || ''
                }
            });

            setStatusMessage('Manifest successfully created.');
            setTrackingNumber('');
            setWeightLbs('');

            // Fire the parent notification callback if provided
            if (onSuccess) {
                onSuccess();
            }
        } catch (error: any) {
            const detail = error.response?.data?.detail || 'Server side parsing error';
            setErrorMessage(detail);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="bg-slate-900 rounded-xl border border-slate-800 p-5 space-y-4 text-slate-100">
            <h2 className="text-sm font-semibold tracking-wider text-slate-400 uppercase">
                Assign Isolated Freight
            </h2>

            {statusMessage && (
                <div className="p-3 bg-emerald-950/40 border border-emerald-500/30 text-emerald-400 rounded-lg text-xs font-mono">
                    {statusMessage}
                </div>
            )}

            {errorMessage && (
                <div className="p-3 bg-rose-950/40 border border-rose-500/30 text-rose-400 rounded-lg text-xs font-mono">
                    {errorMessage}
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-3.5">
                <div>
                    <label htmlFor="trackingNumber" className="block text-xs font-medium text-slate-400 mb-1">
                        Tracking Number
                    </label>
                    <div className="flex gap-2">
                        <input
                            id="trackingNumber"
                            type="text"
                            required
                            value={trackingNumber}
                            onChange={(e) => setTrackingNumber(e.target.value)}
                            placeholder="CFT-123456"
                            className="flex-1 bg-slate-950 border border-slate-800 focus:border-blue-500 rounded-lg px-3 py-1.5 text-sm font-mono text-blue-400 focus:outline-none"
                        />
                        <button
                            type="button"
                            onClick={handleGenerateTracking}
                            className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 rounded-lg text-xs font-semibold border border-slate-700 transition-colors"
                        >
                            ⚡
                        </button>
                    </div>
                </div>

                <div>
                    <label htmlFor="weightLbs" className="block text-xs font-medium text-slate-400 mb-1">
                        Cargo Weight (lbs)
                    </label>
                    <input
                        id="weightLbs"
                        type="number"
                        required
                        min="1"
                        value={weightLbs}
                        onChange={(e) => setWeightLbs(e.target.value === '' ? '' : Number(e.target.value))}
                        placeholder="e.g. 2500"
                        className="w-full bg-slate-950 border border-slate-800 focus:border-blue-500 rounded-lg px-3 py-1.5 text-sm focus:outline-none text-white"
                    />
                </div>

                <button
                    type="submit"
                    disabled={isSubmitting}
                    className="w-full py-2 px-4 mt-2 text-xs font-bold bg-blue-600 hover:bg-blue-500 disabled:bg-blue-800 text-white rounded-lg transition-all shadow-md shadow-blue-950/20"
                >
                    {isSubmitting ? 'Registering Manifest...' : 'Commit Freight Link'}
                </button>
            </form>
        </div>
    );
};