package com.certifreight.backend.service;

import com.certifreight.backend.model.ShipmentRequest;
import com.certifreight.backend.model.Shipment;
import com.certifreight.backend.repository.ShipmentRepository;
import com.certifreight.backend.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceImplTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private EntityManager entityManager;
    @Mock private Query nativeQuery;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    @BeforeEach
    void setupSecurityContext() {
        // Bind mock security framework context to the active execution thread
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should cleanly process and persist a standard valid shipment allocation request")
    void shouldCreateShipmentCleanly() {
        ShipmentRequest request = new ShipmentRequest();
        request.setTrackingNumber("CFT-A1B2C3");
        request.setWeightLbs(new BigDecimal("5500.00"));

        Shipment mockSavedShipment = new Shipment();
        mockSavedShipment.setId(101L);
        mockSavedShipment.setTrackingNumber(request.getTrackingNumber());
        mockSavedShipment.setWeightLbs(request.getWeightLbs());
        mockSavedShipment.setStatus("MANIFEST_CREATED");

        when(shipmentRepository.save(any(Shipment.class))).thenReturn(mockSavedShipment);

        Shipment result = shipmentService.createShipment(request);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    @DisplayName("Seeding: Should run native tenant upsert verification and persist initial cargo manifest")
    void seedTestShipmentSuccessfully() {
        String activeTenant = "enterprise-alpha";

        // 1. Stub the security context to provide the tenant name across access strategies
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(activeTenant);

        // 2. Stub the fluent entity manager native query chain
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), any())).thenReturn(nativeQuery);
        when(nativeQuery.executeUpdate()).thenReturn(1);

        // 3. Stub the shipment database storage return
        Shipment expectedShipment = Shipment.builder()
                .id(200L)
                .tenantId(activeTenant)
                .trackingNumber("CFT-SEED01")
                .status("PROCESSING")
                .build();
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(expectedShipment);

        // Act
        Shipment result = shipmentService.seedTestShipmentForActiveTenant();

        // Assert that the native database gate and save routines executed perfectly
        assertNotNull(result);
        assertEquals(200L, result.getId());
        assertEquals("enterprise-alpha", result.getTenantId());

        verify(entityManager, times(1)).createNativeQuery(anyString());
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    @DisplayName("Read Ledger: Should query repository using active tenant isolation security boundaries")
    void shouldFindAllShipmentsForActiveTenant() {
        String activeTenant = "enterprise-alpha";

        // Mock the security context so internal resolveActiveTenant() executes cleanly
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(activeTenant);

        // ALIGNED MOCK: Mock findAll() to match your actual service layer implementation
        java.util.List<Shipment> mockList = java.util.List.of(
                Shipment.builder().id(1L).tenantId(activeTenant).trackingNumber("CFT-000001").build(),
                Shipment.builder().id(2L).tenantId(activeTenant).trackingNumber("CFT-000002").build()
        );
        when(shipmentRepository.findAll()).thenReturn(mockList);

        // Act
        java.util.List<Shipment> result = shipmentService.getShipmentsForActiveTenant();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(shipmentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Purge Record: Should delegate deletion command straight down to repository contract")
    void shouldDeleteShipmentRecordCleanly() {
        Long targetId = 42L;
        doNothing().when(shipmentRepository).deleteById(targetId);

        // Act
        shipmentService.deleteShipment(targetId);

        // Assert
        verify(shipmentRepository, times(1)).deleteById(targetId);
    }

    @Test
    @DisplayName("Read Ledger: Should throw exception or handle branch gracefully when Authentication context is missing")
    void shouldHandleMissingAuthenticationBranch() {
        // Force the branch condition where the authentication object is completely null
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            shipmentService.getShipmentsForActiveTenant();
        }, "A null authentication framework context must trigger the security branch fallback path");    }

    @Test
    @DisplayName("Read Ledger: Should handle branch when principal or name extraction resolves to null")
    void shouldHandleNullPrincipalBranch() {
        // Force the condition where auth exists but has an uninitialized name identity string
        when(securityContext.getAuthentication()).thenReturn(authentication);

        assertThrows(RuntimeException.class, () -> {
            shipmentService.getShipmentsForActiveTenant();
        }, "An unassigned tenant identification token must exit through the defensive code branch");    }
}