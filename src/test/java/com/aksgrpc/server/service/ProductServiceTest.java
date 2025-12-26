package com.aksgrpc.server.service;

import com.aksgrpc.server.entity.Product;
import com.aksgrpc.server.exception.ProductNotFoundException;
import com.aksgrpc.server.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id("test-id-123")
                .name("Test Product")
                .description("Test Description")
                .price(99.99)
                .quantity(10)
                .category("Electronics")
                .active(true)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
    }

    @Nested
    @DisplayName("getProductById tests")
    class GetProductByIdTests {

        @Test
        @DisplayName("Should return product when found")
        void getProductById_whenProductExists_returnsProduct() {
            when(productRepository.findById("test-id-123")).thenReturn(Optional.of(testProduct));

            Product result = productService.getProductById("test-id-123");

            assertNotNull(result);
            assertEquals("test-id-123", result.getId());
            assertEquals("Test Product", result.getName());
            verify(productRepository).findById("test-id-123");
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException when not found")
        void getProductById_whenProductNotFound_throwsException() {
            when(productRepository.findById("non-existent")).thenReturn(Optional.empty());

            ProductNotFoundException exception = assertThrows(
                    ProductNotFoundException.class,
                    () -> productService.getProductById("non-existent")
            );

            assertTrue(exception.getMessage().contains("non-existent"));
            verify(productRepository).findById("non-existent");
        }
    }

    @Nested
    @DisplayName("createProduct tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should save and return product")
        void createProduct_savesAndReturnsProduct() {
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            Product result = productService.createProduct(testProduct);

            assertNotNull(result);
            assertEquals("Test Product", result.getName());
            verify(productRepository).save(testProduct);
        }
    }

    @Nested
    @DisplayName("updateProduct tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update product fields when provided")
        void updateProduct_whenValidUpdates_updatesProduct() {
            Product updates = Product.builder()
                    .name("Updated Name")
                    .price(149.99)
                    .build();

            when(productRepository.findById("test-id-123")).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Product result = productService.updateProduct("test-id-123", updates);

            assertEquals("Updated Name", result.getName());
            assertEquals(149.99, result.getPrice());
            verify(productRepository).findById("test-id-123");
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException when updating non-existent product")
        void updateProduct_whenProductNotFound_throwsException() {
            Product updates = Product.builder().name("Updated Name").build();
            when(productRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(
                    ProductNotFoundException.class,
                    () -> productService.updateProduct("non-existent", updates)
            );
        }
    }

    @Nested
    @DisplayName("deleteProduct tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should delete product when exists")
        void deleteProduct_whenProductExists_deletesAndReturnsTrue() {
            when(productRepository.existsById("test-id-123")).thenReturn(true);
            doNothing().when(productRepository).deleteById("test-id-123");

            boolean result = productService.deleteProduct("test-id-123");

            assertTrue(result);
            verify(productRepository).existsById("test-id-123");
            verify(productRepository).deleteById("test-id-123");
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException when deleting non-existent product")
        void deleteProduct_whenProductNotFound_throwsException() {
            when(productRepository.existsById("non-existent")).thenReturn(false);

            assertThrows(
                    ProductNotFoundException.class,
                    () -> productService.deleteProduct("non-existent")
            );

            verify(productRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("getActiveProducts tests")
    class GetActiveProductsTests {

        @Test
        @DisplayName("Should return list of active products")
        void getActiveProducts_returnsActiveProducts() {
            List<Product> activeProducts = List.of(testProduct);
            when(productRepository.findByActiveTrue()).thenReturn(activeProducts);

            List<Product> result = productService.getActiveProducts();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.get(0).isActive());
            verify(productRepository).findByActiveTrue();
        }

        @Test
        @DisplayName("Should return empty list when no active products")
        void getActiveProducts_whenNoActiveProducts_returnsEmptyList() {
            when(productRepository.findByActiveTrue()).thenReturn(List.of());

            List<Product> result = productService.getActiveProducts();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
