package com.certifreight.backend.service;

import com.certifreight.backend.model.Shipment;
import com.certifreight.backend.model.ShipmentRequest;
import com.certifreight.backend.repository.ShipmentRepository;
import com.certifreight.backend.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public List<Shipment> getShipmentsForActiveTenant() {
        resolveActiveTenant();
        return shipmentRepository.findAll();
    }

    @Override
    @Transactional
    public Shipment seedTestShipmentForActiveTenant() {
        String activeTenantId = resolveActiveTenant();

        log.info("Executing idempotent database write for tenant context: {}", activeTenantId);

        entityManager.createNativeQuery("""
            INSERT INTO tenants (id, company_name)
            VALUES (?, ?)
            ON CONFLICT (id) DO NOTHING
        """)
                .setParameter(1, activeTenantId)
                .setParameter(2, activeTenantId.toUpperCase().replace("-", " "))
                .executeUpdate();

        Shipment mockShipment = Shipment.builder()
                .tenantId(activeTenantId)
                .trackingNumber("TRK-" + System.currentTimeMillis())
                .weightLbs(new BigDecimal("5500.00"))
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
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            return tenantId;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new RuntimeException("Authentication context missing");
        }

        tenantId = authentication.getName();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new RuntimeException("Tenant identity token missing");
        }
        return tenantId;
    }

    @Transactional
    @Override
    public Shipment createShipment(ShipmentRequest request) {
        Shipment shipment = new Shipment();

        shipment.setTrackingNumber(request.getTrackingNumber());
        shipment.setWeightLbs(request.getWeightLbs());
        shipment.setStatus("MANIFEST_CREATED");

        return shipmentRepository.save(shipment);
    }

    @Override
    @Transactional
    public void deleteShipment(Long id) {
        shipmentRepository.deleteById(id);
    }
}
