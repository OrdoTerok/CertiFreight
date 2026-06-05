package com.certifreight.backend.controller;

import com.certifreight.backend.model.Shipment;
import com.certifreight.backend.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Slf4j
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping
    public ResponseEntity<List<Shipment>> getTenantShipments() {
        log.debug("HTTP GET /api/shipments incoming request");
        List<Shipment> shipments = shipmentService.getShipmentsForActiveTenant();
        return ResponseEntity.ok(shipments);
    }

    @PostMapping("/seed")
    public ResponseEntity<Shipment> seedTestShipment() {
        log.debug("HTTP POST /api/shipments/seed incoming request");
        Shipment savedShipment = shipmentService.seedTestShipmentForActiveTenant();
        return ResponseEntity.ok(savedShipment);
    }
}