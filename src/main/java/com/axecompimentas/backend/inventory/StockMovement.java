package com.axecompimentas.backend.inventory;

import java.time.Instant;
import java.util.UUID;

public record StockMovement(
        long id,
        long productId,
        StockMovementType type,
        int quantityDelta,
        int resultingQuantity,
        String reason,
        String reference,
        String responsible,
        Instant occurredAt,
        UUID idempotencyKey,
        Long reversalOfMovementId) {
}
