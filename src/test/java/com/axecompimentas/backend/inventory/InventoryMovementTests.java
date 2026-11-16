package com.axecompimentas.backend.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.axecompimentas.backend.product.FulfillmentMode;
import com.axecompimentas.backend.product.Product;
import com.axecompimentas.backend.product.ProductRepository;
import com.axecompimentas.backend.product.ProductRequest;
import com.axecompimentas.backend.product.ProductService;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryMovementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private long productId;

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
        Product product = productService.create(new ProductRequest(
                "Molho", "Molho artesanal", new BigDecimal("19.90"),
                null, "molhos", FulfillmentMode.READY_TO_SHIP));
        productId = product.id();
    }

    @Test
    void recordsMovementsAndReplaysAnIdenticalIdempotentRequest() {
        UUID key = UUID.randomUUID();
        StockMovementRequest receipt = new StockMovementRequest(
                StockMovementType.RECEIPT, 10, "Compra recebida", "NF-123", "Operador", key);

        StockMovement first = inventoryService.registerMovement(productId, receipt);
        StockMovement replay = inventoryService.registerMovement(productId, receipt);

        assertEquals(first, replay);
        assertEquals(10, inventoryService.getFinishedStock(productId).quantity());

        StockMovement issue = inventoryService.registerMovement(productId, new StockMovementRequest(
                StockMovementType.ISSUE, 3, "Separação", "PED-12", "Operador", UUID.randomUUID()));
        assertEquals(-3, issue.quantityDelta());
        assertEquals(7, inventoryService.getFinishedStock(productId).quantity());
        assertEquals(2, inventoryService.findMovements(productId).size());
    }

    @Test
    void rejectsNegativeBalancesAndConflictingReuseOfAnIdempotencyKey() {
        UUID key = UUID.randomUUID();
        StockMovementRequest receipt = new StockMovementRequest(
                StockMovementType.RECEIPT, 10, "Compra recebida", null, "Operador", key);
        inventoryService.registerMovement(productId, receipt);

        ResponseStatusException insufficientStock = assertThrows(ResponseStatusException.class,
                () -> inventoryService.registerMovement(productId, new StockMovementRequest(
                        StockMovementType.ISSUE, 11, "Saída", null, "Operador", UUID.randomUUID())));
        assertEquals(HttpStatus.BAD_REQUEST, insufficientStock.getStatusCode());

        ResponseStatusException conflictingKey = assertThrows(ResponseStatusException.class,
                () -> inventoryService.registerMovement(productId, new StockMovementRequest(
                        StockMovementType.RECEIPT, 9, "Outra compra", null, "Operador", key)));
        assertEquals(HttpStatus.CONFLICT, conflictingKey.getStatusCode());
        assertEquals(10, inventoryService.getFinishedStock(productId).quantity());
        assertEquals(1, inventoryService.findMovements(productId).size());
    }

    @Test
    void reversesAMovementOnceAndKeepsTheOriginalRecord() {
        StockMovement receipt = inventoryService.registerMovement(productId, new StockMovementRequest(
                StockMovementType.RECEIPT, 10, "Compra recebida", null,
                "Operador", UUID.randomUUID()));
        StockMovement issue = inventoryService.registerMovement(productId, new StockMovementRequest(
                StockMovementType.ISSUE, 3, "Saída incorreta", "PED-12",
                "Operador", UUID.randomUUID()));
        ReverseStockMovementRequest request = new ReverseStockMovementRequest(
                "Correção da saída", "Supervisor", UUID.randomUUID());

        StockMovement reversal = inventoryService.reverseMovement(productId, issue.id(), request);
        StockMovement replay = inventoryService.reverseMovement(productId, issue.id(), request);

        assertEquals(reversal, replay);
        assertEquals(issue.id(), reversal.reversalOfMovementId());
        assertEquals(10, inventoryService.getFinishedStock(productId).quantity());
        assertEquals(3, inventoryService.findMovements(productId).size());
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        () -> inventoryService.reverseMovement(productId, issue.id(),
                                new ReverseStockMovementRequest(
                                        "Segundo estorno", "Supervisor", UUID.randomUUID())))
                        .getStatusCode());
        assertEquals(receipt.id(), inventoryService.findMovements(productId).get(2).id());
    }

    @Test
    void administrativeHistoryListsAllProductsAndRequiresAuthentication() throws Exception {
        Product other = productService.create(new ProductRequest(
                "Outro", "Outro produto", new BigDecimal("10.00"),
                null, "molhos", FulfillmentMode.READY_TO_SHIP));
        StockMovement first = inventoryService.registerMovement(productId, new StockMovementRequest(
                StockMovementType.RECEIPT, 2, "Primeira", null, "Admin", UUID.randomUUID()));
        StockMovement second = inventoryService.registerMovement(other.id(), new StockMovementRequest(
                StockMovementType.RECEIPT, 3, "Segunda", null, "Admin", UUID.randomUUID()));
        String authorization = "Basic " + Base64.getEncoder()
                .encodeToString("test-admin:test-password".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/admin/inventory/movements"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/inventory/movements").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second.id()))
                .andExpect(jsonPath("$[0].productId").value(other.id()))
                .andExpect(jsonPath("$[1].id").value(first.id()));
    }
}
