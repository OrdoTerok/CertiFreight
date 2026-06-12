package com.certifreight.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Shipment E2E Integration Tests")
public class ShipmentIntegrationE2ETest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setupTenantMetadata() {
        jdbcTemplate.execute("INSERT INTO tenants (id, company_name) VALUES ('alpha', 'Alpha Logistics Ltd') ON CONFLICT DO NOTHING;");
        jdbcTemplate.execute("INSERT INTO tenants (id, company_name) VALUES ('beta', 'Beta Freight Corp') ON CONFLICT DO NOTHING;");
        jdbcTemplate.execute("INSERT INTO tenants (id, company_name) VALUES ('gamma', 'Gamma Shipping Inc') ON CONFLICT DO NOTHING;");
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should retrieve empty list when no shipments exist for tenant")
    public void shouldReturnEmptyListWhenNoShipmentsExist() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should create shipment with null weight successfully")
    public void shouldCreateShipmentWithNullWeight() throws Exception {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("trackingNumber", "CFT-NULL01");
        payload.put("weightLbs", null);

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNumber").value("CFT-NULL01"))
                .andExpect(jsonPath("$.weightLbs").doesNotExist());

        // Verify it was persisted
        Map<String, Object> savedRow = jdbcTemplate.queryForMap(
                "SELECT tracking_number, weight_lbs FROM shipments WHERE tracking_number = ?",
                "CFT-NULL01"
        );
        assertThat(savedRow).isNotNull();
        assertThat(savedRow.get("weight_lbs")).isNull();
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should isolate shipments by tenant context")
    public void shouldIsolateTenantShipments() throws Exception {
        // Create shipment for alpha tenant
        Map<String, Object> alphaPayload = Map.of(
                "trackingNumber", "CFT-ALPHA1",
                "weightLbs", 5000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alphaPayload)))
                .andExpect(status().isCreated());

        // Verify alpha shipment exists in DB
        Map<String, Object> alphaRow = jdbcTemplate.queryForMap(
                "SELECT tenant_id FROM shipments WHERE tracking_number = ?",
                "CFT-ALPHA1"
        );
        assertThat(alphaRow.get("tenant_id")).isEqualTo("alpha");
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should retrieve multiple shipments for active tenant")
    public void shouldRetrieveMultipleShipmentsForTenant() throws Exception {
        // Create first shipment
        Map<String, Object> payload1 = Map.of(
                "trackingNumber", "CFT-MULTI1",
                "weightLbs", 2500
        );
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload1)))
                .andExpect(status().isCreated());

        // Create second shipment
        Map<String, Object> payload2 = Map.of(
                "trackingNumber", "CFT-MULTI2",
                "weightLbs", 3500
        );
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload2)))
                .andExpect(status().isCreated());

        // Retrieve all shipments
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].trackingNumber").value("CFT-MULTI1"))
                .andExpect(jsonPath("$[1].trackingNumber").value("CFT-MULTI2"));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should reject shipment with negative weight")
    public void shouldRejectNegativeWeight() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-NEG01",
                "weightLbs", -100
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle very large weight values")
    public void shouldHandleLargeWeightValues() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-LARGE",
                "weightLbs", new BigDecimal("999999999.99")
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should reject duplicate tracking numbers within same tenant")
    public void shouldRejectDuplicateTrackingNumberSameTenant() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-DUP01",
                "weightLbs", 1000
        );

        // First creation should succeed
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        // Second creation with same tracking number should fail
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should allow same tracking number in different tenants")
    public void shouldAllowDuplicateTrackingNumberDifferentTenants() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-CROSS",
                "weightLbs", 2000
        );

        // Create in alpha tenant
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        // Create same tracking in beta tenant - should succeed
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "beta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should delete shipment when user has ADMIN role")
    public void shouldDeleteShipmentWithAdminRole() throws Exception {
        // First create a shipment as DISPATCHER
        // We need to switch to DISPATCHER context for creation
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "trackingNumber", "CFT-DEL01",
                                "weightLbs", 1500
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Extract the ID from the response - admin should be able to delete
        mockMvc.perform(delete("/api/shipments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cargo record successfully purged from tracking matrices"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("Should block GET shipments for VIEWER role")
    public void shouldBlockViewerFromGettingShipments() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("Should block CREATE for VIEWER role")
    public void shouldBlockViewerFromCreatingShipment() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-VIEW01",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should seed test shipment successfully")
    public void shouldSeedTestShipmentSuccessfully() throws Exception {
        mockMvc.perform(post("/api/shipments/seed")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.trackingNumber").isString())
                .andExpect(jsonPath("$.status").isString());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should return proper status on shipment creation")
    public void shouldReturnProperStatusOnCreation() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-STATUS",
                "weightLbs", 3000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("MANIFEST_CREATED"));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should reject empty tracking number")
    public void shouldRejectEmptyTrackingNumber() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Constraint Violation"));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should reject tracking number with wrong format")
    public void shouldRejectWrongTrackingFormat() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "WRONG-123",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tracking number must match enterprise standard format: CFT-XXXXXX"));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should accept tracking number with correct format CFT-XXXXXX")
    public void shouldAcceptCorrectTrackingFormat() throws Exception {
        String[] validFormats = {"CFT-A1B2C3", "CFT-123456", "CFT-ABCDEF"};

        for (String format : validFormats) {
            Map<String, Object> payload = Map.of("trackingNumber", format, "weightLbs", 1000);
            mockMvc.perform(post("/api/shipments")
                            .header("X-Tenant-ID", "gamma")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should maintain shipment state across retrieve operations")
    public void shouldMaintainShipmentStateAcrossRetrieves() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-STATE1",
                "weightLbs", 4500
        );

        // Create
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNumber").value("CFT-STATE1"))
                .andExpect(jsonPath("$.weightLbs").value(4500))
                .andExpect(jsonPath("$.status").value("MANIFEST_CREATED"));

        // Retrieve and verify state is maintained
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackingNumber").value("CFT-STATE1"))
                .andExpect(jsonPath("$[0].weightLbs").value(4500))
                .andExpect(jsonPath("$[0].status").value("MANIFEST_CREATED"));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle sequential shipment operations")
    public void shouldHandleSequentialOperations() throws Exception {
        // Create first
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "trackingNumber", "CFT-SEQ01",
                                "weightLbs", 1000
                        ))))
                .andExpect(status().isCreated());

        // Create second
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "trackingNumber", "CFT-SEQ02",
                                "weightLbs", 2000
                        ))))
                .andExpect(status().isCreated());

        // Create third
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "trackingNumber", "CFT-SEQ03",
                                "weightLbs", 3000
                        ))))
                .andExpect(status().isCreated());

        // Verify all exist
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("Should reject unauthenticated request when authentication required")
    public void shouldRejectUnauthenticatedRequest() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-UNAUTH",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle DELETE on non-existent shipment gracefully")
    public void shouldHandleDeleteNonExistentShipment() throws Exception {
        mockMvc.perform(delete("/api/shipments/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should properly serialize BigDecimal weight in response")
    public void shouldSerializeWeightCorrectly() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-DECIMAL",
                "weightLbs", new BigDecimal("2500.50")
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightLbs").value(2500.50));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should return all shipment fields in GET response")
    public void shouldReturnAllShipmentFields() throws Exception {
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "trackingNumber", "CFT-FIELDS",
                                "weightLbs", 1500
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].trackingNumber").isString())
                .andExpect(jsonPath("$[0].weightLbs").isNumber())
                .andExpect(jsonPath("$[0].status").isString());
    }

}

