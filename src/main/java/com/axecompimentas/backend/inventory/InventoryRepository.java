package com.axecompimentas.backend.inventory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface InventoryRepository extends JpaRepository<FinishedProductInventoryEntity, Long> {

    List<FinishedProductInventoryEntity> findAllByProductIdIn(Collection<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from FinishedProductInventoryEntity inventory where inventory.productId = :productId")
    Optional<FinishedProductInventoryEntity> findByProductIdForUpdate(@Param("productId") long productId);
}