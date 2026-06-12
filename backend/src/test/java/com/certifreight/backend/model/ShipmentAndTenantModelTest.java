package com.certifreight.backend.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentAndTenantModelTest {

    @Test
    void shipmentPrePersistShouldSetCreationAndUpdateTimestamps() {
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber("CFT-ABC123");
        shipment.setStatus("MANIFEST_CREATED");
        shipment.setWeightLbs(new BigDecimal("5500"));

        shipment.onCreate();

        assertNotNull(shipment.getCreatedAt());
        assertNotNull(shipment.getUpdatedAt());
    }

    @Test
    void shipmentPreUpdateShouldRefreshUpdatedAtTimestamp() {
        Shipment shipment = new Shipment();
        shipment.onCreate();
        ZonedDateTime oldUpdatedAt = shipment.getUpdatedAt();

        shipment.onUpdate();

        assertNotNull(shipment.getUpdatedAt());
        assertFalse(shipment.getUpdatedAt().isBefore(oldUpdatedAt));
    }

    @Test
    void tenantPrePersistShouldSetCreatedAtTimestamp() {
        Tenant tenant = new Tenant();
        tenant.setId("alpha");
        tenant.setCompanyName("Alpha Logistics");

        tenant.onCreate();

        assertNotNull(tenant.getCreatedAt());
    }
}

