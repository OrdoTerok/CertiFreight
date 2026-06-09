package com.certifreight.backend.controller;

import com.certifreight.backend.model.Shipment;
import com.certifreight.backend.model.ShipmentRequest;
import com.certifreight.backend.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @PostMapping
    public ResponseEntity<Shipment> createShipment(
            @Valid @RequestBody ShipmentRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId) {

        // The TenantContext is already bound by your TenantFilter filter layer from the JWT
        Shipment created = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // <-- GATEKEEPER: Instantly blocks execution if the JWT authority lacks 'ROLE_ADMIN'
    public ResponseEntity<Map<String, String>> deleteShipment(@PathVariable Long id) {
        shipmentService.deleteShipment(id); // Ensure this method is mapped in your service to execute repository.deleteById(id)
        return ResponseEntity.ok(Map.of("message", "Cargo record successfully purged from tracking matrices"));
    }
}