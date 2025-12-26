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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private StreamObserver<ProductResponse> listResponseObserver;

    @Mock
    private StreamObserver<ProductResponse> searchResponseObserver;

    @Mock
    private StreamObserver<BulkCreateResponse> bulkCreateResponseObserver;

    @Mock
    private StreamObserver<TotalValueResponse> totalValueResponseObserver;

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
            assertInstanceOf(StatusException.class, error);
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
            assertInstanceOf(StatusException.class, error);
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
            assertNotNull(((StatusException) error).getStatus().getDescription());
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
            assertNotNull(((StatusException) error).getStatus().getDescription());
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
            assertNotNull(((StatusException) error).getStatus().getDescription());
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

    @Nested
    @DisplayName("listProducts tests")
    class ListProductsTests {

        @Test
        @DisplayName("Should list all products when no filters applied")
        void listProducts_noFilters_streamsAllProducts() {
            Product product1 = Product.builder()
                    .id("id1")
                    .name("Product 1")
                    .price(99.99)
                    .quantity(10)
                    .category("Electronics")
                    .active(true)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();

            when(productService.getAllProducts()).thenReturn(Collections.singletonList(product1));

            ListProductsRequest request = ListProductsRequest.newBuilder().build();
            grpcService.listProducts(request, listResponseObserver);

            verify(listResponseObserver).onCompleted();
        }

        @Test
        @DisplayName("Should filter products by category")
        void listProducts_withCategory_streamsFilteredProducts() {
            Product product = Product.builder()
                    .id("id1")
                    .name("Laptop")
                    .price(999.99)
                    .quantity(3)
                    .category("Electronics")
                    .active(true)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();

            when(productService.getProductsByCategory("Electronics")).thenReturn(Collections.singletonList(product));

            ListProductsRequest request = ListProductsRequest.newBuilder()
                    .setCategory("Electronics")
                    .build();
            grpcService.listProducts(request, listResponseObserver);

            verify(listResponseObserver).onCompleted();
        }

        @Test
        @DisplayName("Should filter products by active status")
        void listProducts_activeOnly_streamsActiveProducts() {
            Product product = Product.builder()
                    .id("id1")
                    .name("Active Product")
                    .price(99.99)
                    .quantity(10)
                    .category("Test")
                    .active(true)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();

            when(productService.getActiveProducts()).thenReturn(Collections.singletonList(product));

            ListProductsRequest request = ListProductsRequest.newBuilder()
                    .setActiveOnly(true)
                    .build();
            grpcService.listProducts(request, listResponseObserver);

            verify(listResponseObserver).onCompleted();
        }

        @Test
        @DisplayName("Should apply page size limit")
        void listProducts_withPageSize_limitsResults() {
            Product product1 = Product.builder()
                    .id("id1")
                    .name("Product 1")
                    .price(99.99)
                    .quantity(10)
                    .category("Test")
                    .active(true)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();

            Product product2 = Product.builder()
                    .id("id2")
                    .name("Product 2")
                    .price(49.99)
                    .quantity(5)
                    .category("Test")
                    .active(true)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();

            when(productService.getAllProducts()).thenReturn(Arrays.asList(product1, product2));

            ListProductsRequest request = ListProductsRequest.newBuilder()
                    .setPageSize(1)
                    .build();
            grpcService.listProducts(request, listResponseObserver);

            verify(listResponseObserver).onCompleted();
        }
    }

    @Nested
    @DisplayName("searchProducts tests")
    class SearchProductsTests {

        @Test
        @DisplayName("Should return error when query is empty")
        void searchProducts_emptyQuery_returnsInvalidArgumentError() {
            SearchProductsRequest request = SearchProductsRequest.newBuilder()
                    .setName("")
                    .build();

            grpcService.searchProducts(request, searchResponseObserver);

            verify(searchResponseObserver).onError(errorCaptor.capture());
            Throwable error = errorCaptor.getValue();
            assertInstanceOf(StatusException.class, error);
            assertEquals(Status.INVALID_ARGUMENT.getCode(), ((StatusException) error).getStatus().getCode());
        }

        @Test
        @DisplayName("Should search products by name")
        void searchProducts_validQuery_streamsMatchingProducts() {
            Product product = Product.builder()
                    .id("id1")
                    .name("MacBook Pro")
                    .price(1999.99)
                    .quantity(2)
                    .category("Electronics")
                    .active(true)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();

            when(productService.searchProducts(anyString(), anyDouble(), anyInt()))
                    .thenReturn(Collections.singletonList(product));

            SearchProductsRequest request = SearchProductsRequest.newBuilder()
                    .setName("MacBook")
                    .build();
            grpcService.searchProducts(request, searchResponseObserver);

            verify(searchResponseObserver).onCompleted();
        }

        @Test
        @DisplayName("Should search products with price filter")
        void searchProducts_withMaxPrice_streamsFilteredProducts() {
            Product product = Product.builder()
                    .id("id1")
                    .name("Budget Laptop")
                    .price(499.99)
                    .quantity(10)
                    .category("Electronics")
                    .active(true)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();

            when(productService.searchProducts(anyString(), anyDouble(), anyInt()))
                    .thenReturn(Collections.singletonList(product));

            SearchProductsRequest request = SearchProductsRequest.newBuilder()
                    .setName("Laptop")
                    .setMaxPrice(1000.0)
                    .build();
            grpcService.searchProducts(request, searchResponseObserver);

            verify(searchResponseObserver).onCompleted();
        }

        @Test
        @DisplayName("Should search products with quantity filter")
        void searchProducts_withMinQuantity_streamsFilteredProducts() {
            Product product = Product.builder()
                    .id("id1")
                    .name("In Stock Product")
                    .price(99.99)
                    .quantity(20)
                    .category("Test")
                    .active(true)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .build();

            when(productService.searchProducts(anyString(), anyDouble(), anyInt()))
                    .thenReturn(Collections.singletonList(product));

            SearchProductsRequest request = SearchProductsRequest.newBuilder()
                    .setName("Product")
                    .setMinQuantity(10)
                    .build();
            grpcService.searchProducts(request, searchResponseObserver);

            verify(searchResponseObserver).onCompleted();
        }

        @Test
        @DisplayName("Should return empty stream when no products match")
        void searchProducts_noMatches_streamsNothing() {
            when(productService.searchProducts(anyString(), anyDouble(), anyInt()))
                    .thenReturn(new ArrayList<>());

            SearchProductsRequest request = SearchProductsRequest.newBuilder()
                    .setName("NonExistent")
                    .build();
            grpcService.searchProducts(request, searchResponseObserver);

            verify(searchResponseObserver).onCompleted();
        }
    }

    @Nested
    @DisplayName("bulkCreateProducts tests")
    class BulkCreateProductsTests {

        @Test
        @DisplayName("Should successfully create multiple products")
        void bulkCreateProducts_multipleValidRequests_createsAllProducts() {
            when(productService.createProduct(any(Product.class))).thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                product.setId("generated-id-" + System.nanoTime());
                return product;
            });
            when(productMapper.createProductRequestToEntity(any())).thenAnswer(invocation -> {
                CreateProductRequest req = invocation.getArgument(0);
                return Product.builder()
                        .name(req.getName())
                        .description(req.getDescription())
                        .price(req.getPrice())
                        .quantity(req.getQuantity())
                        .category(req.getCategory())
                        .active(true)
                        .createdAt(System.currentTimeMillis())
                        .updatedAt(System.currentTimeMillis())
                        .build();
            });

            ArgumentCaptor<BulkCreateResponse> responseCaptor = ArgumentCaptor.forClass(BulkCreateResponse.class);

            StreamObserver<CreateProductRequest> requestObserver = grpcService.bulkCreateProducts(bulkCreateResponseObserver);

            // Send 3 valid products
            CreateProductRequest req1 = CreateProductRequest.newBuilder()
                    .setName("Product 1")
                    .setPrice(99.99)
                    .setQuantity(10)
                    .build();
            CreateProductRequest req2 = CreateProductRequest.newBuilder()
                    .setName("Product 2")
                    .setPrice(149.99)
                    .setQuantity(5)
                    .build();
            CreateProductRequest req3 = CreateProductRequest.newBuilder()
                    .setName("Product 3")
                    .setPrice(199.99)
                    .setQuantity(8)
                    .build();

            requestObserver.onNext(req1);
            requestObserver.onNext(req2);
            requestObserver.onNext(req3);
            requestObserver.onCompleted();

            verify(bulkCreateResponseObserver).onNext(responseCaptor.capture());
            verify(bulkCreateResponseObserver).onCompleted();

            BulkCreateResponse response = responseCaptor.getValue();
            assertEquals(3, response.getTotalReceived());
            assertEquals(3, response.getTotalCreated());
            assertEquals(0, response.getTotalFailed());
        }

        @Test
        @DisplayName("Should handle invalid products in bulk create")
        void bulkCreateProducts_invalidRequests_reportErrors() {
            when(productService.createProduct(any(Product.class))).thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                product.setId("generated-id-" + System.nanoTime());
                return product;
            });
            when(productMapper.createProductRequestToEntity(any())).thenAnswer(invocation -> {
                CreateProductRequest req = invocation.getArgument(0);
                return Product.builder()
                        .name(req.getName())
                        .description(req.getDescription())
                        .price(req.getPrice())
                        .quantity(req.getQuantity())
                        .category(req.getCategory())
                        .active(true)
                        .createdAt(System.currentTimeMillis())
                        .updatedAt(System.currentTimeMillis())
                        .build();
            });

            ArgumentCaptor<BulkCreateResponse> responseCaptor = ArgumentCaptor.forClass(BulkCreateResponse.class);

            StreamObserver<CreateProductRequest> requestObserver = grpcService.bulkCreateProducts(bulkCreateResponseObserver);

            // Send 1 valid and 2 invalid products
            CreateProductRequest validReq = CreateProductRequest.newBuilder()
                    .setName("Valid Product")
                    .setPrice(99.99)
                    .setQuantity(10)
                    .build();

            CreateProductRequest invalidReq1 = CreateProductRequest.newBuilder()
                    .setName("")
                    .setPrice(99.99)
                    .setQuantity(10)
                    .build();

            CreateProductRequest invalidReq2 = CreateProductRequest.newBuilder()
                    .setName("Invalid Price")
                    .setPrice(0)
                    .setQuantity(10)
                    .build();

            requestObserver.onNext(validReq);
            requestObserver.onNext(invalidReq1);
            requestObserver.onNext(invalidReq2);
            requestObserver.onCompleted();

            verify(bulkCreateResponseObserver).onNext(responseCaptor.capture());
            verify(bulkCreateResponseObserver).onCompleted();

            BulkCreateResponse response = responseCaptor.getValue();
            assertEquals(3, response.getTotalReceived());
            assertEquals(1, response.getTotalCreated());
            assertEquals(2, response.getTotalFailed());
            assertEquals(2, response.getErrorMessagesList().size());
        }
    }

    @Nested
    @DisplayName("calculateTotalValue tests")
    class CalculateTotalValueTests {

        @Test
        @DisplayName("Should calculate total value for multiple products")
        void calculateTotalValue_multipleProductIds_returnsTotalValue() {
            Product product1 = Product.builder()
                    .id("id1")
                    .name("Product 1")
                    .price(100.0)
                    .quantity(2)
                    .active(true)
                    .build();

            Product product2 = Product.builder()
                    .id("id2")
                    .name("Product 2")
                    .price(50.0)
                    .quantity(3)
                    .active(true)
                    .build();

            when(productService.getProductById("id1")).thenReturn(product1);
            when(productService.getProductById("id2")).thenReturn(product2);

            ArgumentCaptor<TotalValueResponse> responseCaptor = ArgumentCaptor.forClass(TotalValueResponse.class);

            StreamObserver<ProductIdRequest> requestObserver = grpcService.calculateTotalValue(totalValueResponseObserver);

            ProductIdRequest req1 = ProductIdRequest.newBuilder().setId("id1").build();
            ProductIdRequest req2 = ProductIdRequest.newBuilder().setId("id2").build();

            requestObserver.onNext(req1);
            requestObserver.onNext(req2);
            requestObserver.onCompleted();

            verify(totalValueResponseObserver).onNext(responseCaptor.capture());
            verify(totalValueResponseObserver).onCompleted();

            TotalValueResponse response = responseCaptor.getValue();
            assertEquals(2, response.getProductCount());
            // Total value = (100 * 2) + (50 * 3) = 200 + 150 = 350
            assertEquals(350.0, response.getTotalValue(), 0.01);
            // Average = 350 / 2 = 175
            assertEquals(175.0, response.getAveragePrice(), 0.01);
        }

        @Test
        @DisplayName("Should handle missing products gracefully")
        void calculateTotalValue_missingProducts_continuesProcessing() {
            Product product1 = Product.builder()
                    .id("id1")
                    .name("Product 1")
                    .price(100.0)
                    .quantity(2)
                    .active(true)
                    .build();

            when(productService.getProductById("id1")).thenReturn(product1);
            when(productService.getProductById("non-existent")).thenThrow(new ProductNotFoundException("non-existent"));

            ArgumentCaptor<TotalValueResponse> responseCaptor = ArgumentCaptor.forClass(TotalValueResponse.class);

            StreamObserver<ProductIdRequest> requestObserver = grpcService.calculateTotalValue(totalValueResponseObserver);

            ProductIdRequest req1 = ProductIdRequest.newBuilder().setId("id1").build();
            ProductIdRequest req2 = ProductIdRequest.newBuilder().setId("non-existent").build();

            requestObserver.onNext(req1);
            requestObserver.onNext(req2);
            requestObserver.onCompleted();

            verify(totalValueResponseObserver).onNext(responseCaptor.capture());
            verify(totalValueResponseObserver).onCompleted();

            TotalValueResponse response = responseCaptor.getValue();
            // Only 1 product found (the non-existent one is skipped)
            assertEquals(1, response.getProductCount());
            assertEquals(200.0, response.getTotalValue(), 0.01);
        }

        @Test
        @DisplayName("Should return zero values when no products found")
        void calculateTotalValue_noProductsFound_returnsZeroes() {
            when(productService.getProductById(anyString())).thenThrow(new ProductNotFoundException("any"));

            ArgumentCaptor<TotalValueResponse> responseCaptor = ArgumentCaptor.forClass(TotalValueResponse.class);

            StreamObserver<ProductIdRequest> requestObserver = grpcService.calculateTotalValue(totalValueResponseObserver);

            ProductIdRequest req = ProductIdRequest.newBuilder().setId("non-existent").build();
            requestObserver.onNext(req);
            requestObserver.onCompleted();

            verify(totalValueResponseObserver).onNext(responseCaptor.capture());
            verify(totalValueResponseObserver).onCompleted();

            TotalValueResponse response = responseCaptor.getValue();
            assertEquals(0, response.getProductCount());
            assertEquals(0.0, response.getTotalValue(), 0.01);
            assertEquals(0.0, response.getAveragePrice(), 0.01);
        }
    }
}
