import { http, HttpResponse, HttpHandler } from 'msw';

export const handlers: HttpHandler[] = [
    // FIX: Using a wildcard match to intercept both relative and absolute Axios requests
    http.post('*/shipments', async ({ request }) => {
        const tenantId = request.headers.get('X-Tenant-ID');
        const body = await request.json() as { trackingNumber?: string; weightLbs?: number };

        // Strict multi-tenant verification check
        if (!tenantId || tenantId !== 'alpha') {
            return HttpResponse.json(
                { title: "Bad Request", detail: "Tenant Context Missing" },
                { status: 400 }
            );
        }

        return HttpResponse.json({
            id: 99381,
            trackingNumber: body.trackingNumber,
            weightLbs: body.weightLbs,
            status: 'MANIFEST_CREATED'
        }, { status: 201 });
    })
];