package com.axecompimentas.backend.inventory;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory/products/{productId}/finished-stock")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public FinishedProductStock getFinishedStock(@PathVariable long productId) {
        return inventoryService.getFinishedStock(productId);
    }

    @PutMapping
    public FinishedProductStock setFinishedStock(@PathVariable long productId,
            @Valid @RequestBody SetFinishedProductStockRequest request) {
        return inventoryService.setFinishedQuantity(productId, request.quantity());
    }

    @GetMapping("/movements")
    public List<StockMovement> findMovements(@PathVariable long productId) {
        return inventoryService.findMovements(productId);
    }

    @PostMapping("/movements")
    public StockMovement registerMovement(@PathVariable long productId,
            @Valid @RequestBody StockMovementRequest request) {
        return inventoryService.registerMovement(productId, request);
    }

    @PostMapping("/movements/{movementId}/reversal")
    public StockMovement reverseMovement(@PathVariable long productId, @PathVariable long movementId,
            @Valid @RequestBody ReverseStockMovementRequest request) {
        return inventoryService.reverseMovement(productId, movementId, request);
    }
}