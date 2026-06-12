package com.certifreight.backend.config;

import com.certifreight.backend.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantIdentifierResolverTest {

    private final TenantIdentifierResolver resolver = new TenantIdentifierResolver();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldResolveTenantFromThreadContextWhenPresent() {
        TenantContext.setTenantId("enterprise-alpha");

        assertEquals("enterprise-alpha", resolver.resolveCurrentTenantIdentifier());
    }

    @Test
    void shouldFallbackToAnonymousWhenTenantIsNull() {
        TenantContext.clear();

        assertEquals("anonymous_tenant", resolver.resolveCurrentTenantIdentifier());
    }

    @Test
    void shouldFallbackToAnonymousWhenTenantIsBlank() {
        TenantContext.setTenantId("   ");

        assertEquals("anonymous_tenant", resolver.resolveCurrentTenantIdentifier());
    }

    @Test
    void shouldAlwaysValidateExistingSessions() {
        assertTrue(resolver.validateExistingCurrentSessions());
    }
}

