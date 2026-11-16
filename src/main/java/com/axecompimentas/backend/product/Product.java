package com.axecompimentas.backend.product;

import java.math.BigDecimal;

public record Product(
                long id,
                String name,
                String description,
                BigDecimal price,
                BigDecimal originalPrice,
                String categoryId,
                FulfillmentMode fulfillmentMode,
                String sku,
                String barcode,
                String supplier,
                Long supplierId,
                BigDecimal purchaseCost,
                boolean active,
                String variationColor,
                String variationSize,
                String variationModel,
                Integer productionLeadTimeDays,
                String customizationDescription) {
}