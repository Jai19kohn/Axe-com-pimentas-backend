package com.axecompimentas.backend.inventory;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_movements")
public class StockMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType type;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "resulting_quantity", nullable = false)
    private int resultingQuantity;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(length = 120)
    private String reference;

    @Column(nullable = false, length = 120)
    private String responsible;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "reversal_of_movement_id", unique = true)
    private Long reversalOfMovementId;

    protected StockMovementEntity() {
    }

    public StockMovementEntity(long productId, StockMovementType type, int quantityDelta,
            int resultingQuantity, String reason, String reference, String responsible,
            Instant occurredAt, UUID idempotencyKey) {
        this(productId, type, quantityDelta, resultingQuantity, reason, reference,
                responsible, occurredAt, idempotencyKey, null);
    }

    public StockMovementEntity(long productId, StockMovementType type, int quantityDelta,
            int resultingQuantity, String reason, String reference, String responsible,
            Instant occurredAt, UUID idempotencyKey, Long reversalOfMovementId) {
        this.productId = productId;
        this.type = type;
        this.quantityDelta = quantityDelta;
        this.resultingQuantity = resultingQuantity;
        this.reason = reason;
        this.reference = reference;
        this.responsible = responsible;
        this.occurredAt = occurredAt;
        this.idempotencyKey = idempotencyKey;
        this.reversalOfMovementId = reversalOfMovementId;
    }

    public Long getId() {
        return id;
    }

    public long getProductId() {
        return productId;
    }

    public StockMovementType getType() {
        return type;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public int getResultingQuantity() {
        return resultingQuantity;
    }

    public String getReason() {
        return reason;
    }

    public String getReference() {
        return reference;
    }

    public String getResponsible() {
        return responsible;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getReversalOfMovementId() {
        return reversalOfMovementId;
    }
}
