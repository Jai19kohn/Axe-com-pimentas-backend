package com.axecompimentas.backend.product;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<AdminProduct> findAll() {
        return productService.findAllForAdmin();
    }

    @GetMapping("/{id}")
    public AdminProduct findById(@PathVariable long id) {
        return productService.findByIdForAdmin(id);
    }
}
