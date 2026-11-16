package com.axecompimentas.backend.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.axecompimentas.backend.product.ProductRepository;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
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
    public java.util.Map<Long, Integer> findFinishedQuantities(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return java.util.Map.of();
        }
        return inventoryRepository.findAllByProductIdIn(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        FinishedProductInventoryEntity::getProductId,
                        FinishedProductInventoryEntity::getQuantity));
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
        int previousQuantity = inventory.getQuantity();
        inventory.setQuantity(quantity);
        FinishedProductInventoryEntity saved = inventoryRepository.save(inventory);
        if (previousQuantity != quantity) {
            stockMovementRepository.save(new StockMovementEntity(
                    productId, StockMovementType.ADJUSTMENT, quantity - previousQuantity, quantity,
                    "Ajuste legado de saldo sem motivo informado", null, "API legada",
                    Instant.now().truncatedTo(ChronoUnit.MICROS), java.util.UUID.randomUUID()));
        }
        return new FinishedProductStock(productId, saved.getQuantity());
    }

    @Transactional
    public StockMovement registerMovement(long productId, StockMovementRequest request) {
        requireProduct(productId);
        StockMovementEntity previous = stockMovementRepository
                .findByIdempotencyKey(request.idempotencyKey())
                .orElse(null);
        if (previous != null) {
            return replayOrReject(previous, productId, request);
        }

        FinishedProductInventoryEntity inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("Estoque do produto não foi inicializado"));

        previous = stockMovementRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (previous != null) {
            return replayOrReject(previous, productId, request);
        }

        int currentQuantity = inventory.getQuantity();
        int quantityDelta = switch (request.type()) {
            case RECEIPT -> requirePositiveQuantity(request.quantity());
            case ISSUE, DAMAGE -> -requirePositiveQuantity(request.quantity());
            case ADJUSTMENT -> request.quantity() - currentQuantity;
        };
        if (quantityDelta == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O ajuste informado não altera o saldo atual");
        }

        long resultingQuantity = (long) currentQuantity + quantityDelta;
        if (resultingQuantity < 0 || resultingQuantity > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O estoque não pode ser negativo");
        }

        inventory.setQuantity((int) resultingQuantity);
        inventoryRepository.save(inventory);
        StockMovementEntity movement = stockMovementRepository.save(new StockMovementEntity(
                productId, request.type(), quantityDelta, (int) resultingQuantity,
                request.reason().trim(), trimToNull(request.reference()),
                request.responsible().trim(), Instant.now().truncatedTo(ChronoUnit.MICROS),
                request.idempotencyKey()));
        return toMovement(movement);
    }

    @Transactional
    public StockMovement reverseMovement(long productId, long movementId,
            ReverseStockMovementRequest request) {
        requireProduct(productId);
        StockMovementEntity original = stockMovementRepository.findByIdAndProductId(movementId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Movimentação " + movementId + " não encontrada"));

        StockMovementEntity previous = stockMovementRepository
                .findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (previous != null) {
            return replayOrRejectReverse(previous, productId, movementId, request);
        }

        if (stockMovementRepository.findByReversalOfMovementId(movementId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta movimentação já foi estornada");
        }

        FinishedProductInventoryEntity inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new IllegalStateException("Estoque do produto não foi inicializado"));
        previous = stockMovementRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (previous != null) {
            return replayOrRejectReverse(previous, productId, movementId, request);
        }
        if (stockMovementRepository.findByReversalOfMovementId(movementId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta movimentação já foi estornada");
        }
        long quantityDelta = -(long) original.getQuantityDelta();
        long resultingQuantity = (long) inventory.getQuantity() + quantityDelta;
        if (resultingQuantity < 0 || resultingQuantity > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O estorno não pode ser aplicado sem gerar saldo negativo");
        }

        inventory.setQuantity((int) resultingQuantity);
        inventoryRepository.save(inventory);
        StockMovementEntity reversal = stockMovementRepository.save(new StockMovementEntity(
                productId, StockMovementType.ADJUSTMENT, (int) quantityDelta, (int) resultingQuantity,
                request.reason().trim(), "ESTORNO:" + movementId, request.responsible().trim(),
                Instant.now().truncatedTo(ChronoUnit.MICROS), request.idempotencyKey(), movementId));
        return toMovement(reversal);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> findMovements(long productId) {
        requireProduct(productId);
        return stockMovementRepository.findAllByProductIdOrderByOccurredAtDescIdDesc(productId)
                .stream()
                .map(this::toMovement)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovement> findAllMovements() {
        return stockMovementRepository.findAllByOrderByOccurredAtDescIdDesc()
                .stream()
                .map(this::toMovement)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasMovementHistory(long productId) {
        return stockMovementRepository.existsByProductId(productId);
    }

    @Transactional
    public void deleteFinishedStock(long productId) {
        inventoryRepository.findByProductIdForUpdate(productId);
        if (stockMovementRepository.existsByProductId(productId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Produtos com movimentações não podem ser excluídos; desative o produto");
        }
        inventoryRepository.deleteById(productId);
    }

    private StockMovement replayOrReject(StockMovementEntity previous, long productId,
            StockMovementRequest request) {
        boolean sameRequest = previous.getProductId() == productId
                && previous.getType() == request.type()
                && Objects.equals(previous.getReason(), request.reason().trim())
                && Objects.equals(previous.getReference(), trimToNull(request.reference()))
                && Objects.equals(previous.getResponsible(), request.responsible().trim())
                && previous.getReversalOfMovementId() == null
                && requestQuantityMatches(previous, request);
        if (!sameRequest) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave de idempotência já foi usada em outra movimentação");
        }
        return toMovement(previous);
    }

    private StockMovement replayOrRejectReverse(StockMovementEntity previous, long productId,
            long movementId, ReverseStockMovementRequest request) {
        boolean sameRequest = previous.getProductId() == productId
                && Objects.equals(previous.getReversalOfMovementId(), movementId)
                && Objects.equals(previous.getReason(), request.reason().trim())
                && Objects.equals(previous.getResponsible(), request.responsible().trim());
        if (!sameRequest) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave de idempotência já foi usada em outra movimentação");
        }
        return toMovement(previous);
    }

    private boolean requestQuantityMatches(StockMovementEntity previous, StockMovementRequest request) {
        return switch (request.type()) {
            case RECEIPT -> previous.getQuantityDelta() == request.quantity();
            case ISSUE, DAMAGE -> previous.getQuantityDelta() == -request.quantity();
            case ADJUSTMENT -> previous.getResultingQuantity() == request.quantity();
        };
    }

    private int requirePositiveQuantity(int quantity) {
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A quantidade de entrada ou saída deve ser maior que zero");
        }
        return quantity;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private StockMovement toMovement(StockMovementEntity entity) {
        return new StockMovement(entity.getId(), entity.getProductId(), entity.getType(),
                entity.getQuantityDelta(), entity.getResultingQuantity(), entity.getReason(),
                entity.getReference(), entity.getResponsible(), entity.getOccurredAt(),
                entity.getIdempotencyKey(), entity.getReversalOfMovementId());
    }

    private void requireProduct(long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto " + productId + " não encontrado");
        }
    }
}