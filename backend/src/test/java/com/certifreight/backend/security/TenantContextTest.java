package com.certifreight.backend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldStoreAndReturnTenantIdForCurrentThread() {
        TenantContext.setTenantId("alpha");

        assertEquals("alpha", TenantContext.getTenantId());
    }

    @Test
    void shouldClearTenantFromCurrentThread() {
        TenantContext.setTenantId("alpha");

        TenantContext.clear();

        assertNull(TenantContext.getTenantId());
    }

    @Test
    void shouldIsolateTenantByThread() throws Exception {
        TenantContext.setTenantId("main-thread");

        final String[] valueFromWorker = new String[1];
        Thread worker = new Thread(() -> valueFromWorker[0] = TenantContext.getTenantId());
        worker.start();
        worker.join();

        assertEquals("main-thread", TenantContext.getTenantId());
        assertNull(valueFromWorker[0]);
    }
}

