import { describe, it, expect } from 'vitest';
import { validateTrackingFormat, formatCargoWeight } from './trackingUtils';

describe('Logistics Ledger Verification Utilities', () => {

    describe('validateTrackingFormat()', () => {
        it('should pass cleanly on standard conforming alphanumeric sequences', () => {
            expect(validateTrackingFormat('CFT-A1B2C3')).toBe(true);
            expect(validateTrackingFormat('CFT-000000')).toBe(true);
            expect(validateTrackingFormat('CFT-ZZZZZZ')).toBe(true);
        });

        it('should reject strings failing the structural length boundaries', () => {
            expect(validateTrackingFormat('CFT-A1B2C')).toBe(false);   // Too short
            expect(validateTrackingFormat('CFT-A1B2C3D')).toBe(false); // Too long
        });

        it('should enforce strict case boundaries', () => {
            expect(validateTrackingFormat('CFT-abc123')).toBe(false);
        });

        it('should reject malformed or missing prefix markers', () => {
            expect(validateTrackingFormat('TRK-A1B2C3')).toBe(false);
            expect(validateTrackingFormat('A1B2C3')).toBe(false);
        });
    });

    describe('formatCargoWeight()', () => {
        it('should format numbers with standard scale formatting rules', () => {
            expect(formatCargoWeight(5500)).toBe('5,500.00 lbs');
            expect(formatCargoWeight(1234567.89)).toBe('1,234,567.89 lbs');
        });

        it('should cleanly handle fractional values matching decimal scales', () => {
            expect(formatCargoWeight(0.5)).toBe('0.50 lbs');
        });

        it('should return a placeholder marker if null values are passed down', () => {
            expect(formatCargoWeight(null)).toBe('—');
            expect(formatCargoWeight(undefined)).toBe('—');
            expect(formatCargoWeight(NaN)).toBe('—');
        });
    });
});