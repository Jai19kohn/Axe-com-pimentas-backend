package com.axecompimentas.backend.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetFinishedProductStockRequest(
        @NotNull @Min(0) Integer quantity) {
}