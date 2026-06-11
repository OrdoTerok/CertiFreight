package com.certifreight.backend.controller;

import com.certifreight.backend.model.Shipment;
import com.certifreight.backend.service.ShipmentService;
import com.certifreight.backend.security.TenantFilter;
import com.certifreight.backend.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(ShipmentController.class)
@Import(SecurityConfig.class)
public class ShipmentControllerComponentTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ShipmentService shipmentService;

    @MockitoBean
    private TenantFilter tenantFilter;

    private Shipment mockShipment1;
    private Shipment mockShipment2;

    @BeforeEach
    public void setupDatasetVectors() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(tenantFilter).doFilter(any(), any(), any());

        mockShipment1 = new Shipment();
        mockShipment1.setId(1L);
        mockShipment1.setTrackingNumber("CFT-11111");
        mockShipment1.setWeightLbs(BigDecimal.valueOf(2500));
        mockShipment1.setStatus("MANIFEST_CREATED");

        mockShipment2 = new Shipment();
        mockShipment2.setId(2L);
        mockShipment2.setTrackingNumber("CFT-22222");
        mockShipment2.setWeightLbs(BigDecimal.valueOf(4800));
        mockShipment2.setStatus("PROCESSING");
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    public void shouldFetchAndReturnActiveShipmentLedgerCleanly() throws Exception {
        when(shipmentService.getShipmentsForActiveTenant()).thenReturn(List.of(mockShipment1, mockShipment2));

        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackingNumber").value("CFT-11111"))
                .andExpect(jsonPath("$[0].weightLbs").value(2500))
                .andExpect(jsonPath("$[1].trackingNumber").value("CFT-22222"))
                .andExpect(jsonPath("$[1].status").value("PROCESSING"));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    public void shouldAcceptAndCommitValidFreightLinkManifest() throws Exception {
        Map<String, Object> inputPayload = Map.of(
                "trackingNumber", "CFT-333333",
                "weightLbs", 6000
        );

        Shipment committedResult = new Shipment();
        committedResult.setId(3L);
        committedResult.setTrackingNumber("CFT-333333");
        committedResult.setWeightLbs(BigDecimal.valueOf(6000));
        committedResult.setStatus("MANIFEST_CREATED");

        when(shipmentService.createShipment(any())).thenReturn(committedResult);

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputPayload)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNumber").value("CFT-333333"))
                .andExpect(jsonPath("$.status").value("MANIFEST_CREATED"));

        verify(shipmentService, times(1)).createShipment(any());
    }

    @Test
    @WithMockUser(roles = "UNAUTHORIZED_VISITOR")
    public void shouldInterceptAndRejectRequestsWhenRoleVectorIsInvalid() throws Exception {
        Map<String, Object> inputPayload = Map.of(
                "trackingNumber", "CFT-333333",
                "weightLbs", 6000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputPayload)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(shipmentService, never()).createShipment(any());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    public void shouldRejectMalformedManifestPayloadWhenValidationFails() throws Exception {
        Map<String, Object> invalidPayload = Map.of(
                "trackingNumber", "BAD-REF",
                "weightLbs", 6000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Constraint Violation"))
                .andExpect(jsonPath("$.detail").value("Tracking number must match enterprise standard format: CFT-XXXXXX"));

        verify(shipmentService, never()).createShipment(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void shouldAllowDeleteWhenCallerHasAdminRole() throws Exception {
        doNothing().when(shipmentService).deleteShipment(9L);

        mockMvc.perform(delete("/api/shipments/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cargo record successfully purged from tracking matrices"));

        verify(shipmentService, times(1)).deleteShipment(eq(9L));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    public void shouldBlockDeleteWhenCallerIsNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/shipments/9"))
                .andExpect(status().isForbidden());

        verify(shipmentService, never()).deleteShipment(any());
    }
}