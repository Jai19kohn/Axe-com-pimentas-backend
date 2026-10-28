package com.axecompimentas.backend.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.axecompimentas.backend.inventory.InventoryRepository;
import com.axecompimentas.backend.inventory.InventoryService;

@SpringBootTest
class ProductServiceTests {

        @Autowired
        private ProductService productService;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private InventoryRepository inventoryRepository;

        @Autowired
        private InventoryService inventoryService;

        @BeforeEach
        void clearProducts() {
                inventoryRepository.deleteAll();
                productRepository.deleteAll();
        }

        @Test
        void createsFindsUpdatesAndDeletesProduct() {
                ProductRequest request = new ProductRequest("Molho de pimenta", "Molho artesanal",
                                new BigDecimal("19.90"),
                                new BigDecimal("24.90"), "molhos", FulfillmentMode.MADE_TO_ORDER);

                Product created = productService.create(request);
                long productId = created.id();
                assertEquals(0, inventoryService.getFinishedStock(productId).quantity());
                assertEquals(HttpStatus.BAD_REQUEST,
                                assertThrows(ResponseStatusException.class,
                                                () -> inventoryService.setFinishedQuantity(productId, -1))
                                                .getStatusCode());
                inventoryService.setFinishedQuantity(productId, 25);

                assertEquals("Molho de pimenta", productService.findById(productId).name());
                assertEquals(25, inventoryService.getFinishedStock(productId).quantity());
                assertEquals(1, productService.findAll().size());

                ProductRequest update = new ProductRequest("Molho extra forte", "Molho artesanal extra forte",
                                new BigDecimal("21.90"), null, "molhos", FulfillmentMode.MADE_TO_ORDER);
                Product updated = productService.update(productId, update);
                assertEquals("Molho extra forte", updated.name());
                assertEquals(new BigDecimal("21.90"), updated.price());
                assertEquals(25, inventoryService.getFinishedStock(productId).quantity());

                inventoryService.setFinishedQuantity(productId, 8);
                assertEquals(8, inventoryService.getFinishedStock(productId).quantity());

                productService.delete(productId);
                assertEquals(0, productService.findAll().size());
                assertEquals(HttpStatus.NOT_FOUND,
                                assertThrows(ResponseStatusException.class, () -> productService.findById(productId))
                                                .getStatusCode());
        }
}