package com.axecompimentas.backend.inventory;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockMovementRequest(
        @NotNull StockMovementType type,
        @NotNull @Min(0) Integer quantity,
        @NotBlank @Size(max = 500) String reason,
        @Size(max = 120) String reference,
        @NotBlank @Size(max = 120) String responsible,
        @NotNull UUID idempotencyKey) {
}
