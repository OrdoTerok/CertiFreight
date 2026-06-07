export interface Tenant {
    id: string;
    companyName: string;
    createdAt?: string;
}

export interface Shipment {
    id: number;
    tenantId: string;
    trackingNumber: string;
    status: string;
    weightLbs: number | null;
    createdAt: string;
    updatedAt: string;
}

export interface AuthResponse {
    accessToken: string;
}