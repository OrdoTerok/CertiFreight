/**
 * Evaluates whether a custom tracking reference token strictly adheres
 * to the enterprise B2B formatting syntax: CFT-123456 (6 digits only)
 */
export const validateTrackingFormat = (trackingNum: string): boolean => {
    const trackingRegex = /^CFT-\d{6}$/;
    return trackingRegex.test(trackingNum);
};

/**
 * Normalizes user numeric weight entries into precise string displays
 * matching database scale parameters.
 */
export const formatCargoWeight = (weight: number | null | undefined): string => {
    if (weight === null || weight === undefined || isNaN(weight)) return '—';
    return `${Number(weight).toLocaleString(undefined, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    })} lbs`;
};