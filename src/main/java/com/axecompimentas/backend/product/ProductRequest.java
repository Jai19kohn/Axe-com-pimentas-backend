package com.axecompimentas.backend.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
                @NotBlank @Size(max = 120) String name,
                @NotBlank @Size(max = 2000) String description,
                @NotNull @DecimalMin("0.01") BigDecimal price,
                @DecimalMin("0.01") BigDecimal originalPrice,
                @NotBlank @Size(max = 80) String categoryId,
                @NotNull FulfillmentMode fulfillmentMode) {
}