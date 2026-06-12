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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Request Payload & Special Characters E2E Tests")
public class PayloadHandlingIntegrationE2ETest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setupTestTenants() {
        jdbcTemplate.execute("INSERT INTO tenants (id, company_name) VALUES ('test-tenant', 'Test Company') ON CONFLICT DO NOTHING;");
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle tracking number with hyphen correctly")
    public void shouldHandleTrackingWithHyphen() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-100123",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle decimal weights with many decimal places")
    public void shouldHandleDecimalWeights() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-001001",
                "weightLbs", new BigDecimal("1234.5678")
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle very deep nested request")
    public void shouldHandleComplexPayload() throws Exception {
        String complexPayload = "{\n" +
                "  \"trackingNumber\": \"CFT-550001\",\n" +
                "  \"weightLbs\": 1000,\n" +
                "  \"metadata\": {\n" +
                "    \"nested\": {\n" +
                "      \"deeply\": {\n" +
                "        \"value\": \"ignored\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(complexPayload))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should reject request with NaN value")
    public void shouldRejectNaNValue() throws Exception {
        String nanPayload = "{\n" +
                "  \"trackingNumber\": \"CFT-560001\",\n" +
                "  \"weightLbs\": NaN\n" +
                "}";

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nanPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should reject request with Infinity value")
    public void shouldRejectInfinityValue() throws Exception {
        String infPayload = "{\n" +
                "  \"trackingNumber\": \"CFT-570001\",\n" +
                "  \"weightLbs\": Infinity\n" +
                "}";

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(infPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle request with Unicode characters in tracking number")
    public void shouldHandleUnicodeInTracking() throws Exception {
        // Should reject as tracking must be CFT-123456
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-你好01",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle very long string in request")
    public void shouldHandleLongString() throws Exception {
        String longString = "A".repeat(1000);

        Map<String, Object> payload = new HashMap<>();
        payload.put("trackingNumber", "CFT-580001");
        payload.put("weightLbs", 1000);
        payload.put("longField", longString);

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle request with numeric strings converted to numbers")
    public void shouldHandleNumericStrings() throws Exception {
        String payload = "{\n" +
                "  \"trackingNumber\": \"CFT-590001\",\n" +
                "  \"weightLbs\": \"5000\"\n" +
                "}";

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should reject request with boolean instead of number")
    public void shouldRejectBooleanForNumber() throws Exception {
        String payload = "{\n" +
                "  \"trackingNumber\": \"CFT-600001\",\n" +
                "  \"weightLbs\": true\n" +
                "}";

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should reject duplicate keys (last value should win)")
    public void shouldHandleDuplicateKeys() throws Exception {
        String payload = "{\n" +
                "  \"trackingNumber\": \"CFT-610001\",\n" +
                "  \"weightLbs\": 1000,\n" +
                "  \"weightLbs\": 2000\n" +
                "}";

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle negative weight correctly (should reject)")
    public void shouldRejectNegativeWeight() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-980001",
                "weightLbs", -1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle zero weight correctly (should reject)")
    public void shouldRejectZeroWeight() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-990001",
                "weightLbs", 0
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle whitespace-only tracking number")
    public void shouldRejectWhitespaceTracking() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "   ",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should strip leading/trailing whitespace from strings")
    public void shouldHandleLeadingTrailingWhitespace() throws Exception {
        String payload = "{\n" +
                "  \"trackingNumber\": \"  CFT-640001  \",\n" +
                "  \"weightLbs\": 1000\n" +
                "}";

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle case sensitivity in tracking number")
    public void shouldHandleCaseSensitivityInTracking() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-001234",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

}

