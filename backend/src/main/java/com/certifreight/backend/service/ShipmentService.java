package com.certifreight.backend.service;

import com.certifreight.backend.model.Shipment;
import com.certifreight.backend.model.ShipmentRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ShipmentService {

    /**
     * Retrieves all shipments belonging to the active authenticated tenant context.
     */
    List<Shipment> getShipmentsForActiveTenant();

    /**
     * Safely provisions a test shipment payload, establishing the parent organization
     * record if it doesn't exist yet.
     */
    Shipment seedTestShipmentForActiveTenant();

    @Transactional
    Shipment createShipment(ShipmentRequest request);

    @Transactional
    void deleteShipment(Long id);
}