package com.certifreight.backend.service;

import com.certifreight.backend.model.Shipment;
import com.certifreight.backend.model.Tenant;
import com.certifreight.backend.repository.ShipmentRepository;
import com.certifreight.backend.repository.TenantRepository;
import com.certifreight.backend.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final TenantRepository tenantRepository;

    @Override
    public List<Shipment> getShipmentsForActiveTenant() {
        resolveActiveTenant();
        return shipmentRepository.findAll();
    }

    @Override
    @Transactional
    public Shipment seedTestShipmentForActiveTenant() {
        String activeTenantId = resolveActiveTenant();

        if (!tenantRepository.existsById(activeTenantId)) {
            log.info("Provisioning initial parent tenant record for ID: {}", activeTenantId);
            Tenant newTenant = Tenant.builder()
                    .id(activeTenantId)
                    .companyName(activeTenantId.toUpperCase().replace("-", " "))
                    .build();
            tenantRepository.save(newTenant);
        }

        Shipment mockShipment = Shipment.builder()
                .tenantId(activeTenantId)
                .trackingNumber("TRK-" + System.currentTimeMillis())
                .status("PROCESSING_FREIGHT")
                .build();

        log.info("Persisting new tenant-isolated shipment record");
        return shipmentRepository.save(mockShipment);
    }

    /**
     * Internal helper to extract the active tenant context, throwing a clean
     * security exception if an unauthenticated thread bypasses the gateway.
     */
    private String resolveActiveTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.error("Multi-tenant execution failure: No active tenant bound to thread context");
            throw new IllegalStateException("Access Denied: Missing organization scope");
        }
        return tenantId;
    }
}
