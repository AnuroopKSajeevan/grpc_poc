package com.aksgrpc.server.grpc;

import com.aksgrpc.server.entity.Product;
import com.aksgrpc.server.exception.ProductNotFoundException;
import com.aksgrpc.server.mapper.ProductMapper;
import com.aksgrpc.server.service.ProductService;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceGrpcImplTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private StreamObserver<ProductResponse> productResponseObserver;

    @Mock
    private StreamObserver<DeleteProductResponse> deleteResponseObserver;

    @InjectMocks
    private ProductServiceGrpcImpl grpcService;

    @Captor
    private ArgumentCaptor<Throwable> errorCaptor;

    @Captor
    private ArgumentCaptor<ProductResponse> productResponseCaptor;

    @Captor
    private ArgumentCaptor<DeleteProductResponse> deleteResponseCaptor;

    private Product testProduct;
    private ProductResponse testProductResponse;

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

        testProductResponse = ProductResponse.newBuilder()
                .setId("test-id-123")
                .setName("Test Product")
                .setDescription("Test Description")
                .setPrice(99.99)
                .setQuantity(10)
                .setCategory("Electronics")
                .setActive(true)
                .build();
    }

    @Nested
    @DisplayName("getProduct tests")
    class GetProductTests {

        @Test
        @DisplayName("Should return product when found")
        void getProduct_whenProductExists_returnsProduct() {
            GetProductRequest request = GetProductRequest.newBuilder()
                    .setId("test-id-123")
                    .build();

            when(productService.getProductById("test-id-123")).thenReturn(testProduct);
            when(productMapper.toProductResponse(testProduct)).thenReturn(testProductResponse);

            grpcService.getProduct(request, productResponseObserver);

            verify(productResponseObserver).onNext(productResponseCaptor.capture());
            verify(productResponseObserver).onCompleted();
            verify(productResponseObserver, never()).onError(any());

            ProductResponse response = productResponseCaptor.getValue();
            assertEquals("test-id-123", response.getId());
            assertEquals("Test Product", response.getName());
        }

        @Test
        @DisplayName("Should return error when product ID is empty")
        void getProduct_whenIdEmpty_returnsInvalidArgumentError() {
            GetProductRequest request = GetProductRequest.newBuilder()
                    .setId("")
                    .build();

            grpcService.getProduct(request, productResponseObserver);

            verify(productResponseObserver).onError(errorCaptor.capture());
            verify(productResponseObserver, never()).onNext(any());
            verify(productResponseObserver, never()).onCompleted();

            Throwable error = errorCaptor.getValue();
            assertTrue(error instanceof StatusException);
            assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusException) error).getStatus().getCode());
        }

        @Test
        @DisplayName("Should return NOT_FOUND when product doesn't exist")
        void getProduct_whenProductNotFound_returnsNotFoundError() {
            GetProductRequest request = GetProductRequest.newBuilder()
                    .setId("non-existent")
                    .build();

            when(productService.getProductById("non-existent"))
                    .thenThrow(new ProductNotFoundException("non-existent"));

            grpcService.getProduct(request, productResponseObserver);

            verify(productResponseObserver).onError(errorCaptor.capture());
            Throwable error = errorCaptor.getValue();
            assertTrue(error instanceof StatusException);
            assertEquals(Status.NOT_FOUND.getCode(), ((StatusException) error).getStatus().getCode());
        }
    }

    @Nested
    @DisplayName("createProduct tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully")
        void createProduct_withValidRequest_createsProduct() {
            CreateProductRequest request = CreateProductRequest.newBuilder()
                    .setName("New Product")
                    .setDescription("New Description")
                    .setPrice(49.99)
                    .setQuantity(5)
                    .setCategory("Books")
                    .build();

            when(productMapper.createProductRequestToEntity(request)).thenReturn(testProduct);
            when(productService.createProduct(testProduct)).thenReturn(testProduct);
            when(productMapper.toProductResponse(testProduct)).thenReturn(testProductResponse);

            grpcService.createProduct(request, productResponseObserver);

            verify(productResponseObserver).onNext(any(ProductResponse.class));
            verify(productResponseObserver).onCompleted();
            verify(productResponseObserver, never()).onError(any());
        }

        @Test
        @DisplayName("Should return error when name is empty")
        void createProduct_whenNameEmpty_returnsInvalidArgumentError() {
            CreateProductRequest request = CreateProductRequest.newBuilder()
                    .setName("")
                    .setPrice(49.99)
                    .setQuantity(5)
                    .build();

            grpcService.createProduct(request, productResponseObserver);

            verify(productResponseObserver).onError(errorCaptor.capture());
            Throwable error = errorCaptor.getValue();
            assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusException) error).getStatus().getCode());
            assertTrue(((StatusException) error).getStatus().getDescription().contains("name"));
        }

        @Test
        @DisplayName("Should return error when price is zero or negative")
        void createProduct_whenPriceInvalid_returnsInvalidArgumentError() {
            CreateProductRequest request = CreateProductRequest.newBuilder()
                    .setName("Product")
                    .setPrice(0)
                    .setQuantity(5)
                    .build();

            grpcService.createProduct(request, productResponseObserver);

            verify(productResponseObserver).onError(errorCaptor.capture());
            Throwable error = errorCaptor.getValue();
            assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusException) error).getStatus().getCode());
            assertTrue(((StatusException) error).getStatus().getDescription().contains("price"));
        }

        @Test
        @DisplayName("Should return error when quantity is negative")
        void createProduct_whenQuantityNegative_returnsInvalidArgumentError() {
            CreateProductRequest request = CreateProductRequest.newBuilder()
                    .setName("Product")
                    .setPrice(10.0)
                    .setQuantity(-1)
                    .build();

            grpcService.createProduct(request, productResponseObserver);

            verify(productResponseObserver).onError(errorCaptor.capture());
            Throwable error = errorCaptor.getValue();
            assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusException) error).getStatus().getCode());
            assertTrue(((StatusException) error).getStatus().getDescription().contains("quantity"));
        }
    }

    @Nested
    @DisplayName("updateProduct tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update product successfully")
        void updateProduct_withValidRequest_updatesProduct() {
            UpdateProductRequest request = UpdateProductRequest.newBuilder()
                    .setId("test-id-123")
                    .setName("Updated Name")
                    .setPrice(199.99)
                    .build();

            when(productService.getProductById("test-id-123")).thenReturn(testProduct);
            when(productService.createProduct(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toProductResponse(any(Product.class))).thenReturn(testProductResponse);

            grpcService.updateProduct(request, productResponseObserver);

            verify(productMapper).updateProductFromRequest(eq(request), any(Product.class));
            verify(productResponseObserver).onNext(any(ProductResponse.class));
            verify(productResponseObserver).onCompleted();
        }

        @Test
        @DisplayName("Should return error when ID is empty")
        void updateProduct_whenIdEmpty_returnsInvalidArgumentError() {
            UpdateProductRequest request = UpdateProductRequest.newBuilder()
                    .setId("")
                    .setName("Updated Name")
                    .build();

            grpcService.updateProduct(request, productResponseObserver);

            verify(productResponseObserver).onError(errorCaptor.capture());
            Throwable error = errorCaptor.getValue();
            assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusException) error).getStatus().getCode());
        }

        @Test
        @DisplayName("Should return NOT_FOUND when product doesn't exist")
        void updateProduct_whenProductNotFound_returnsNotFoundError() {
            UpdateProductRequest request = UpdateProductRequest.newBuilder()
                    .setId("non-existent")
                    .setName("Updated Name")
                    .build();

            when(productService.getProductById("non-existent"))
                    .thenThrow(new ProductNotFoundException("non-existent"));

            grpcService.updateProduct(request, productResponseObserver);

            verify(productResponseObserver).onError(errorCaptor.capture());
            Throwable error = errorCaptor.getValue();
            assertEquals(Status.NOT_FOUND.getCode(), ((StatusException) error).getStatus().getCode());
        }
    }

    @Nested
    @DisplayName("deleteProduct tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should delete product successfully")
        void deleteProduct_whenProductExists_deletesSuccessfully() {
            DeleteProductRequest request = DeleteProductRequest.newBuilder()
                    .setId("test-id-123")
                    .build();

            when(productService.deleteProduct("test-id-123")).thenReturn(true);

            grpcService.deleteProduct(request, deleteResponseObserver);

            verify(deleteResponseObserver).onNext(deleteResponseCaptor.capture());
            verify(deleteResponseObserver).onCompleted();

            DeleteProductResponse response = deleteResponseCaptor.getValue();
            assertTrue(response.getSuccess());
            assertTrue(response.getMessage().contains("successfully"));
        }

        @Test
        @DisplayName("Should return error when ID is empty")
        void deleteProduct_whenIdEmpty_returnsInvalidArgumentError() {
            DeleteProductRequest request = DeleteProductRequest.newBuilder()
                    .setId("")
                    .build();

            grpcService.deleteProduct(request, deleteResponseObserver);

            verify(deleteResponseObserver).onError(errorCaptor.capture());
            Throwable error = errorCaptor.getValue();
            assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusException) error).getStatus().getCode());
        }

        @Test
        @DisplayName("Should return NOT_FOUND when product doesn't exist")
        void deleteProduct_whenProductNotFound_returnsNotFoundError() {
            DeleteProductRequest request = DeleteProductRequest.newBuilder()
                    .setId("non-existent")
                    .build();

            when(productService.deleteProduct("non-existent"))
                    .thenThrow(new ProductNotFoundException("non-existent"));

            grpcService.deleteProduct(request, deleteResponseObserver);

            verify(deleteResponseObserver).onError(errorCaptor.capture());
            Throwable error = errorCaptor.getValue();
            assertEquals(Status.NOT_FOUND.getCode(), ((StatusException) error).getStatus().getCode());
        }
    }
}
