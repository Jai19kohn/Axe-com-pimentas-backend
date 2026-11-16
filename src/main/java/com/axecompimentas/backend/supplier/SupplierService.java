package com.axecompimentas.backend.supplier;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        return supplierRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toSupplier)
                .toList();
    }

    @Transactional
    public Supplier create(SupplierRequest request) {
        String name = request.name().trim();
        if (supplierRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fornecedor já cadastrado");
        }
        return toSupplier(supplierRepository.save(new SupplierEntity(name)));
    }

    private Supplier toSupplier(SupplierEntity entity) {
        return new Supplier(entity.getId(), entity.getName(), entity.isActive());
    }
}
