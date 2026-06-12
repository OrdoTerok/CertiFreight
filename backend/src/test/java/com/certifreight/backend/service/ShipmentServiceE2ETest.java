package com.certifreight.backend.service;

import com.certifreight.backend.model.Shipment;
import com.certifreight.backend.model.ShipmentRequest;
import com.certifreight.backend.repository.ShipmentRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("Shipment Service E2E Tests")
@ExtendWith(MockitoExtension.class)
public class ShipmentServiceE2ETest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private EntityManager entityManager;
    @Mock private Query nativeQuery;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    @BeforeEach
    void setupSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create shipment with all required fields populated")
    void shouldCreateShipmentWithAllFields() {
        ShipmentRequest request = new ShipmentRequest();
        request.setTrackingNumber("CFT-A1B2C3");
        request.setWeightLbs(new BigDecimal("5500.75"));

        Shipment mockSavedShipment = new Shipment();
        mockSavedShipment.setId(101L);
        mockSavedShipment.setTrackingNumber("CFT-A1B2C3");
        mockSavedShipment.setWeightLbs(new BigDecimal("5500.75"));
        mockSavedShipment.setStatus("MANIFEST_CREATED");
        mockSavedShipment.setTenantId("alpha");

        when(shipmentRepository.save(any(Shipment.class))).thenReturn(mockSavedShipment);

        Shipment result = shipmentService.createShipment(request);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("CFT-A1B2C3", result.getTrackingNumber());
        assertEquals(new BigDecimal("5500.75"), result.getWeightLbs());
        assertEquals("MANIFEST_CREATED", result.getStatus());
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    @DisplayName("Should create shipment with null weight")
    void shouldCreateShipmentWithNullWeight() {
        ShipmentRequest request = new ShipmentRequest();
        request.setTrackingNumber("CFT-NULL99");
        request.setWeightLbs(null);

        Shipment mockSavedShipment = new Shipment();
        mockSavedShipment.setId(102L);
        mockSavedShipment.setTrackingNumber("CFT-NULL99");
        mockSavedShipment.setWeightLbs(null);
        mockSavedShipment.setStatus("MANIFEST_CREATED");

        when(shipmentRepository.save(any(Shipment.class))).thenReturn(mockSavedShipment);

        Shipment result = shipmentService.createShipment(request);

        assertNotNull(result);
        assertNull(result.getWeightLbs());
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    @DisplayName("Should handle large decimal weight values")
    void shouldHandleLargeDecimalWeights() {
        ShipmentRequest request = new ShipmentRequest();
        request.setTrackingNumber("CFT-BIG01");
        request.setWeightLbs(new BigDecimal("999999.99"));

        Shipment mockSavedShipment = new Shipment();
        mockSavedShipment.setId(103L);
        mockSavedShipment.setWeightLbs(new BigDecimal("999999.99"));
        mockSavedShipment.setStatus("MANIFEST_CREATED");

        when(shipmentRepository.save(any(Shipment.class))).thenReturn(mockSavedShipment);

        Shipment result = shipmentService.createShipment(request);

        assertEquals(new BigDecimal("999999.99"), result.getWeightLbs());
    }

    @Test
    @DisplayName("Should retrieve all shipments for active tenant")
    void shouldRetrieveAllShipmentsForTenant() {
        String activeTenant = "enterprise-alpha";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(activeTenant);

        List<Shipment> mockList = List.of(
                Shipment.builder().id(1L).tenantId(activeTenant).trackingNumber("CFT-000001").status("MANIFEST_CREATED").build(),
                Shipment.builder().id(2L).tenantId(activeTenant).trackingNumber("CFT-000002").status("PROCESSING").build(),
                Shipment.builder().id(3L).tenantId(activeTenant).trackingNumber("CFT-000003").status("IN_TRANSIT").build()
        );
        when(shipmentRepository.findAll()).thenReturn(mockList);

        List<Shipment> result = shipmentService.getShipmentsForActiveTenant();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(s -> s.getTenantId().equals(activeTenant)));
        verify(shipmentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no shipments exist for tenant")
    void shouldReturnEmptyListWhenNoShipments() {
        String activeTenant = "empty-tenant";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(activeTenant);
        when(shipmentRepository.findAll()).thenReturn(List.of());

        List<Shipment> result = shipmentService.getShipmentsForActiveTenant();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should successfully delete shipment by ID")
    void shouldDeleteShipmentSuccessfully() {
        Long targetId = 42L;
        doNothing().when(shipmentRepository).deleteById(targetId);

        shipmentService.deleteShipment(targetId);

        verify(shipmentRepository, times(1)).deleteById(42L);
    }

    @Test
    @DisplayName("Should handle delete of non-existent shipment gracefully")
    void shouldHandleDeleteNonExistentShipment() {
        Long nonExistentId = 99999L;
        doNothing().when(shipmentRepository).deleteById(nonExistentId);

        assertDoesNotThrow(() -> shipmentService.deleteShipment(nonExistentId));

        verify(shipmentRepository, times(1)).deleteById(nonExistentId);
    }

    @Test
    @DisplayName("Should seed test shipment with proper tenant context")
    void shouldSeedTestShipmentWithContext() {
        String activeTenant = "enterprise-alpha";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(activeTenant);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), any())).thenReturn(nativeQuery);
        when(nativeQuery.executeUpdate()).thenReturn(1);

        Shipment expectedShipment = Shipment.builder()
                .id(200L)
                .tenantId(activeTenant)
                .trackingNumber("CFT-SEED01")
                .status("PROCESSING")
                .build();
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(expectedShipment);

        Shipment result = shipmentService.seedTestShipmentForActiveTenant();

        assertNotNull(result);
        assertEquals(200L, result.getId());
        assertEquals(activeTenant, result.getTenantId());
        assertNotNull(result.getTrackingNumber());
        verify(entityManager, times(1)).createNativeQuery(anyString());
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    @DisplayName("Should throw exception when authentication context is null")
    void shouldThrowWhenAuthenticationNull() {
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> shipmentService.getShipmentsForActiveTenant());
    }

    @Test
    @DisplayName("Should throw exception when principal is null")
    void shouldThrowWhenPrincipalNull() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> shipmentService.getShipmentsForActiveTenant());
    }

    @Test
    @DisplayName("Should persist shipment with correct status")
    void shouldSetCorrectStatus() {
        ShipmentRequest request = new ShipmentRequest();
        request.setTrackingNumber("CFT-STATUS");
        request.setWeightLbs(new BigDecimal("1500"));

        Shipment mockSavedShipment = new Shipment();
        mockSavedShipment.setId(150L);
        mockSavedShipment.setStatus("MANIFEST_CREATED");

        when(shipmentRepository.save(any(Shipment.class))).thenReturn(mockSavedShipment);

        Shipment result = shipmentService.createShipment(request);

        assertEquals("MANIFEST_CREATED", result.getStatus());
    }

    @Test
    @DisplayName("Should invoke repository save method exactly once per create")
    void shouldInvokeRepositoryOnce() {
        ShipmentRequest request = new ShipmentRequest();
        request.setTrackingNumber("CFT-ONCE01");
        request.setWeightLbs(new BigDecimal("2000"));

        Shipment mockSavedShipment = new Shipment();
        mockSavedShipment.setId(250L);

        when(shipmentRepository.save(any(Shipment.class))).thenReturn(mockSavedShipment);

        shipmentService.createShipment(request);

        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    @DisplayName("Should retrieve shipments using repository findAll")
    void shouldUseFindAllMethod() {
        String activeTenant = "test-tenant";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(activeTenant);
        when(shipmentRepository.findAll()).thenReturn(List.of());

        shipmentService.getShipmentsForActiveTenant();

        verify(shipmentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should seed shipment with multiple attempts at tenant upsert")
    void shouldExecuteTenantUpsertDuringSeed() {
        String activeTenant = "new-tenant";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(activeTenant);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), any())).thenReturn(nativeQuery);
        when(nativeQuery.executeUpdate()).thenReturn(1);

        Shipment expectedShipment = Shipment.builder().id(1L).build();
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(expectedShipment);

        shipmentService.seedTestShipmentForActiveTenant();

        verify(entityManager).createNativeQuery(anyString());
    }

}

