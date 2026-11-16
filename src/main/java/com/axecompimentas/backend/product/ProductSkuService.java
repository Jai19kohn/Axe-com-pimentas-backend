package com.axecompimentas.backend.product;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductSkuService {

    private final ProductRepository productRepository;

    public ProductSkuService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** Próximo SKU livre (maior SKU numérico existente + 1), sem consumir nenhum contador. */
    public String reserveNextSku() {
        long next = productRepository.findSixCharacterSkus().stream()
                .filter(sku -> sku.chars().allMatch(Character::isDigit))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0) + 1;
        if (next > 999999) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Limite de SKUs de seis dígitos atingido");
        }
        return "%06d".formatted(next);
    }
}
