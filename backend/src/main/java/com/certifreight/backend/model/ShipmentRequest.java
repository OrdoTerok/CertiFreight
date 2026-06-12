package com.certifreight.backend.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShipmentRequest {

    @NotBlank(message = "Tracking reference key code cannot be blank")
    @Pattern(regexp = "^CFT-\\d{6}$", message = "Tracking number must match enterprise standard format: CFT-123456")
    private String trackingNumber;

    @NotNull(message = "Cargo weight field is mandatory")
    @Min(value = 1, message = "Cargo weight must be a positive integer value greater than 0 lbs")
    private BigDecimal weightLbs;
}