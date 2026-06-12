package com.certifreight.backend.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.BeforeEach;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void resetSharedTables() {
		// Keep tenant fixtures intact; clear mutable shipment state to make tests deterministic.
		jdbcTemplate.execute("DELETE FROM shipments");
		// Hibernate tenant resolver falls back to anonymous_tenant when TenantContext is empty.
		// Most integration tests authenticate with @WithMockUser (no JWT), so we seed this tenant once per test.
		jdbcTemplate.execute("""
			INSERT INTO tenants (id, company_name)
			VALUES ('anonymous_tenant', 'Default Anonymous Workspace')
			ON CONFLICT (id) DO NOTHING
		""");
	}
}
