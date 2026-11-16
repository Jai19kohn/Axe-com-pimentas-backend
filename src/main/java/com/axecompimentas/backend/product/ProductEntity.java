package com.axecompimentas.backend.product;

import java.math.BigDecimal;

import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.axecompimentas.backend.supplier.SupplierEntity;

@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"),
        @UniqueConstraint(name = "uk_products_barcode", columnNames = "barcode")
})
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, length = 80)
    private String categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FulfillmentMode fulfillmentMode;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(length = 80)
    private String barcode;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private SupplierEntity supplier;

    @Column(name = "purchase_cost", precision = 10, scale = 2)
    private BigDecimal purchaseCost;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "variation_color", length = 80)
    private String variationColor;

    @Column(name = "variation_size", length = 80)
    private String variationSize;

    @Column(name = "variation_model", length = 80)
    private String variationModel;

    @Column(name = "production_lead_time_days")
    private Integer productionLeadTimeDays;

    @Column(name = "customization_description", length = 2000)
    private String customizationDescription;

    protected ProductEntity() {
    }

    public ProductEntity(String name, String description, BigDecimal price, BigDecimal originalPrice,
            String categoryId, FulfillmentMode fulfillmentMode) {
        this(name, description, price, originalPrice, categoryId, fulfillmentMode, null, null,
                null, null, true, null, null, null);
    }

    public ProductEntity(String name, String description, BigDecimal price, BigDecimal originalPrice,
            String categoryId, FulfillmentMode fulfillmentMode, String sku, String barcode,
            SupplierEntity supplier, BigDecimal purchaseCost, boolean active,
            String variationColor, String variationSize, String variationModel) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.categoryId = categoryId;
        this.fulfillmentMode = fulfillmentMode;
        this.sku = sku;
        this.barcode = barcode;
        this.supplier = supplier;
        this.purchaseCost = purchaseCost;
        this.active = active;
        this.variationColor = variationColor;
        this.variationSize = variationSize;
        this.variationModel = variationModel;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public FulfillmentMode getFulfillmentMode() {
        return fulfillmentMode;
    }

    public void setFulfillmentMode(FulfillmentMode fulfillmentMode) {
        this.fulfillmentMode = fulfillmentMode;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public SupplierEntity getSupplier() {
        return supplier;
    }

    public void setSupplier(SupplierEntity supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(BigDecimal purchaseCost) {
        this.purchaseCost = purchaseCost;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getVariationColor() {
        return variationColor;
    }

    public void setVariationColor(String variationColor) {
        this.variationColor = variationColor;
    }

    public String getVariationSize() {
        return variationSize;
    }

    public void setVariationSize(String variationSize) {
        this.variationSize = variationSize;
    }

    public Integer getProductionLeadTimeDays() {
        return productionLeadTimeDays;
    }

    public void setProductionLeadTimeDays(Integer productionLeadTimeDays) {
        this.productionLeadTimeDays = productionLeadTimeDays;
    }

    public String getCustomizationDescription() {
        return customizationDescription;
    }

    public void setCustomizationDescription(String customizationDescription) {
        this.customizationDescription = customizationDescription;
    }

    public String getVariationModel() {
        return variationModel;
    }

    public void setVariationModel(String variationModel) {
        this.variationModel = variationModel;
    }
}