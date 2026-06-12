package com.certifreight.backend.model;

import com.certifreight.backend.testsupport.AiShipmentRequestCases;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiGeneratedShipmentRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    @Test
    void shouldValidateAiGeneratedBoundaryCases() {
        for (AiShipmentRequestCases.ShipmentCase scenario : AiShipmentRequestCases.all()) {
            ShipmentRequest request = new ShipmentRequest();
            request.setTrackingNumber(scenario.trackingNumber());
            request.setWeightLbs(scenario.weightLbs());

            boolean valid = validator.validate(request).isEmpty();
            assertEquals(
                    scenario.valid(),
                    valid,
                    () -> "Validation mismatch for tracking=" + scenario.trackingNumber() + ", weight=" + scenario.weightLbs()
            );
        }
    }
}

