import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, test, expect, afterEach } from 'vitest';
import { ShipmentForm } from './ShipmentForm';
import { AuthProvider } from '../context/AuthContext'; // Point to your real AuthProvider
import { server } from '../test/mocks/server';
import { http, HttpResponse } from 'msw';

describe('ShipmentForm Component Integration Pipeline', () => {

    afterEach(() => {
        // Clear storage states between runs to keep test boundaries completely isolated
        localStorage.clear();
    });

    test('should successfully post valid form data and display the success banner under tenant alpha', async () => {
        const user = userEvent.setup();

        // Seed localStorage to initialize the AuthProvider state core
        localStorage.setItem('certifreight_token', 'mock-valid-jwt');
        localStorage.setItem('certifreight_tenant_id', 'alpha');
        localStorage.setItem('certifreight_role', 'ROLE_DISPATCHER');

        render(
            <AuthProvider>
                <ShipmentForm />
            </AuthProvider>
        );

        const trackingInput = screen.getByLabelText(/tracking number/i);
        const weightInput = screen.getByLabelText(/weight \(lbs\)/i);
        const submitButton = screen.getByRole('button', { name: /registering manifest...|commit freight link/i });

        // Simulate standard user workflow
        await user.type(trackingInput, 'CFT-999999');
        await user.type(weightInput, '45000');
        await user.click(submitButton);

        // Wait for the async MSW response cycle to paint the UI update
        const successBanner = await screen.findByText(/manifest successfully created/i);
        expect(successBanner).toBeInTheDocument();

        // Confirm inputs clear out upon successful lifecycle submission
        expect(trackingInput).toHaveValue('');
        expect(weightInput).toHaveValue(null);
    });

    test('should capture and render backend validation messages when context mapping criteria fails', async () => {
        const user = userEvent.setup();

        // Seed an invalid workspace string to force the MSW interceptor to reject the packet
        localStorage.setItem('certifreight_token', 'mock-valid-jwt');
        localStorage.setItem('certifreight_tenant_id', 'unauthorized-workspace');
        localStorage.setItem('certifreight_role', 'ROLE_DISPATCHER');

        render(
            <AuthProvider>
                <ShipmentForm />
            </AuthProvider>
        );

        const trackingInput = screen.getByLabelText(/tracking number/i);
        const weightInput = screen.getByLabelText(/weight \(lbs\)/i);
        const submitButton = screen.getByRole('button', { name: /registering manifest...|commit freight link/i });

        await user.type(trackingInput, 'CFT-000000');
        await user.type(weightInput, '1000');
        await user.click(submitButton);

        // Assert that the MSW 400 response trickles down to the UI error layout
        const errorBanner = await screen.findByText(/tenant context missing/i);
        expect(errorBanner).toBeInTheDocument();
    });
});