package com.certifreight.backend.model;

import com.certifreight.backend.model.ShipmentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Valid DTO configurations should pass validation without violations")
    void validDtoShouldHaveNoViolations() {
        ShipmentRequest request = new ShipmentRequest();
        request.setTrackingNumber("CFT-A1B2C3");
        request.setWeightLbs(new BigDecimal("5500.00"));

        Set<ConstraintViolation<ShipmentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid manifest payload should pass validation boundaries clean");
    }

    @Test
    @DisplayName("Should flag violation when tracking sequence violates standard format pattern")
    void invalidTrackingPatternShouldFail() {
        ShipmentRequest request = new ShipmentRequest();
        request.setTrackingNumber("BAD-PREFIX-123");
        request.setWeightLbs(new BigDecimal("1200.00"));

        Set<ConstraintViolation<ShipmentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Validation should fail for an invalid tracking pattern");

        // Assert directly on the field property name instead of the message text
        boolean hasTrackingViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("trackingNumber"));
        assertTrue(hasTrackingViolation, "Expected a validation defect on 'trackingNumber'");
    }

    @Test
    @DisplayName("Should flag violation when weight parameter falls below mandatory minimum scale limits")
    void weightBelowMinimumShouldFail() {
        ShipmentRequest request = new ShipmentRequest();
        request.setTrackingNumber("CFT-A1B2C3"); // Guaranteed valid pattern match
        request.setWeightLbs(new BigDecimal("0.00")); // Fails @DecimalMin constraint

        Set<ConstraintViolation<ShipmentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Validation should fail for zero or negative cargo weights");

        // ROBUST ASSERTON: Verify the engine caught a boundary breach specifically on the weight field
        boolean hasWeightViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("weightLbs"));

        assertTrue(hasWeightViolation, "The validation engine failed to map a constraint violation to the 'weightLbs' field");
    }
}