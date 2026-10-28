package com.axecompimentas.backend.product;

import java.util.List;

import com.axecompimentas.backend.inventory.InventoryService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

        private final ProductRepository productRepository;
        private final InventoryService inventoryService;

        public ProductService(ProductRepository productRepository, InventoryService inventoryService) {
                this.productRepository = productRepository;
                this.inventoryService = inventoryService;
        }

        /**
         * @return
         **/
        @Transactional(readOnly = true)
        public List<Product> findAll() {
                return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                                .map(this::toProduct)
                                .toList();
        }

        @Transactional(readOnly = true)
        public Product findById(long id) {
                return productRepository.findById(id)
                                .map(this::toProduct)
                                .orElseThrow(() -> notFound(id));
        }

        @Transactional
        public Product create(ProductRequest product) {
                ProductEntity entity = new ProductEntity(product.name(), product.description(), product.price(),
                                product.originalPrice(), product.categoryId(), product.fulfillmentMode());
                ProductEntity saved = productRepository.save(entity);
                inventoryService.initializeFinishedStock(saved.getId());
                return toProduct(saved);
        }

        @Transactional
        public Product update(long id, ProductRequest product) {
                ProductEntity entity = productRepository.findById(id)
                                .orElseThrow(() -> notFound(id));
                entity.setName(product.name());
                entity.setDescription(product.description());
                entity.setPrice(product.price());
                entity.setOriginalPrice(product.originalPrice());
                entity.setCategoryId(product.categoryId());
                entity.setFulfillmentMode(product.fulfillmentMode());
                ProductEntity saved = productRepository.save(entity);
                return toProduct(saved);
        }

        @Transactional(readOnly = true)
        public FulfillmentPlan planFulfillment(long id, int requestedQuantity) {
                if (requestedQuantity < 1) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "A quantidade solicitada deve ser maior que zero");
                }

                ProductEntity product = productRepository.findById(id)
                                .orElseThrow(() -> notFound(id));
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
                inventoryService.deleteFinishedStock(id);
                productRepository.deleteById(id);
        }

        private Product toProduct(ProductEntity entity) {
                return new Product(entity.getId(), entity.getName(), entity.getDescription(), entity.getPrice(),
                                entity.getOriginalPrice(), entity.getCategoryId(), entity.getFulfillmentMode());
        }

        private ResponseStatusException notFound(long id) {
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto " + id + " não encontrado");
        }
}