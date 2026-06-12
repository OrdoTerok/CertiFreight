package com.certifreight.backend.controller;

import com.certifreight.backend.model.Shipment;
import com.certifreight.backend.service.ShipmentService;
import com.certifreight.backend.security.TenantFilter;
import com.certifreight.backend.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@DisplayName("Shipment Controller E2E Component Tests")
@WebMvcTest(ShipmentController.class)
@Import(SecurityConfig.class)
public class ShipmentControllerE2EComponentTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ShipmentService shipmentService;

    @MockitoBean
    private TenantFilter tenantFilter;

    @BeforeEach
    public void setupMockFilters() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(tenantFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should return 200 OK with empty list when no shipments exist")
    public void shouldReturnEmptyListCorrectly() throws Exception {
        when(shipmentService.getShipmentsForActiveTenant()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should return all shipment details in list response")
    public void shouldReturnCompleteShipmentData() throws Exception {
        Shipment shipment1 = new Shipment();
        shipment1.setId(1L);
        shipment1.setTrackingNumber("CFT-11111");
        shipment1.setWeightLbs(BigDecimal.valueOf(2500.50));
        shipment1.setStatus("MANIFEST_CREATED");
        shipment1.setTenantId("alpha");

        Shipment shipment2 = new Shipment();
        shipment2.setId(2L);
        shipment2.setTrackingNumber("CFT-22222");
        shipment2.setWeightLbs(null);
        shipment2.setStatus("PROCESSING");
        shipment2.setTenantId("alpha");

        when(shipmentService.getShipmentsForActiveTenant()).thenReturn(List.of(shipment1, shipment2));

        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].trackingNumber").value("CFT-11111"))
                .andExpect(jsonPath("$[0].weightLbs").value(2500.50))
                .andExpect(jsonPath("$[0].status").value("MANIFEST_CREATED"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].trackingNumber").value("CFT-22222"))
                .andExpect(jsonPath("$[1].status").value("PROCESSING"));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should create shipment and return 201 CREATED status")
    public void shouldCreateShipmentWithProperHttpStatus() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-NEW001",
                "weightLbs", 5000
        );

        Shipment createdShipment = new Shipment();
        createdShipment.setId(100L);
        createdShipment.setTrackingNumber("CFT-NEW001");
        createdShipment.setWeightLbs(BigDecimal.valueOf(5000));
        createdShipment.setStatus("MANIFEST_CREATED");

        when(shipmentService.createShipment(any())).thenReturn(createdShipment);

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.trackingNumber").value("CFT-NEW001"))
                .andExpect(jsonPath("$.status").value("MANIFEST_CREATED"));

        verify(shipmentService, times(1)).createShipment(any());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle shipment with null weight gracefully")
    public void shouldHandleNullWeightInResponse() throws Exception {
        Shipment shipment = new Shipment();
        shipment.setId(50L);
        shipment.setTrackingNumber("CFT-NULLWT");
        shipment.setWeightLbs(null);
        shipment.setStatus("MANIFEST_CREATED");

        when(shipmentService.createShipment(any())).thenReturn(shipment);

        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-NULLWT"
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightLbs").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should reject missing required fields with 400 Bad Request")
    public void shouldRejectMissingRequiredFields() throws Exception {
        // Missing tracking number
        Map<String, Object> missingTrackingPayload = Map.of(
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingTrackingPayload)))
                .andExpect(status().isBadRequest());

        verify(shipmentService, never()).createShipment(any());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should validate tracking number format and return descriptive error")
    public void shouldValidateTrackingNumberFormat() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "trackingNumber", "INVALID",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tracking number must match enterprise standard format: CFT-XXXXXX"));

        verify(shipmentService, never()).createShipment(any());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should pass valid tracking number formats to service")
    public void shouldAcceptValidTrackingFormats() throws Exception {
        String[] validFormats = {"CFT-123456", "CFT-ABCDEF", "CFT-A1B2C3"};

        Shipment createdShipment = new Shipment();
        createdShipment.setId(1L);
        createdShipment.setStatus("MANIFEST_CREATED");

        when(shipmentService.createShipment(any())).thenReturn(createdShipment);

        for (String format : validFormats) {
            Map<String, Object> payload = Map.of(
                    "trackingNumber", format,
                    "weightLbs", 1000
            );

            mockMvc.perform(post("/api/shipments")
                            .header("X-Tenant-ID", "alpha")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return success message on delete with ADMIN role")
    public void shouldReturnSuccessMessageOnDelete() throws Exception {
        doNothing().when(shipmentService).deleteShipment(anyLong());

        mockMvc.perform(delete("/api/shipments/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cargo record successfully purged from tracking matrices"));

        verify(shipmentService, times(1)).deleteShipment(42L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should invoke service delete with correct ID")
    public void shouldPassCorrectIdToService() throws Exception {
        doNothing().when(shipmentService).deleteShipment(anyLong());

        mockMvc.perform(delete("/api/shipments/123"))
                .andExpect(status().isOk());

        verify(shipmentService).deleteShipment(123L);
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should reject DELETE from non-ADMIN with 403 Forbidden")
    public void shouldRejectNonAdminDelete() throws Exception {
        mockMvc.perform(delete("/api/shipments/42"))
                .andExpect(status().isForbidden());

        verify(shipmentService, never()).deleteShipment(any());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("Should reject POST from VIEWER role with 403 Forbidden")
    public void shouldRejectViewerPost() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-VIEW01",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());

        verify(shipmentService, never()).createShipment(any());
    }

    @Test
    @DisplayName("Should reject POST without authentication with 401 Unauthorized")
    public void shouldRejectUnauthenticatedPost() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-UNAUTH",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        verify(shipmentService, never()).createShipment(any());
    }

    @Test
    @DisplayName("Should reject GET without authentication with 401 Unauthorized")
    public void shouldRejectUnauthenticatedGet() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(shipmentService, never()).getShipmentsForActiveTenant();
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle large monetary amounts in weight field")
    public void shouldHandleLargeWeightValues() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-LARGE",
                "weightLbs", new BigDecimal("999999.99")
        );

        Shipment createdShipment = new Shipment();
        createdShipment.setId(1L);
        createdShipment.setWeightLbs(new BigDecimal("999999.99"));
        createdShipment.setStatus("MANIFEST_CREATED");

        when(shipmentService.createShipment(any())).thenReturn(createdShipment);

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightLbs").value(999999.99));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should include X-Tenant-ID header in request without error")
    public void shouldAcceptTenantHeaderInRequest() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-TENANT",
                "weightLbs", 1000
        );

        Shipment createdShipment = new Shipment();
        createdShipment.setId(1L);
        createdShipment.setStatus("MANIFEST_CREATED");

        when(shipmentService.createShipment(any())).thenReturn(createdShipment);

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should seed test shipment and return proper response structure")
    public void shouldSeedShipmentWithProperStructure() throws Exception {
        Shipment seededShipment = new Shipment();
        seededShipment.setId(999L);
        seededShipment.setTrackingNumber("CFT-SEED-TEST");
        seededShipment.setStatus("PROCESSING");

        when(shipmentService.seedTestShipmentForActiveTenant()).thenReturn(seededShipment);

        mockMvc.perform(post("/api/shipments/seed")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(999))
                .andExpect(jsonPath("$.trackingNumber").value("CFT-SEED-TEST"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        verify(shipmentService, times(1)).seedTestShipmentForActiveTenant();
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle request with no X-Tenant-ID header")
    public void shouldHandleWithoutTenantHeader() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-NOTENANT",
                "weightLbs", 1000
        );

        Shipment createdShipment = new Shipment();
        createdShipment.setId(1L);
        createdShipment.setStatus("MANIFEST_CREATED");

        when(shipmentService.createShipment(any())).thenReturn(createdShipment);

        mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

}

