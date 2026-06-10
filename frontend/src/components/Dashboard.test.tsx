import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { Dashboard } from './Dashboard';
import { useAuth } from '../hooks/useAuth';
import axiosClient from '../api/axiosClient';
import React from 'react';

vi.mock('../hooks/useAuth', () => ({
    useAuth: vi.fn()
}));

vi.mock('../api/axiosClient', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn()
    }
}));

describe('Dashboard Feature View Component Matrix', () => {
    const mockShipments = [
        { id: 1, trackingNumber: 'CFT-11111', weightLbs: 2500, status: 'MANIFEST_CREATED' },
        { id: 2, trackingNumber: 'CFT-22222', weightLbs: 4800, status: 'PROCESSING' }
    ];

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

        vi.mocked(axiosClient.get).mockResolvedValue({ data: mockShipments });
    });

    it('should fetch and render active shipment data rows cleanly upon mounting', async () => {
        render(<Dashboard />);

        await waitFor(() => {
            expect(screen.getByText('CFT-11111')).toBeInTheDocument();
            expect(screen.getByText('CFT-22222')).toBeInTheDocument();
        });

        expect(screen.getByText('MANIFEST_CREATED')).toBeInTheDocument();
        expect(screen.getByText('PROCESSING')).toBeInTheDocument();
    });

    it('should update input states when typing and invoke the API call on a valid form submission', async () => {
        vi.mocked(axiosClient.post).mockResolvedValue({
            data: { id: 3, trackingNumber: 'CFT-NEW99', weightLbs: 6000, status: 'MANIFEST_CREATED' }
        });

        render(<Dashboard />);

        // Wait for initial data render to clear out loading states
        await waitFor(() => {
            expect(screen.getByText('CFT-11111')).toBeInTheDocument();
        });

        // ALIGNED SELECTORS: Match the literal placeholders and labels from your JSX layout
        const trackingInput = screen.getByPlaceholderText('CFT-XXXXXX');
        const weightInput = screen.getByPlaceholderText('e.g. 2500');
        const submitButton = screen.getByRole('button', { name: /Commit Freight Link/i });

        // Execute input mutations and trigger the submit cycle inside the async act block
        await act(async () => {
            fireEvent.change(trackingInput, { target: { value: 'CFT-NEW99' } });
            fireEvent.change(weightInput, { target: { value: '6000' } });
            fireEvent.click(submitButton);
        });

        // Assert that the network layer catches the tracking number and stringified weight parameters
        await waitFor(() => {
            expect(axiosClient.post).toHaveBeenCalledWith(
                '/shipments',
                expect.objectContaining({
                    trackingNumber: 'CFT-NEW99',
                    weightLbs: 6000
                })
            );
        });
    });

    it('should render an error message state or fallback banner if the initial data fetch fails', async () => {
        // Prevent expected console logs from cluttering the terminal stream during failure paths
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
        vi.mocked(axiosClient.get).mockRejectedValueOnce(new Error('Network Error'));

        render(<Dashboard />);

        await waitFor(() => {
            expect(screen.getByText(/Failed to load shipments|error/i)).toBeInTheDocument();
        });

        consoleSpy.mockRestore();
    });
});