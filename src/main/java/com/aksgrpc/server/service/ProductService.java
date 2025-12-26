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

    public List<Product> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll();
    }

    public List<Product> getProductsByCategory(String category) {
        log.info("Fetching products by category: {}", category);
        return productRepository.findByCategory(category);
    }

    public List<Product> getActiveProductsByCategory(String category) {
        log.info("Fetching active products by category: {}", category);
        return productRepository.findByActiveTrueAndCategory(category);
    }

    public List<Product> searchProducts(String name, double maxPrice, int minQuantity) {
        log.info("Searching products with name: {}, maxPrice: {}, minQuantity: {}", name, maxPrice, minQuantity);

        if (maxPrice > 0 && minQuantity > 0) {
            // Search by both price and quantity
            List<Product> byPrice = productRepository.searchByNameAndMaxPrice(name, maxPrice);
            return byPrice.stream()
                    .filter(p -> p.getQuantity() >= minQuantity)
                    .toList();
        } else if (maxPrice > 0) {
            // Search by price only
            return productRepository.searchByNameAndMaxPrice(name, maxPrice);
        } else if (minQuantity > 0) {
            // Search by quantity only
            return productRepository.searchByNameAndMinQuantity(name, minQuantity);
        } else {
            // Search by name only
            return productRepository.findByNameContainingIgnoreCase(name);
        }
    }
}

