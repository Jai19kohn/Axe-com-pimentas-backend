package com.axecompimentas.backend.product;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.axecompimentas.backend.inventory.InventoryService;
import com.axecompimentas.backend.inventory.StockMovementRequest;
import com.axecompimentas.backend.inventory.StockMovementType;
import com.axecompimentas.backend.supplier.SupplierEntity;
import com.axecompimentas.backend.supplier.SupplierRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

        private final ProductRepository productRepository;
        private final InventoryService inventoryService;
        private final ProductSkuService productSkuService;
        private final SupplierRepository supplierRepository;

        public ProductService(ProductRepository productRepository, InventoryService inventoryService,
                ProductSkuService productSkuService, SupplierRepository supplierRepository) {
                this.productRepository = productRepository;
                this.inventoryService = inventoryService;
                this.productSkuService = productSkuService;
                this.supplierRepository = supplierRepository;
        }

        /**
         * @return
         **/
        @Transactional(readOnly = true)
        public List<Product> findAll() {
                return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                                .filter(ProductEntity::isActive)
                                .map(this::toProduct)
                                .toList();
        }

        @Transactional(readOnly = true)
        public Product findById(long id) {
                return productRepository.findById(id)
                                .filter(ProductEntity::isActive)
                                .map(this::toProduct)
                                .orElseThrow(() -> notFound(id));
        }

        @Transactional(readOnly = true)
        public List<AdminProduct> findAllForAdmin() {
                List<ProductEntity> entities = productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
                Map<Long, Integer> stock = inventoryService.findFinishedQuantities(
                                entities.stream().map(ProductEntity::getId).toList());
                return entities.stream()
                                .map(entity -> toAdminProduct(entity, stock.getOrDefault(entity.getId(), 0)))
                                .toList();
        }

        @Transactional(readOnly = true)
        public AdminProduct findByIdForAdmin(long id) {
                ProductEntity entity = productRepository.findById(id).orElseThrow(() -> notFound(id));
                return toAdminProduct(entity, inventoryService.findFinishedQuantity(id));
        }

        private AdminProduct toAdminProduct(ProductEntity entity, int stockQuantity) {
                Product p = toProduct(entity);
                return new AdminProduct(p.id(), p.name(), p.description(), p.price(), p.originalPrice(),
                                p.categoryId(), p.fulfillmentMode(), p.sku(), p.barcode(), p.supplier(),
                                p.supplierId(), p.purchaseCost(), p.active(), p.variationColor(),
                                p.variationSize(), p.variationModel(), p.productionLeadTimeDays(),
                                p.customizationDescription(), stockQuantity);
        }

        @Transactional
        public Product create(ProductRequest product) {
                String sku = normalizeSku(product.sku());
                if (sku == null) {
                        sku = productSkuService.reserveNextSku();
                }
                validateSku(sku);
                ensureSkuAvailable(sku, null);
                String barcode = normalizeBarcode(product.barcode());
                ensureBarcodeAvailable(barcode, null);
                SupplierEntity supplier = findSupplier(product.supplierId());
                ProductEntity entity = new ProductEntity(product.name(), product.description(), product.price(),
                                product.originalPrice(), product.categoryId(), product.fulfillmentMode(),
                                sku, barcode, supplier, product.purchaseCost(),
                                product.active() == null || product.active(),
                                trimToNull(product.variationColor()), trimToNull(product.variationSize()),
                                trimToNull(product.variationModel()));
                applyProduction(entity, product);
                ProductEntity saved = productRepository.save(entity);
                inventoryService.initializeFinishedStock(saved.getId());
                if (product.initialStock() != null && product.initialStock() > 0) {
                        inventoryService.registerMovement(saved.getId(), new StockMovementRequest(
                                        StockMovementType.RECEIPT, product.initialStock(), "Estoque inicial",
                                        null, "Cadastro de produto", UUID.randomUUID()));
                }
                return toProduct(saved);
        }

        @Transactional
        public Product update(long id, ProductRequest product) {
                ProductEntity entity = productRepository.findById(id)
                                .orElseThrow(() -> notFound(id));
                String sku = normalizeSku(product.sku());
                if (sku != null) {
                        validateSku(sku);
                        ensureSkuAvailable(sku, id);
                        entity.setSku(sku);
                }
                String barcode = normalizeBarcode(product.barcode());
                ensureBarcodeAvailable(barcode, id);
                SupplierEntity supplier = findSupplier(product.supplierId());
                entity.setName(product.name());
                entity.setDescription(product.description());
                entity.setPrice(product.price());
                entity.setOriginalPrice(product.originalPrice());
                entity.setCategoryId(product.categoryId());
                entity.setFulfillmentMode(product.fulfillmentMode());
                entity.setBarcode(barcode);
                entity.setSupplier(supplier);
                entity.setPurchaseCost(product.purchaseCost());
                entity.setActive(product.active() == null ? entity.isActive() : product.active());
                entity.setVariationColor(trimToNull(product.variationColor()));
                entity.setVariationSize(trimToNull(product.variationSize()));
                entity.setVariationModel(trimToNull(product.variationModel()));
                applyProduction(entity, product);
                ProductEntity saved = productRepository.save(entity);
                return toProduct(saved);
        }

        private void applyProduction(ProductEntity entity, ProductRequest request) {
                FulfillmentMode mode = request.fulfillmentMode();
                boolean madeToOrder = mode == FulfillmentMode.MADE_TO_ORDER || mode == FulfillmentMode.CUSTOM;
                if (madeToOrder && request.productionLeadTimeDays() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Informe o prazo de produção para produtos sob encomenda ou personalizados");
                }
                entity.setProductionLeadTimeDays(madeToOrder ? request.productionLeadTimeDays() : null);
                entity.setCustomizationDescription(
                                mode == FulfillmentMode.CUSTOM ? trimToNull(request.customizationDescription()) : null);
        }

        @Transactional(readOnly = true)
        public FulfillmentPlan planFulfillment(long id, int requestedQuantity) {
                if (requestedQuantity < 1) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "A quantidade solicitada deve ser maior que zero");
                }

                ProductEntity product = productRepository.findById(id)
                                .orElseThrow(() -> notFound(id));
                if (!product.isActive()) {
                        throw notFound(id);
                }
                int finishedStock = inventoryService.findFinishedQuantity(id);
                int quantityFromStock;
                int quantityToProduce;
                boolean canBeOrdered = true;

                switch (product.getFulfillmentMode()) {
                        case READY_TO_SHIP -> {
                                canBeOrdered = finishedStock >= requestedQuantity;
                                quantityFromStock = canBeOrdered ? requestedQuantity : 0;
                                quantityToProduce = 0;
                        }
                        case MADE_TO_ORDER -> {
                                quantityFromStock = Math.min(finishedStock, requestedQuantity);
                                quantityToProduce = requestedQuantity - quantityFromStock;
                        }
                        case CUSTOM -> {
                                quantityFromStock = 0;
                                quantityToProduce = requestedQuantity;
                        }
                        default -> throw new IllegalStateException("Modalidade de atendimento não suportada");
                }

                return new FulfillmentPlan(id, product.getFulfillmentMode(), requestedQuantity,
                                quantityFromStock, quantityToProduce, canBeOrdered);
        }

        @Transactional
        public void delete(long id) {
                if (!productRepository.existsById(id)) {
                        throw notFound(id);
                }
                if (inventoryService.hasMovementHistory(id)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Produtos com movimentações não podem ser excluídos; desative o produto");
                }
                inventoryService.deleteFinishedStock(id);
                productRepository.deleteById(id);
        }

        private Product toProduct(ProductEntity entity) {
                return new Product(entity.getId(), entity.getName(), entity.getDescription(), entity.getPrice(),
                                entity.getOriginalPrice(), entity.getCategoryId(), entity.getFulfillmentMode(),
                                entity.getSku(), entity.getBarcode(),
                                entity.getSupplier() == null ? null : entity.getSupplier().getName(),
                                entity.getSupplier() == null ? null : entity.getSupplier().getId(),
                                entity.getPurchaseCost(), entity.isActive(),
                                entity.getVariationColor(), entity.getVariationSize(), entity.getVariationModel(),
                                entity.getProductionLeadTimeDays(), entity.getCustomizationDescription());
        }

        private String normalizeSku(String sku) {
                String value = trimToNull(sku);
                return value == null ? null : value.toUpperCase(Locale.ROOT);
        }

        private String normalizeBarcode(String barcode) {
                String value = trimToNull(barcode);
                return value == null ? null : value.toUpperCase(Locale.ROOT);
        }

        @Transactional
        public String reserveNextSku() {
                return productSkuService.reserveNextSku();
        }

        private void validateSku(String sku) {
                if (!sku.matches("\\d{6}")) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "O SKU deve conter exatamente seis dígitos");
                }
        }

        private SupplierEntity findSupplier(Long supplierId) {
                if (supplierId == null) {
                        return null;
                }
                return supplierRepository.findById(supplierId)
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Fornecedor não encontrado"));
        }

        private String trimToNull(String value) {
                return value == null || value.isBlank() ? null : value.trim();
        }

        private void ensureSkuAvailable(String sku, Long currentProductId) {
                boolean exists = currentProductId == null
                                ? productRepository.existsBySkuIgnoreCase(sku)
                                : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, currentProductId);
                if (exists) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU já cadastrado");
                }
        }

        private void ensureBarcodeAvailable(String barcode, Long currentProductId) {
                if (barcode == null) {
                        return;
                }
                boolean exists = currentProductId == null
                                ? productRepository.existsByBarcodeIgnoreCase(barcode)
                                : productRepository.existsByBarcodeIgnoreCaseAndIdNot(barcode, currentProductId);
                if (exists) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Código de barras já cadastrado");
                }
        }

        private ResponseStatusException notFound(long id) {
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto " + id + " não encontrado");
        }
}