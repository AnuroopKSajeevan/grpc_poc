package com.aksgrpc.server.mapper;

import com.aksgrpc.server.entity.Product;
import com.aksgrpc.server.grpc.CreateProductRequest;
import com.aksgrpc.server.grpc.ProductResponse;
import com.aksgrpc.server.grpc.UpdateProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductMapperTest {

    private ProductMapper productMapper;

    @BeforeEach
    void setUp() {
        productMapper = new ProductMapper();
    }

    @Nested
    @DisplayName("toProductResponse tests")
    class ToProductResponseTests {

        @Test
        @DisplayName("Should map Product entity to ProductResponse")
        void toProductResponse_mapsAllFields() {
            Product product = Product.builder()
                    .id("test-id")
                    .name("Test Product")
                    .description("Test Description")
                    .price(99.99)
                    .quantity(10)
                    .category("Electronics")
                    .active(true)
                    .createdAt(1000L)
                    .updatedAt(2000L)
                    .build();

            ProductResponse response = productMapper.toProductResponse(product);

            assertNotNull(response);
            assertEquals("test-id", response.getId());
            assertEquals("Test Product", response.getName());
            assertEquals("Test Description", response.getDescription());
            assertEquals(99.99, response.getPrice(), 0.001);
            assertEquals(10, response.getQuantity());
            assertEquals("Electronics", response.getCategory());
            assertTrue(response.getActive());
            assertEquals(1000L, response.getCreatedAt());
            assertEquals(2000L, response.getUpdatedAt());
        }

        @Test
        @DisplayName("Should return null when product is null")
        void toProductResponse_whenNull_returnsNull() {
            ProductResponse response = productMapper.toProductResponse(null);
            assertNull(response);
        }
    }

    @Nested
    @DisplayName("createProductRequestToEntity tests")
    class CreateProductRequestToEntityTests {

        @Test
        @DisplayName("Should map CreateProductRequest to Product entity")
        void createProductRequestToEntity_mapsAllFields() {
            CreateProductRequest request = CreateProductRequest.newBuilder()
                    .setName("New Product")
                    .setDescription("New Description")
                    .setPrice(49.99)
                    .setQuantity(5)
                    .setCategory("Books")
                    .build();

            Product product = productMapper.createProductRequestToEntity(request);

            assertNotNull(product);
            assertNull(product.getId()); // ID should be null for new products
            assertEquals("New Product", product.getName());
            assertEquals("New Description", product.getDescription());
            assertEquals(49.99, product.getPrice(), 0.001);
            assertEquals(5, product.getQuantity());
            assertEquals("Books", product.getCategory());
            assertTrue(product.isActive()); // New products should be active by default
            assertTrue(product.getCreatedAt() > 0);
            assertTrue(product.getUpdatedAt() > 0);
        }

        @Test
        @DisplayName("Should return null when request is null")
        void createProductRequestToEntity_whenNull_returnsNull() {
            Product product = productMapper.createProductRequestToEntity(null);
            assertNull(product);
        }
    }

    @Nested
    @DisplayName("updateProductFromRequest tests")
    class UpdateProductFromRequestTests {

        @Test
        @DisplayName("Should update product fields from request")
        void updateProductFromRequest_updatesAllFields() {
            Product product = Product.builder()
                    .id("test-id")
                    .name("Old Name")
                    .description("Old Description")
                    .price(10.00)
                    .quantity(1)
                    .category("Old Category")
                    .active(false)
                    .createdAt(1000L)
                    .updatedAt(1000L)
                    .build();

            UpdateProductRequest request = UpdateProductRequest.newBuilder()
                    .setId("test-id")
                    .setName("New Name")
                    .setDescription("New Description")
                    .setPrice(99.99)
                    .setQuantity(50)
                    .setCategory("New Category")
                    .setActive(true)
                    .build();

            long beforeUpdate = System.currentTimeMillis();
            productMapper.updateProductFromRequest(request, product);

            assertEquals("New Name", product.getName());
            assertEquals("New Description", product.getDescription());
            assertEquals(99.99, product.getPrice(), 0.001);
            assertEquals(50, product.getQuantity());
            assertEquals("New Category", product.getCategory());
            assertTrue(product.isActive());
            assertTrue(product.getUpdatedAt() >= beforeUpdate);
        }

        @Test
        @DisplayName("Should not update fields when empty in request")
        void updateProductFromRequest_skipEmptyFields() {
            Product product = Product.builder()
                    .id("test-id")
                    .name("Original Name")
                    .description("Original Description")
                    .price(50.00)
                    .quantity(10)
                    .category("Original Category")
                    .active(true)
                    .build();

            UpdateProductRequest request = UpdateProductRequest.newBuilder()
                    .setId("test-id")
                    // Name and description are empty strings by default
                    .setActive(false)
                    .build();

            productMapper.updateProductFromRequest(request, product);

            assertEquals("Original Name", product.getName()); // Should not change
            assertEquals("Original Description", product.getDescription()); // Should not change
            assertFalse(product.isActive()); // Should change
        }

        @Test
        @DisplayName("Should handle null product gracefully")
        void updateProductFromRequest_whenProductNull_doesNothing() {
            UpdateProductRequest request = UpdateProductRequest.newBuilder()
                    .setName("New Name")
                    .build();

            // Should not throw exception
            assertDoesNotThrow(() -> productMapper.updateProductFromRequest(request, null));
        }

        @Test
        @DisplayName("Should handle null request gracefully")
        void updateProductFromRequest_whenRequestNull_doesNothing() {
            Product product = Product.builder().name("Original").build();

            // Should not throw exception
            assertDoesNotThrow(() -> productMapper.updateProductFromRequest(null, product));
            assertEquals("Original", product.getName()); // Should remain unchanged
        }
    }
}
