package com.axecompimentas.backend.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    @Query("select p.sku from ProductEntity p where length(p.sku) = 6")
    List<String> findSixCharacterSkus();

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByBarcodeIgnoreCase(String barcode);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, long id);

    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, long id);
}