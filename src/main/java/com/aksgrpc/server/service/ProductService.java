package com.aksgrpc.server.service;

import com.aksgrpc.server.entity.Product;
import com.aksgrpc.server.exception.ProductNotFoundException;
import com.aksgrpc.server.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public Product getProductById(String id) {
        log.info("Fetching product with id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product createProduct(Product product) {
        log.info("Creating new product: {}", product.getName());
        return productRepository.save(product);
    }

    public Product updateProduct(String id, Product productUpdates) {
        log.info("Updating product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (productUpdates.getName() != null && !productUpdates.getName().isEmpty()) {
            product.setName(productUpdates.getName());
        }
        if (productUpdates.getDescription() != null && !productUpdates.getDescription().isEmpty()) {
            product.setDescription(productUpdates.getDescription());
        }
        if (productUpdates.getPrice() > 0) {
            product.setPrice(productUpdates.getPrice());
        }
        if (productUpdates.getQuantity() >= 0) {
            product.setQuantity(productUpdates.getQuantity());
        }
        if (productUpdates.getCategory() != null && !productUpdates.getCategory().isEmpty()) {
            product.setCategory(productUpdates.getCategory());
        }
        product.setActive(productUpdates.isActive());

        return productRepository.save(product);
    }

    public boolean deleteProduct(String id) {
        log.info("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
        return true;
    }

    public List<Product> getActiveProducts() {
        log.info("Fetching all active products");
        return productRepository.findByActiveTrue();
    }

}

