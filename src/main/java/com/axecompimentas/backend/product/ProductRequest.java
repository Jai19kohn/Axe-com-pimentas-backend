package com.axecompimentas.backend.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
                @NotBlank @Size(max = 120) String name,
                @NotBlank @Size(max = 2000) String description,
                @NotNull @DecimalMin("0.01") BigDecimal price,
                @DecimalMin("0.01") BigDecimal originalPrice,
                @NotBlank @Size(max = 80) String categoryId,
                @NotNull FulfillmentMode fulfillmentMode,
                @Size(max = 80) String sku,
                @Size(max = 80) String barcode,
                Long supplierId,
                @DecimalMin("0.00") BigDecimal purchaseCost,
                Boolean active,
                @Size(max = 80) String variationColor,
                @Size(max = 80) String variationSize,
                @Size(max = 80) String variationModel,
                @Min(0) Integer initialStock,
                @Min(1) @Max(365) Integer productionLeadTimeDays,
                @Size(max = 2000) String customizationDescription) {

        public ProductRequest(String name, String description, BigDecimal price, BigDecimal originalPrice,
                String categoryId, FulfillmentMode fulfillmentMode, String sku, String barcode,
                Long supplierId, BigDecimal purchaseCost, Boolean active, String variationColor,
                String variationSize, String variationModel, Integer initialStock) {
                this(name, description, price, originalPrice, categoryId, fulfillmentMode, sku, barcode,
                        supplierId, purchaseCost, active, variationColor, variationSize, variationModel,
                        initialStock, null, null);
        }

        public ProductRequest(String name, String description, BigDecimal price, BigDecimal originalPrice,
                String categoryId, FulfillmentMode fulfillmentMode) {
                this(name, description, price, originalPrice, categoryId, fulfillmentMode,
                        null, null, null, null, null, null, null, null, null, null, null);
        }
}