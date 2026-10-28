package com.axecompimentas.backend.product;

import java.math.BigDecimal;

public record Product(
                long id,
                String name,
                String description,
                BigDecimal price,
                BigDecimal originalPrice,
                String categoryId,
                FulfillmentMode fulfillmentMode) {
}