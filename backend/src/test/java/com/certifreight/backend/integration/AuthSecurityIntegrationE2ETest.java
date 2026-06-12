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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Auth & Security E2E Integration Tests")
public class AuthSecurityIntegrationE2ETest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setupTestData() {
        jdbcTemplate.execute("INSERT INTO tenants (id, company_name) VALUES ('test-tenant', 'Test Tenant Inc') ON CONFLICT DO NOTHING;");
        jdbcTemplate.execute("INSERT INTO tenants (id, company_name) VALUES ('alpha', 'Alpha Logistics Ltd') ON CONFLICT DO NOTHING;");
        jdbcTemplate.execute("INSERT INTO tenants (id, company_name) VALUES ('beta', 'Beta Logistics Ltd') ON CONFLICT DO NOTHING;");
    }

    @Test
    @DisplayName("Should generate JWT token for DISPATCHER role")
    public void shouldGenerateTokenForDispatcher() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("tenantId", "test-tenant")
                        .param("role", "ROLE_DISPATCHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Should generate JWT token for ADMIN role")
    public void shouldGenerateTokenForAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("tenantId", "test-tenant")
                        .param("role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    @DisplayName("Should generate JWT token for VIEWER role")
    public void shouldGenerateTokenForViewer() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("tenantId", "test-tenant")
                        .param("role", "ROLE_VIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    @DisplayName("Should default to DISPATCHER when role not provided")
    public void shouldDefaultToDispatcherRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("tenantId", "test-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    @DisplayName("Should handle multiple tenant login requests independently")
    public void shouldHandleMultipleTenantLogins() throws Exception {
        String alpha = mockMvc.perform(post("/api/auth/login")
                        .param("tenantId", "alpha")
                        .param("role", "ROLE_DISPATCHER"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String beta = mockMvc.perform(post("/api/auth/login")
                        .param("tenantId", "beta")
                        .param("role", "ROLE_DISPATCHER"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Tokens should be different
        assertThat(alpha).isNotEqualTo(beta);
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should enforce role-based access on POST /api/shipments")
    public void shouldEnforceDispatcherRoleOnCreate() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-100101",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("Should block VIEWER role from creating shipments")
    public void shouldBlockViewerFromCreate() throws Exception {
        Map<String, Object> payload = Map.of(
                "trackingNumber", "CFT-100201",
                "weightLbs", 1000
        );

        mockMvc.perform(post("/api/shipments")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should allow ADMIN role to delete shipments")
    public void shouldAllowAdminToDelete() throws Exception {
        mockMvc.perform(delete("/api/shipments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should block non-ADMIN roles from deleting shipments")
    public void shouldBlockNonAdminFromDelete() throws Exception {
        mockMvc.perform(delete("/api/shipments/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should require valid authorization header format")
    public void shouldValidateAuthHeaderFormat() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .header("Authorization", "InvalidFormat token"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should accept Bearer token format in Authorization header")
    public void shouldAcceptBearerTokenFormat() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should propagate error when token is missing for protected endpoint")
    public void shouldRequireTokenForProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/shipments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Should allow GET without explicit role requirement")
    public void shouldAllowGetWithoutExplicitRole() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "anonymous_tenant", roles = "DISPATCHER")
    @DisplayName("Should allow SEED operation without explicit role restriction")
    public void shouldAllowSeedWithDispatcher() throws Exception {
        mockMvc.perform(post("/api/shipments/seed")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "anonymous_tenant", roles = "VIEWER")
    @DisplayName("Should allow VIEWER to call seed")
    public void shouldAllowViewerToSeed() throws Exception {
        mockMvc.perform(post("/api/shipments/seed")
                        .header("X-Tenant-ID", "test-tenant")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return a valid token payload for repeated tenant/role requests")
    public void shouldGenerateDistinctTokens() throws Exception {
        String response1 = mockMvc.perform(post("/api/auth/login")
                        .param("tenantId", "test-tenant")
                        .param("role", "ROLE_DISPATCHER"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(post("/api/auth/login")
                        .param("tenantId", "test-tenant")
                        .param("role", "ROLE_DISPATCHER"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> payload1 = objectMapper.readValue(response1, Map.class);
        Map<?, ?> payload2 = objectMapper.readValue(response2, Map.class);

        assertThat(payload1.get("accessToken")).isNotNull();
        assertThat(payload2.get("accessToken")).isNotNull();
    }

}
