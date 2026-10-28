package com.axecompimentas.backend.inventory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}