package com.axecompimentas.backend.inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovementEntity, Long> {

    Optional<StockMovementEntity> findByIdempotencyKey(UUID idempotencyKey);

    List<StockMovementEntity> findAllByProductIdOrderByOccurredAtDescIdDesc(long productId);

    List<StockMovementEntity> findAllByOrderByOccurredAtDescIdDesc();

    boolean existsByProductId(long productId);

    Optional<StockMovementEntity> findByIdAndProductId(long id, long productId);

    Optional<StockMovementEntity> findByReversalOfMovementId(long movementId);
}
