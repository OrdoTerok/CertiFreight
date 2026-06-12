package com.certifreight.backend.integration;

import com.certifreight.backend.testsupport.AiShipmentRequestCases;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiGeneratedShipmentApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedTenantAndCleanCatalogData() {
        jdbcTemplate.execute("INSERT INTO tenants (id, company_name) VALUES ('alpha', 'Alpha Logistics Ltd') ON CONFLICT DO NOTHING;");
        // Purge any 901xxx catalog shipments left over from a previous run or Surefire retry,
        // preventing unique-constraint violations when this test is re-executed.
        jdbcTemplate.execute("DELETE FROM shipments WHERE tracking_number LIKE 'CFT-901%'");
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void shouldExerciseAiGeneratedPayloadCatalog() throws Exception {
        for (AiShipmentRequestCases.ShipmentCase scenario : AiShipmentRequestCases.all()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("trackingNumber", scenario.trackingNumber());
            payload.put("weightLbs", scenario.weightLbs());

            var action = mockMvc.perform(post("/api/shipments")
                    .header("X-Tenant-ID", "alpha")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)));

            if (scenario.valid()) {
                action.andExpect(status().isCreated());
            } else {
                action.andExpect(status().isBadRequest());
            }
        }
    }
}

