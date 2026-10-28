package com.axecompimentas.backend.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.axecompimentas.backend.product.ProductRepository;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void initializeFinishedStock(long productId) {
        inventoryRepository.save(new FinishedProductInventoryEntity(productId, 0));
    }

    @Transactional(readOnly = true)
    public int findFinishedQuantity(long productId) {
        return inventoryRepository.findById(productId)
                .map(FinishedProductInventoryEntity::getQuantity)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public FinishedProductStock getFinishedStock(long productId) {
        requireProduct(productId);
        int quantity = inventoryRepository.findById(productId)
            .map(FinishedProductInventoryEntity::getQuantity)
            .orElse(0);
        return new FinishedProductStock(productId, quantity);
    }

    @Transactional
    public FinishedProductStock setFinishedQuantity(long productId, int quantity) {
        if (quantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O estoque não pode ser negativo");
        }
        requireProduct(productId);
        FinishedProductInventoryEntity inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseGet(() -> inventoryRepository.save(new FinishedProductInventoryEntity(productId, 0)));
        inventory.setQuantity(quantity);
        FinishedProductInventoryEntity saved = inventoryRepository.save(inventory);
        return new FinishedProductStock(productId, saved.getQuantity());
    }

    @Transactional
    public void deleteFinishedStock(long productId) {
        inventoryRepository.deleteById(productId);
    }

    private void requireProduct(long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto " + productId + " não encontrado");
        }
    }
}