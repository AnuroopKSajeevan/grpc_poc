package com.aksgrpc.server.mapper;

import com.aksgrpc.server.entity.Product;
import com.aksgrpc.server.grpc.CreateProductRequest;
import com.aksgrpc.server.grpc.ProductResponse;
import com.aksgrpc.server.grpc.UpdateProductRequest;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class ProductMapper {

    public ProductResponse toProductResponse(Product product) {
        if (product == null) {
            return null;
        }

        return ProductResponse.newBuilder()
                .setId(product.getId())
                .setName(product.getName())
                .setDescription(product.getDescription())
                .setPrice(product.getPrice())
                .setQuantity(product.getQuantity())
                .setCategory(product.getCategory())
                .setActive(product.isActive())
                .setCreatedAt(product.getCreatedAt())
                .setUpdatedAt(product.getUpdatedAt())
                .build();
    }

    public Product createProductRequestToEntity(CreateProductRequest request) {
        if (request == null) {
            return null;
        }

        long now = Instant.now().toEpochMilli();

        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .category(request.getCategory())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void updateProductFromRequest(UpdateProductRequest request, Product product) {
        if (request == null || product == null) {
            return;
        }

        if (!request.getName().isEmpty()) {
            product.setName(request.getName());
        }
        if (!request.getDescription().isEmpty()) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() > 0) {
            product.setPrice(request.getPrice());
        }
        if (request.getQuantity() >= 0) {
            product.setQuantity(request.getQuantity());
        }
        if (!request.getCategory().isEmpty()) {
            product.setCategory(request.getCategory());
        }
        product.setActive(request.getActive());
        product.setUpdatedAt(Instant.now().toEpochMilli());
    }
}

