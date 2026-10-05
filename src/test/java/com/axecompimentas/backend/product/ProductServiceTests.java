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

@SpringBootTest
class ProductServiceTests {

        @Autowired
        private ProductService productService;

        @Autowired
        private ProductRepository productRepository;

        @BeforeEach
        void clearProducts() {
                productRepository.deleteAll();
        }

        @Test
        void createsFindsUpdatesAndDeletesProduct() {
                ProductRequest request = new ProductRequest("Molho de pimenta", "Molho artesanal",
                                new BigDecimal("19.90"),
                                new BigDecimal("24.90"), "molhos", 25, FulfillmentMode.MADE_TO_ORDER);

                Product created = productService.create(request);

                assertEquals("Molho de pimenta", productService.findById(created.id()).name());
                assertEquals(25, created.stock());
                assertEquals(25, productService.findById(created.id()).stock());
                assertEquals(1, productService.findAll().size());

                ProductRequest update = new ProductRequest("Molho extra forte", "Molho artesanal extra forte",
                                new BigDecimal("21.90"), null, "molhos", 8, FulfillmentMode.MADE_TO_ORDER);
                Product updated = productService.update(created.id(), update);
                assertEquals("Molho extra forte", updated.name());
                assertEquals(new BigDecimal("21.90"), updated.price());
                assertEquals(8, updated.stock());

                productService.delete(created.id());
                assertEquals(0, productService.findAll().size());
                assertEquals(HttpStatus.NOT_FOUND,
                                assertThrows(ResponseStatusException.class, () -> productService.findById(created.id()))
                                                .getStatusCode());
        }
}