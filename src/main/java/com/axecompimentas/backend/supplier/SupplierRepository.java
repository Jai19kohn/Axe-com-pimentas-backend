package com.axecompimentas.backend.supplier;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {

    List<SupplierEntity> findAllByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
