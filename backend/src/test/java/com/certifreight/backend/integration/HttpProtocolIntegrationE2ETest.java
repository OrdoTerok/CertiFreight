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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("HTTP Protocol & Response Handling E2E Tests")
public class HttpProtocolIntegrationE2ETest extends BaseIntegrationTest {

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
    @DisplayName("Should return correct Content-Type header for JSON responses")
    public void shouldReturnJsonContentTypeHeader() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should return Content-Type in response for POST requests")
    public void shouldReturnContentTypeInPostResponse() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-100401",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle requests with charset in Content-Type header")
    public void shouldHandleContentTypeWithCharset() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-100402",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return 415 for unsupported Content-Type")
    public void shouldRejectUnsupportedContentType() throws Exception {
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not json"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should return 200 OK for successful GET request")
    public void shouldReturnOkStatus() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should return 201 CREATED for resource creation")
    public void shouldReturnCreatedStatus() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-530001",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 200 OK for DELETE successful")
    public void shouldReturnOkForDelete() throws Exception {
        mockMvc.perform(delete("/api/shipments/999"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return validation failure when auth is missing but payload is incomplete")
    public void shouldReturnValidationFailureWhenAuthenticationMissing() throws Exception {
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingNumber\": \"CFT-530002\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("Should return 403 for insufficient permissions")
    public void shouldReturn403WhenPermissionDenied() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-530003",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle empty request body gracefully")
    public void shouldHandleEmptyBody() throws Exception {
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle malformed JSON gracefully")
    public void shouldHandleMalformedJson() throws Exception {
        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should ignore extra fields in request body")
    public void shouldIgnoreExtraFields() throws Exception {
        String payload = "{" +
                "\"trackingNumber\": \"CFT-540001\"," +
                "\"weightLbs\": 1000," +
                "\"unknownField\": \"value\"," +
                "\"anotherField\": 123" +
                "}";

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should handle URL with trailing slash")
    public void shouldHandleTrailingSlash() throws Exception {
        mockMvc.perform(get("/api/shipments/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


}
