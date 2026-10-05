package com.axecompimentas.backend.product;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

        private final ProductRepository productRepository;

        public ProductService(ProductRepository productRepository) {
                this.productRepository = productRepository;
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

        public Product create(ProductRequest product) {
                ProductEntity entity = new ProductEntity(product.name(), product.description(), product.price(),
                                product.originalPrice(), product.categoryId(), product.stock(),
                                product.fulfillmentMode());
                return toProduct(productRepository.save(entity));
        }

        public Product update(long id, ProductRequest product) {
                ProductEntity entity = productRepository.findById(id)
                                .orElseThrow(() -> notFound(id));
                entity.setName(product.name());
                entity.setDescription(product.description());
                entity.setPrice(product.price());
                entity.setOriginalPrice(product.originalPrice());
                entity.setCategoryId(product.categoryId());
                entity.setStock(product.stock());
                entity.setFulfillmentMode(product.fulfillmentMode());
                return toProduct(productRepository.save(entity));
        }

        @Transactional(readOnly = true)
        public FulfillmentPlan planFulfillment(long id, int requestedQuantity) {
                if (requestedQuantity < 1) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "A quantidade solicitada deve ser maior que zero");
                }

                ProductEntity product = productRepository.findById(id)
                                .orElseThrow(() -> notFound(id));
                int quantityFromStock;
                int quantityToProduce;
                boolean canBeOrdered = true;

                switch (product.getFulfillmentMode()) {
                        case READY_TO_SHIP -> {
                                canBeOrdered = product.getStock() >= requestedQuantity;
                                quantityFromStock = canBeOrdered ? requestedQuantity : 0;
                                quantityToProduce = 0;
                        }
                        case MADE_TO_ORDER -> {
                                quantityFromStock = Math.min(product.getStock(), requestedQuantity);
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

        public void delete(long id) {
                if (!productRepository.existsById(id)) {
                        throw notFound(id);
                }
                productRepository.deleteById(id);
        }

        private Product toProduct(ProductEntity entity) {
                return new Product(entity.getId(), entity.getName(), entity.getDescription(), entity.getPrice(),
                                entity.getOriginalPrice(), entity.getCategoryId(), entity.getStock(),
                                entity.getFulfillmentMode());
        }

        private ResponseStatusException notFound(long id) {
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto " + id + " não encontrado");
        }
}