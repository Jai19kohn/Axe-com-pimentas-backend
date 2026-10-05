package com.axecompimentas.backend.product;

public record FulfillmentPlan(
        long productId,
        FulfillmentMode fulfillmentMode,
        int requestedQuantity,
        int quantityFromStock,
        int quantityToProduce,
        boolean canBeOrdered) {
}