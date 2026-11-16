package com.axecompimentas.backend.inventory;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReverseStockMovementRequest(
        @NotBlank @Size(max = 500) String reason,
        @NotBlank @Size(max = 120) String responsible,
        @NotNull UUID idempotencyKey) {
}
