import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Dashboard } from './Dashboard';
import { useAuth } from '../hooks/useAuth';
import axiosClient from '../api/axiosClient';

vi.mock('../hooks/useAuth', () => ({
    useAuth: vi.fn()
}));

vi.mock('../api/axiosClient', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
        delete: vi.fn(),
    }
}));

vi.mock('./ShipmentForm', () => ({
    ShipmentForm: ({ onSuccess }: { onSuccess?: () => void }) => (
        <div>
            <div data-testid="shipment-form">Shipment Form</div>
            <button type="button" onClick={onSuccess}>Trigger refresh</button>
        </div>
    ),
}));

describe('Dashboard Feature View Component Matrix', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        vi.mocked(useAuth).mockReturnValue({
            token: 'valid-jwt-token',
            activeTenantId: 'enterprise-alpha',
            activeRole: 'ROLE_DISPATCHER',
            isLoading: false,
            login: vi.fn(),
            logout: vi.fn()
        });

        vi.mocked(axiosClient.get).mockResolvedValue({ data: [] });
    });

    it('renders shipment form for tenant users with permitted roles', async () => {
        render(<Dashboard />);

        await waitFor(() => {
            expect(screen.getByTestId('shipment-form')).toBeInTheDocument();
        });

        expect(axiosClient.get).toHaveBeenCalledWith('/shipments');
    });

    it('does not render shipment form and skips loading when no tenant is active', () => {
        vi.mocked(useAuth).mockReturnValue({
            token: 'valid-jwt-token',
            activeTenantId: null,
            activeRole: 'ROLE_DISPATCHER',
            isLoading: false,
            login: vi.fn(),
            logout: vi.fn(),
        });

        render(<Dashboard />);

        expect(screen.queryByTestId('shipment-form')).not.toBeInTheDocument();
        expect(axiosClient.get).not.toHaveBeenCalled();
    });

    it('refreshes shipments when ShipmentForm invokes onSuccess callback', async () => {
        const user = userEvent.setup();

        render(<Dashboard />);

        await waitFor(() => {
            expect(axiosClient.get).toHaveBeenCalledTimes(1);
        });

        await user.click(screen.getByRole('button', { name: 'Trigger refresh' }));

        await waitFor(() => {
            expect(axiosClient.get).toHaveBeenCalledTimes(2);
        });
    });

    it('simulates an unauthenticated breach request and surfaces blocked gateway message', async () => {
        const user = userEvent.setup();

        vi.mocked(axiosClient.get)
            .mockResolvedValueOnce({ data: [] })
            .mockRejectedValueOnce({
                response: {
                    data: {
                        detail: 'Unauthorized'
                    }
                }
            });

        render(<Dashboard />);

        await waitFor(() => {
            expect(axiosClient.get).toHaveBeenCalledTimes(1);
        });

        await user.click(screen.getByRole('button', { name: 'Simulate System Breach Test' }));

        await waitFor(() => {
            expect(axiosClient.get).toHaveBeenNthCalledWith(2, '/shipments', {
                headers: { Authorization: '' }
            });
        });

        expect(screen.getByText('Breach Blocked by Gateway: "Unauthorized"')).toBeInTheDocument();
    });
});