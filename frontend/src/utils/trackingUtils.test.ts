import { describe, it, expect } from 'vitest';
import { validateTrackingFormat, formatCargoWeight } from './trackingUtils';

describe('Logistics Ledger Verification Utilities', () => {

    describe('validateTrackingFormat()', () => {
        it('should pass cleanly on standard conforming digit sequences', () => {
            expect(validateTrackingFormat('CFT-123456')).toBe(true);
            expect(validateTrackingFormat('CFT-000000')).toBe(true);
            expect(validateTrackingFormat('CFT-999999')).toBe(true);
        });

        it('should reject alphanumeric sequences that contain letters', () => {
            expect(validateTrackingFormat('CFT-A1B2C3')).toBe(false);
            expect(validateTrackingFormat('CFT-ABCDEF')).toBe(false);
            expect(validateTrackingFormat('CFT-ZZZZZZ')).toBe(false);
        });

        it('should reject strings failing the structural length boundaries', () => {
            expect(validateTrackingFormat('CFT-12345')).toBe(false);   // Too short
            expect(validateTrackingFormat('CFT-1234567')).toBe(false); // Too long
        });

        it('should reject malformed or missing prefix markers', () => {
            expect(validateTrackingFormat('TRK-123456')).toBe(false);
            expect(validateTrackingFormat('123456')).toBe(false);
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