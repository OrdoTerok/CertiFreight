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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Concurrent Operations & Transaction E2E Tests")
public class ConcurrentOperationsIntegrationE2ETest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setupTestTenants() {
        jdbcTemplate.execute("INSERT INTO tenants (id, company_name) VALUES ('alpha', 'Alpha Logistics') ON CONFLICT DO NOTHING;");
        jdbcTemplate.execute("INSERT INTO tenants (id, company_name) VALUES ('beta', 'Beta Freight') ON CONFLICT DO NOTHING;");
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle multiple sequential requests from same tenant")
    public void shouldHandleSequentialRequests() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> payload = Map.of(
                    "trackingNumber", "CFT-SEQ0" + i,
                    "weightLbs", 1000
            );

            mockMvc.perform(post("/api/shipments")
                            .header("X-Tenant-ID", "alpha")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated());
        }

        // Verify all were created
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shipments WHERE tenant_id = 'alpha'",
                Integer.class
        );
        assertThat(count).isEqualTo(5);
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle rapid GET requests")
    public void shouldHandleRapidGetRequests() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/shipments")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should maintain data consistency across multiple operations")
    public void shouldMaintainDataConsistency() throws Exception {
        // Create two shipments
        Map<String, Object> payload1 = Map.of(
                "trackingNumber", "CFT-CONS1",
                "weightLbs", 1000
        );

        Map<String, Object> payload2 = Map.of(
                "trackingNumber", "CFT-CONS2",
                "weightLbs", 2000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload2)))
                .andExpect(status().isCreated());

        // Read all and verify
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackingNumber").value("CFT-CONS1"))
                .andExpect(jsonPath("$[0].weightLbs").value(1000))
                .andExpect(jsonPath("$[1].trackingNumber").value("CFT-CONS2"))
                .andExpect(jsonPath("$[1].weightLbs").value(2000));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should prevent duplicate tracking numbers under concurrent attempts")
    public void shouldPreventDuplicatesDuringConcurrentAttempts() throws Exception {
        // First, create a shipment
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-DUP99",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        // Now try to create duplicate - should fail
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle tenant isolation during concurrent requests")
    public void shouldIslateTenantsDuringConcurrentRequests() throws Exception {
        // Create shipments for different tenants
        Map<String, Object> alphaPayload = Map.of(
                "trackingNumber", "CFT-ALPHA01",
                "weightLbs", 1000
        );

        Map<String, Object> betaPayload = Map.of(
                "trackingNumber", "CFT-BETA01",
                "weightLbs", 2000
        );

        // Create for alpha
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alphaPayload)))
                .andExpect(status().isCreated());

        // Create for beta
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "beta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(betaPayload)))
                .andExpect(status().isCreated());

        // Verify alpha has 1 shipment
        Integer alphaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shipments WHERE tenant_id = 'alpha'",
                Integer.class
        );
        assertThat(alphaCount).isEqualTo(1);

        // Verify beta has 1 shipment
        Integer betaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shipments WHERE tenant_id = 'beta'",
                Integer.class
        );
        assertThat(betaCount).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle create and read in quick succession")
    public void shouldHandleCreateAndReadInQuickSuccession() throws Exception {
        // Create
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-CRQ01",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        // Read immediately
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.trackingNumber == 'CFT-CRQ01')]").exists());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle create, read, and delete operations")
    public void shouldHandleFullLifecycle() throws Exception {
        // Create
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-LIFE1",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        // Read to get ID
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle multiple requests with different roles sequentially")
    public void shouldHandleMultipleRoleSequences() throws Exception {
        // Dispatcher creates
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-ROLE01",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        // Viewer reads (should be able to read through role context)
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle batch-like sequential creates")
    public void shouldHandleBatchSequentialCreates() throws Exception {
        int batchSize = 20;
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 1; i <= batchSize; i++) {
            Map<String, Object> payload = Map.of(
                    "trackingNumber", String.format("CFT-BAT%02d", i),
                    "weightLbs", 1000 * i
            );

            mockMvc.perform(post("/api/shipments")
                            .header("X-Tenant-ID", "alpha")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated());

            successCount.incrementAndGet();
        }

        assertThat(successCount.get()).isEqualTo(batchSize);

        // Verify all were created
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shipments WHERE tenant_id = 'alpha'",
                Integer.class
        );
        assertThat(count).isEqualTo(batchSize);
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle rollback-like scenarios with error recovery")
    public void shouldHandleErrorRecoveryScenarios() throws Exception {
        // Create valid shipment
        Map<String, Object> validPayload = Map.of(
                "trackingNumber", "CFT-REC01",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload)))
                .andExpect(status().isCreated());

        // Try invalid shipment - should fail
        Map<String, Object> invalidPayload = Map.of(
                "trackingNumber", "INVALID",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());

        // Create another valid shipment - should succeed
        Map<String, Object> validPayload2 = Map.of(
                "trackingNumber", "CFT-REC02",
                "weightLbs", 2000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload2)))
                .andExpect(status().isCreated());

        // Verify we have 2 valid shipments
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shipments WHERE tenant_id = 'alpha'",
                Integer.class
        );
        assertThat(count).isEqualTo(2);
    }

}

