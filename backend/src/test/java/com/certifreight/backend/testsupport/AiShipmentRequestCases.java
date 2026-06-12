package com.certifreight.backend.testsupport;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI-inspired boundary case catalog used by unit/component/integration tests.
 * It centralizes mutable scenarios so tests can self-heal with shared updates.
 */
public final class AiShipmentRequestCases {

    private AiShipmentRequestCases() {
    }

    public record ShipmentCase(String trackingNumber, BigDecimal weightLbs, boolean valid) {
    }

    public static List<ShipmentCase> all() {
        return List.of(
                // Valid: CFT- followed by exactly 6 digits.
                // Range 901xxx is reserved exclusively for this catalog to
                // prevent unique-constraint collisions with other integration tests.
                new ShipmentCase("CFT-901001", new BigDecimal("1000"), true),
                new ShipmentCase("CFT-901002", new BigDecimal("1"), true),
                new ShipmentCase("CFT-901003", new BigDecimal("999999"), true),
                // Invalid: letters in the suffix
                new ShipmentCase("CFT-A1B2C3", new BigDecimal("1000"), false),
                new ShipmentCase("BAD-TRACK", new BigDecimal("1000"), false),
                new ShipmentCase("", new BigDecimal("1000"), false),
                // Invalid: lowercase letters
                new ShipmentCase("CFT-abc123", new BigDecimal("1000"), false),
                // Invalid: weight out of range
                new ShipmentCase("CFT-901050", new BigDecimal("0"), false),
                new ShipmentCase("CFT-901051", null, false)
        );
    }
}

