package com.axecompimentas.backend.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import com.axecompimentas.backend.inventory.StockMovementRepository;

@SpringBootTest
class ProductServiceTests {

        @Autowired
        private ProductService productService;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private InventoryRepository inventoryRepository;

        @Autowired
        private StockMovementRepository stockMovementRepository;

        @Autowired
        private InventoryService inventoryService;

        @BeforeEach
        void clearProducts() {
                stockMovementRepository.deleteAll();
                inventoryRepository.deleteAll();
                productRepository.deleteAll();
        }

        @Test
        void createsFindsUpdatesAndDeletesProduct() {
                ProductRequest request = new ProductRequest("Molho de pimenta", "Molho artesanal",
                                new BigDecimal("19.90"),
                                new BigDecimal("24.90"), "molhos", FulfillmentMode.MADE_TO_ORDER,
                                null, null, null, null, null, null, null, null, null, 5, null);

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
                                new BigDecimal("21.90"), null, "molhos", FulfillmentMode.MADE_TO_ORDER,
                                null, null, null, null, null, null, null, null, null, 5, null);
                Product updated = productService.update(productId, update);
                assertEquals("Molho extra forte", updated.name());
                assertEquals(new BigDecimal("21.90"), updated.price());
                assertEquals(25, inventoryService.getFinishedStock(productId).quantity());

                inventoryService.setFinishedQuantity(productId, 8);
                assertEquals(8, inventoryService.getFinishedStock(productId).quantity());

                assertEquals(HttpStatus.CONFLICT,
                                assertThrows(ResponseStatusException.class,
                                                () -> productService.delete(productId))
                                                .getStatusCode());

                ProductRequest deactivate = new ProductRequest(
                                "Molho extra forte", "Molho artesanal extra forte", new BigDecimal("21.90"),
                                null, "molhos", FulfillmentMode.MADE_TO_ORDER,
                                null, null, null, null, false, null, null, null, null, 5, null);
                productService.update(productId, deactivate);
                assertEquals(0, productService.findAll().size());
                assertEquals(HttpStatus.NOT_FOUND,
                                assertThrows(ResponseStatusException.class, () -> productService.findById(productId))
                                                .getStatusCode());
        }

        @Test
        void requiresLeadTimeForMadeToOrderAndOnlyKeepsCustomizationForCustom() {
                ProductRequest withoutLeadTime = new ProductRequest("Molho", "Descrição", new BigDecimal("10.00"),
                                null, "molhos", FulfillmentMode.CUSTOM);
                assertEquals(HttpStatus.BAD_REQUEST,
                                assertThrows(ResponseStatusException.class,
                                                () -> productService.create(withoutLeadTime)).getStatusCode());

                Product custom = productService.create(new ProductRequest("Molho", "Descrição",
                                new BigDecimal("10.00"), null, "molhos", FulfillmentMode.CUSTOM,
                                null, null, null, null, null, null, null, null, null, 7, "Escolha as pimentas"));
                assertEquals(7, custom.productionLeadTimeDays());
                assertEquals("Escolha as pimentas", custom.customizationDescription());

                Product madeToOrder = productService.update(custom.id(), new ProductRequest("Molho", "Descrição",
                                new BigDecimal("10.00"), null, "molhos", FulfillmentMode.MADE_TO_ORDER,
                                null, null, null, null, null, null, null, null, null, 3, "Ignorada"));
                assertEquals(3, madeToOrder.productionLeadTimeDays());
                assertEquals(null, madeToOrder.customizationDescription());

                Product ready = productService.update(custom.id(), new ProductRequest("Molho", "Descrição",
                                new BigDecimal("10.00"), null, "molhos", FulfillmentMode.READY_TO_SHIP,
                                null, null, null, null, null, null, null, null, null, 3, "Ignorada"));
                assertEquals(null, ready.productionLeadTimeDays());
        }

        @Test
        void deletesProductWithoutMovementHistory() {
                Product created = productService.create(new ProductRequest(
                                "Molho", "Descrição", new BigDecimal("10.00"), null,
                                "molhos", FulfillmentMode.READY_TO_SHIP));

                productService.delete(created.id());

                assertEquals(HttpStatus.NOT_FOUND,
                                assertThrows(ResponseStatusException.class,
                                                () -> productService.findById(created.id()))
                                                .getStatusCode());
        }

        @Test
        void generatesUniqueSkuAndKeepsInactiveProductsOutOfTheCatalog() {
                ProductRequest request = new ProductRequest(
                                "Vela", "Vela artesanal", new BigDecimal("25.00"), null, "velas",
                                FulfillmentMode.READY_TO_SHIP, "  123456  ", "  123abc  ",
                                null, new BigDecimal("8.50"), false,
                                "Azul", "P", "Clássico", 5);
                Product product = productService.create(request);

                assertEquals(5, productService.findByIdForAdmin(product.id()).stockQuantity());
                assertEquals(1, productService.findAllForAdmin().size());
                assertEquals("123456", product.sku());
                assertEquals("123ABC", product.barcode());
                assertEquals(false, product.active());
                assertEquals(0, productService.findAll().size());
                assertEquals(HttpStatus.NOT_FOUND,
                                assertThrows(ResponseStatusException.class,
                                                () -> productService.findById(product.id()))
                                                .getStatusCode());

                ProductRequest duplicateSku = new ProductRequest(
                                "Outra vela", "Descrição", new BigDecimal("20.00"), null, "velas",
                                FulfillmentMode.READY_TO_SHIP, "123456", null,
                                null, null, true, "Vermelha", "M", "Clássico", null);
                assertEquals(HttpStatus.CONFLICT,
                                assertThrows(ResponseStatusException.class,
                                                () -> productService.create(duplicateSku))
                                                .getStatusCode());
        }

        @Test
        void generatesSixDigitSkuWhenNoSkuWasProvided() {
                Product product = productService.create(new ProductRequest(
                                "Molho", "Descrição", new BigDecimal("10.00"), null,
                                "molhos", FulfillmentMode.READY_TO_SHIP));

                assertEquals(true, product.sku().matches("\\d{6}"));
                String nextSku = productService.reserveNextSku();
                assertEquals(true, nextSku.matches("\\d{6}"));
                assertNotEquals(product.sku(), nextSku);
        }
}