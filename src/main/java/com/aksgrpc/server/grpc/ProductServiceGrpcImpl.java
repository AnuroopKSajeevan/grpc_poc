package com.aksgrpc.server.grpc;

import com.aksgrpc.server.entity.Product;
import com.aksgrpc.server.exception.ProductNotFoundException;
import com.aksgrpc.server.mapper.ProductMapper;
import com.aksgrpc.server.service.ProductService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class ProductServiceGrpcImpl extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductService productService;
    private final ProductMapper productMapper;

    /**
     * UNARY RPC: Get a single product by ID
     */
    @Override
    public void getProduct(GetProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        try {
            log.info("gRPC: GetProduct called with id: {}", request.getId());

            if (request.getId().isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Product ID cannot be empty")
                        .asException());
                return;
            }

            Product product = productService.getProductById(request.getId());
            ProductResponse response = productMapper.toProductResponse(product);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("gRPC: GetProduct completed successfully for id: {}", request.getId());
        } catch (ProductNotFoundException e) {
            log.error("gRPC: GetProduct failed - Product not found: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asException());
        } catch (Exception e) {
            log.error("gRPC: GetProduct failed with exception", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asException());
        }
    }

    /**
     * UNARY RPC: Create a new product
     */
    @Override
    public void createProduct(CreateProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        try {
            log.info("gRPC: CreateProduct called with name: {}", request.getName());

            // Validate request
            if (request.getName().isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Product name cannot be empty")
                        .asException());
                return;
            }

            if (request.getPrice() <= 0) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Product price must be greater than 0")
                        .asException());
                return;
            }

            if (request.getQuantity() < 0) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Product quantity cannot be negative")
                        .asException());
                return;
            }

            // Map request to entity
            Product product = productMapper.createProductRequestToEntity(request);

            // Save product
            Product savedProduct = productService.createProduct(product);
            ProductResponse response = productMapper.toProductResponse(savedProduct);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("gRPC: CreateProduct completed successfully with id: {}", savedProduct.getId());
        } catch (Exception e) {
            log.error("gRPC: CreateProduct failed with exception", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asException());
        }
    }

    /**
     * UNARY RPC: Update an existing product
     */
    @Override
    public void updateProduct(UpdateProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        try {
            log.info("gRPC: UpdateProduct called with id: {}", request.getId());

            if (request.getId().isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Product ID cannot be empty")
                        .asException());
                return;
            }

            // Fetch existing product
            Product product = productService.getProductById(request.getId());

            // Apply updates
            productMapper.updateProductFromRequest(request, product);

            // Save updated product
            Product updatedProduct = productService.createProduct(product);
            ProductResponse response = productMapper.toProductResponse(updatedProduct);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("gRPC: UpdateProduct completed successfully for id: {}", request.getId());
        } catch (ProductNotFoundException e) {
            log.error("gRPC: UpdateProduct failed - Product not found: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asException());
        } catch (Exception e) {
            log.error("gRPC: UpdateProduct failed with exception", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asException());
        }
    }

    /**
     * UNARY RPC: Delete a product
     */
    @Override
    public void deleteProduct(DeleteProductRequest request, StreamObserver<DeleteProductResponse> responseObserver) {
        try {
            log.info("gRPC: DeleteProduct called with id: {}", request.getId());

            if (request.getId().isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Product ID cannot be empty")
                        .asException());
                return;
            }

            boolean deleted = productService.deleteProduct(request.getId());

            DeleteProductResponse response = DeleteProductResponse.newBuilder()
                    .setSuccess(deleted)
                    .setMessage(deleted ? "Product deleted successfully" : "Failed to delete product")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("gRPC: DeleteProduct completed successfully for id: {}", request.getId());
        } catch (ProductNotFoundException e) {
            log.error("gRPC: DeleteProduct failed - Product not found: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asException());
        } catch (Exception e) {
            log.error("gRPC: DeleteProduct failed with exception", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asException());
        }
    }

    // ============================================
    // SERVER STREAMING RPC IMPLEMENTATIONS
    // ============================================

    /**
     * SERVER STREAMING RPC: List all products with optional filters
     * Streams products one by one based on filters (category, active status, page size)
     */
    @Override
    public void listProducts(ListProductsRequest request, StreamObserver<ProductResponse> responseObserver) {
        try {
            log.info("gRPC: ListProducts called with filters - category: {}, activeOnly: {}, pageSize: {}",
                    request.getCategory(), request.getActiveOnly(), request.getPageSize());

            java.util.List<Product> products;

            // Apply filters based on request
            if (!request.getCategory().isEmpty() && request.getActiveOnly()) {
                // Filter by category and active status
                products = productService.getActiveProductsByCategory(request.getCategory());
            } else if (!request.getCategory().isEmpty()) {
                // Filter by category only
                products = productService.getProductsByCategory(request.getCategory());
            } else if (request.getActiveOnly()) {
                // Filter by active status only
                products = productService.getActiveProducts();
            } else {
                // No filters, get all products
                products = productService.getAllProducts();
            }

            // Apply page size limit if specified
            if (request.getPageSize() > 0) {
                products = products.stream()
                        .limit(request.getPageSize())
                        .toList();
            }

            // Stream each product as a response
            int count = 0;
            for (Product product : products) {
                ProductResponse response = productMapper.toProductResponse(product);
                responseObserver.onNext(response);
                count++;
            }

            responseObserver.onCompleted();
            log.info("gRPC: ListProducts completed successfully. Streamed {} products", count);

        } catch (Exception e) {
            log.error("gRPC: ListProducts failed with exception", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asException());
        }
    }

    /**
     * SERVER STREAMING RPC: Search products by criteria
     * Streams matching products based on search query, max price, and min quantity filters
     */
    @Override
    public void searchProducts(SearchProductsRequest request, StreamObserver<ProductResponse> responseObserver) {
        try {
            log.info("gRPC: SearchProducts called with name: {}, maxPrice: {}, minQuantity: {}",
                    request.getName(), request.getMaxPrice(), request.getMinQuantity());

            if (request.getName().isEmpty()) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Search name cannot be empty")
                        .asException());
                return;
            }

            // Search products using service with all filters
            java.util.List<Product> searchResults = productService.searchProducts(
                    request.getName(),
                    request.getMaxPrice(),
                    request.getMinQuantity()
            );

            // Stream each matching product as a response
            int count = 0;
            for (Product product : searchResults) {
                ProductResponse response = productMapper.toProductResponse(product);
                responseObserver.onNext(response);
                count++;
            }

            responseObserver.onCompleted();
            log.info("gRPC: SearchProducts completed successfully. Found and streamed {} products", count);

        } catch (Exception e) {
            log.error("gRPC: SearchProducts failed with exception", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asException());
        }
    }

    // ============================================
    // CLIENT STREAMING RPC IMPLEMENTATIONS
    // ============================================

    /**
     * CLIENT STREAMING RPC: Bulk create products
     * Client streams multiple CreateProductRequest messages, server returns BulkCreateResponse with summary
     */
    @Override
    public StreamObserver<CreateProductRequest> bulkCreateProducts(
            StreamObserver<BulkCreateResponse> responseObserver) {

        return new StreamObserver<>() {
            private final java.util.List<String> createdIds = new java.util.ArrayList<>();
            private final java.util.List<String> errorMessages = new java.util.ArrayList<>();
            private int totalReceived = 0;

            @Override
            public void onNext(CreateProductRequest request) {
                try {
                    totalReceived++;
                    log.debug("gRPC: BulkCreateProducts received request #{} - product: {}",
                            totalReceived, request.getName());

                    // Validate request
                    if (request.getName().isEmpty()) {
                        errorMessages.add("Request #" + totalReceived + ": Product name cannot be empty");
                        return;
                    }

                    if (request.getPrice() <= 0) {
                        errorMessages.add("Request #" + totalReceived + ": Product price must be greater than 0");
                        return;
                    }

                    if (request.getQuantity() < 0) {
                        errorMessages.add("Request #" + totalReceived + ": Product quantity cannot be negative");
                        return;
                    }

                    // Map and create product
                    Product product = productMapper.createProductRequestToEntity(request);
                    Product savedProduct = productService.createProduct(product);
                    createdIds.add(savedProduct.getId());

                    log.debug("gRPC: BulkCreateProducts successfully created product with id: {}",
                            savedProduct.getId());

                } catch (Exception e) {
                    log.error("gRPC: BulkCreateProducts error processing request #{}", totalReceived, e);
                    errorMessages.add("Request #" + totalReceived + ": " + e.getMessage());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC: BulkCreateProducts stream error", t);
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Error in client stream: " + t.getMessage())
                        .asException());
            }

            @Override
            public void onCompleted() {
                try {
                    log.info("gRPC: BulkCreateProducts stream completed. Total received: {}, Created: {}, Failed: {}",
                            totalReceived, createdIds.size(), errorMessages.size());

                    BulkCreateResponse response = BulkCreateResponse.newBuilder()
                            .setTotalReceived(totalReceived)
                            .setTotalCreated(createdIds.size())
                            .setTotalFailed(errorMessages.size())
                            .addAllCreatedIds(createdIds)
                            .addAllErrorMessages(errorMessages)
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();

                } catch (Exception e) {
                    log.error("gRPC: BulkCreateProducts failed to send response", e);
                    responseObserver.onError(Status.INTERNAL
                            .withDescription("Internal server error: " + e.getMessage())
                            .asException());
                }
            }
        };
    }

    /**
     * CLIENT STREAMING RPC: Calculate total value of products
     * Client streams ProductIdRequest messages, server returns TotalValueResponse with calculated values
     */
    @Override
    public StreamObserver<ProductIdRequest> calculateTotalValue(
            StreamObserver<TotalValueResponse> responseObserver) {

        return new StreamObserver<>() {
            private final java.util.List<Product> products = new java.util.ArrayList<>();
            private int totalRequested = 0;
            private int totalFound = 0;

            @Override
            public void onNext(ProductIdRequest request) {
                try {
                    totalRequested++;
                    log.debug("gRPC: CalculateTotalValue received request #{} - product id: {}",
                            totalRequested, request.getId());

                    if (request.getId().isEmpty()) {
                        log.warn("gRPC: CalculateTotalValue request #{} has empty product ID", totalRequested);
                        return;
                    }

                    try {
                        Product product = productService.getProductById(request.getId());
                        products.add(product);
                        totalFound++;
                        log.debug("gRPC: CalculateTotalValue found product: {} with value: {}",
                                product.getName(), product.getPrice() * product.getQuantity());

                    } catch (Exception e) {
                        log.warn("gRPC: CalculateTotalValue product not found for id: {}", request.getId());
                        // Continue processing other products instead of failing
                    }

                } catch (Exception e) {
                    log.error("gRPC: CalculateTotalValue error processing request #{}", totalRequested, e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC: CalculateTotalValue stream error", t);
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Error in client stream: " + t.getMessage())
                        .asException());
            }

            @Override
            public void onCompleted() {
                try {
                    // Calculate totals
                    double totalValue = 0.0;
                    for (Product product : products) {
                        totalValue += product.getPrice() * product.getQuantity();
                    }

                    double averagePrice = products.isEmpty() ? 0.0 : totalValue / products.size();

                    log.info("gRPC: CalculateTotalValue stream completed. Total requested: {}, Found: {}, " +
                            "Total value: ${}, Average price: ${}",
                            totalRequested, totalFound, totalValue, averagePrice);

                    TotalValueResponse response = TotalValueResponse.newBuilder()
                            .setProductCount(totalFound)
                            .setTotalValue(totalValue)
                            .setAveragePrice(averagePrice)
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();

                } catch (Exception e) {
                    log.error("gRPC: CalculateTotalValue failed to send response", e);
                    responseObserver.onError(Status.INTERNAL
                            .withDescription("Internal server error: " + e.getMessage())
                            .asException());
                }
            }
        };
    }

    // ============================================
    // BI-DIRECTIONAL STREAMING RPC IMPLEMENTATIONS
    // ============================================

    /**
     * BI-DIRECTIONAL STREAMING RPC: Product Updates
     * Client and server both stream messages
     * Client sends ProductUpdateRequest (create, update, delete, get operations)
     * Server responds with ProductUpdateResponse for each request
     */
    @Override
    public StreamObserver<ProductUpdateRequest> productUpdates(
            StreamObserver<ProductUpdateResponse> responseObserver) {

        return new StreamObserver<ProductUpdateRequest>() {
            private int requestCount = 0;

            @Override
            public void onNext(ProductUpdateRequest request) {
                try {
                    requestCount++;
                    log.info("gRPC: ProductUpdates received request #{} with id: {}",
                            requestCount, request.getRequestId());

                    if (request.getRequestId().isEmpty()) {
                        ProductUpdateResponse response = ProductUpdateResponse.newBuilder()
                                .setRequestId("unknown-" + requestCount)
                                .setSuccess(false)
                                .setMessage("Request ID cannot be empty")
                                .setServerTimestamp(System.currentTimeMillis())
                                .build();
                        responseObserver.onNext(response);
                        return;
                    }

                    ProductUpdateResponse response = null;

                    // Handle different action types
                    if (request.hasCreate()) {
                        // Handle create action
                        CreateProductRequest createReq = request.getCreate();
                        log.debug("gRPC: ProductUpdates - CREATE action for request: {}", request.getRequestId());

                        if (createReq.getName().isEmpty()) {
                            response = ProductUpdateResponse.newBuilder()
                                    .setRequestId(request.getRequestId())
                                    .setSuccess(false)
                                    .setMessage("Product name cannot be empty")
                                    .setServerTimestamp(System.currentTimeMillis())
                                    .build();
                        } else if (createReq.getPrice() <= 0) {
                            response = ProductUpdateResponse.newBuilder()
                                    .setRequestId(request.getRequestId())
                                    .setSuccess(false)
                                    .setMessage("Product price must be greater than 0")
                                    .setServerTimestamp(System.currentTimeMillis())
                                    .build();
                        } else {
                            try {
                                Product product = productMapper.createProductRequestToEntity(createReq);
                                Product savedProduct = productService.createProduct(product);
                                ProductResponse productResp = productMapper.toProductResponse(savedProduct);

                                response = ProductUpdateResponse.newBuilder()
                                        .setRequestId(request.getRequestId())
                                        .setSuccess(true)
                                        .setMessage("Product created successfully")
                                        .setProduct(productResp)
                                        .setServerTimestamp(System.currentTimeMillis())
                                        .build();
                                log.debug("gRPC: ProductUpdates - Created product: {}", savedProduct.getId());
                            } catch (Exception e) {
                                response = ProductUpdateResponse.newBuilder()
                                        .setRequestId(request.getRequestId())
                                        .setSuccess(false)
                                        .setMessage("Error creating product: " + e.getMessage())
                                        .setServerTimestamp(System.currentTimeMillis())
                                        .build();
                            }
                        }
                    } else if (request.hasUpdate()) {
                        // Handle update action
                        UpdateProductRequest updateReq = request.getUpdate();
                        log.debug("gRPC: ProductUpdates - UPDATE action for product: {}", updateReq.getId());

                        if (updateReq.getId().isEmpty()) {
                            response = ProductUpdateResponse.newBuilder()
                                    .setRequestId(request.getRequestId())
                                    .setSuccess(false)
                                    .setMessage("Product ID cannot be empty")
                                    .setServerTimestamp(System.currentTimeMillis())
                                    .build();
                        } else {
                            try {
                                Product product = productService.getProductById(updateReq.getId());
                                productMapper.updateProductFromRequest(updateReq, product);
                                Product updatedProduct = productService.createProduct(product);
                                ProductResponse productResp = productMapper.toProductResponse(updatedProduct);

                                response = ProductUpdateResponse.newBuilder()
                                        .setRequestId(request.getRequestId())
                                        .setSuccess(true)
                                        .setMessage("Product updated successfully")
                                        .setProduct(productResp)
                                        .setServerTimestamp(System.currentTimeMillis())
                                        .build();
                                log.debug("gRPC: ProductUpdates - Updated product: {}", updateReq.getId());
                            } catch (ProductNotFoundException e) {
                                response = ProductUpdateResponse.newBuilder()
                                        .setRequestId(request.getRequestId())
                                        .setSuccess(false)
                                        .setMessage("Product not found: " + e.getMessage())
                                        .setServerTimestamp(System.currentTimeMillis())
                                        .build();
                            } catch (Exception e) {
                                response = ProductUpdateResponse.newBuilder()
                                        .setRequestId(request.getRequestId())
                                        .setSuccess(false)
                                        .setMessage("Error updating product: " + e.getMessage())
                                        .setServerTimestamp(System.currentTimeMillis())
                                        .build();
                            }
                        }
                    } else if (request.hasDelete()) {
                        // Handle delete action
                        DeleteProductRequest deleteReq = request.getDelete();
                        log.debug("gRPC: ProductUpdates - DELETE action for product: {}", deleteReq.getId());

                        if (deleteReq.getId().isEmpty()) {
                            response = ProductUpdateResponse.newBuilder()
                                    .setRequestId(request.getRequestId())
                                    .setSuccess(false)
                                    .setMessage("Product ID cannot be empty")
                                    .setServerTimestamp(System.currentTimeMillis())
                                    .build();
                        } else {
                            try {
                                productService.deleteProduct(deleteReq.getId());
                                response = ProductUpdateResponse.newBuilder()
                                        .setRequestId(request.getRequestId())
                                        .setSuccess(true)
                                        .setMessage("Product deleted successfully")
                                        .setServerTimestamp(System.currentTimeMillis())
                                        .build();
                                log.debug("gRPC: ProductUpdates - Deleted product: {}", deleteReq.getId());
                            } catch (ProductNotFoundException e) {
                                response = ProductUpdateResponse.newBuilder()
                                        .setRequestId(request.getRequestId())
                                        .setSuccess(false)
                                        .setMessage("Product not found: " + e.getMessage())
                                        .setServerTimestamp(System.currentTimeMillis())
                                        .build();
                            } catch (Exception e) {
                                response = ProductUpdateResponse.newBuilder()
                                        .setRequestId(request.getRequestId())
                                        .setSuccess(false)
                                        .setMessage("Error deleting product: " + e.getMessage())
                                        .setServerTimestamp(System.currentTimeMillis())
                                        .build();
                            }
                        }
                    } else if (request.hasGet()) {
                        // Handle get action
                        GetProductRequest getReq = request.getGet();
                        log.debug("gRPC: ProductUpdates - GET action for product: {}", getReq.getId());

                        if (getReq.getId().isEmpty()) {
                            response = ProductUpdateResponse.newBuilder()
                                    .setRequestId(request.getRequestId())
                                    .setSuccess(false)
                                    .setMessage("Product ID cannot be empty")
                                    .setServerTimestamp(System.currentTimeMillis())
                                    .build();
                        } else {
                            try {
                                Product product = productService.getProductById(getReq.getId());
                                ProductResponse productResp = productMapper.toProductResponse(product);

                                response = ProductUpdateResponse.newBuilder()
                                        .setRequestId(request.getRequestId())
                                        .setSuccess(true)
                                        .setMessage("Product retrieved successfully")
                                        .setProduct(productResp)
                                        .setServerTimestamp(System.currentTimeMillis())
                                        .build();
                                log.debug("gRPC: ProductUpdates - Retrieved product: {}", getReq.getId());
                            } catch (ProductNotFoundException e) {
                                response = ProductUpdateResponse.newBuilder()
                                        .setRequestId(request.getRequestId())
                                        .setSuccess(false)
                                        .setMessage("Product not found: " + e.getMessage())
                                        .setServerTimestamp(System.currentTimeMillis())
                                        .build();
                            } catch (Exception e) {
                                response = ProductUpdateResponse.newBuilder()
                                        .setRequestId(request.getRequestId())
                                        .setSuccess(false)
                                        .setMessage("Error retrieving product: " + e.getMessage())
                                        .setServerTimestamp(System.currentTimeMillis())
                                        .build();
                            }
                        }
                    } else {
                        response = ProductUpdateResponse.newBuilder()
                                .setRequestId(request.getRequestId())
                                .setSuccess(false)
                                .setMessage("No valid action specified (create, update, delete, or get)")
                                .setServerTimestamp(System.currentTimeMillis())
                                .build();
                    }

                    // Send response immediately
                    responseObserver.onNext(response);

                } catch (Exception e) {
                    log.error("gRPC: ProductUpdates error processing request", e);
                    ProductUpdateResponse errorResponse = ProductUpdateResponse.newBuilder()
                            .setRequestId(request.getRequestId())
                            .setSuccess(false)
                            .setMessage("Unexpected error: " + e.getMessage())
                            .setServerTimestamp(System.currentTimeMillis())
                            .build();
                    responseObserver.onNext(errorResponse);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC: ProductUpdates stream error", t);
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Error in bidirectional stream: " + t.getMessage())
                        .asException());
            }

            @Override
            public void onCompleted() {
                log.info("gRPC: ProductUpdates stream completed. Total requests processed: {}", requestCount);
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * BI-DIRECTIONAL STREAMING RPC: Inventory Sync
     * Client and server both stream messages
     * Client sends InventorySyncRequest (inventory changes)
     * Server responds with InventorySyncResponse confirming the change
     */
    @Override
    public StreamObserver<InventorySyncRequest> inventorySync(
            StreamObserver<InventorySyncResponse> responseObserver) {

        return new StreamObserver<InventorySyncRequest>() {
            private int syncCount = 0;

            @Override
            public void onNext(InventorySyncRequest request) {
                try {
                    syncCount++;
                    log.info("gRPC: InventorySync received request #{} for product: {}, change: {}",
                            syncCount, request.getProductId(), request.getQuantityChange());

                    if (request.getProductId().isEmpty()) {
                        InventorySyncResponse response = InventorySyncResponse.newBuilder()
                                .setProductId("unknown-" + syncCount)
                                .setSuccess(false)
                                .setMessage("Product ID cannot be empty")
                                .setServerTimestamp(System.currentTimeMillis())
                                .build();
                        responseObserver.onNext(response);
                        return;
                    }

                    try {
                        // Retrieve the product
                        Product product = productService.getProductById(request.getProductId());
                        int previousQuantity = product.getQuantity();
                        int newQuantity = previousQuantity + request.getQuantityChange();

                        // Validate new quantity
                        if (newQuantity < 0) {
                            InventorySyncResponse response = InventorySyncResponse.newBuilder()
                                    .setProductId(request.getProductId())
                                    .setPreviousQuantity(previousQuantity)
                                    .setNewQuantity(previousQuantity)
                                    .setSuccess(false)
                                    .setMessage("Quantity change would result in negative inventory. " +
                                            "Current: " + previousQuantity + ", Change: " + request.getQuantityChange())
                                    .setServerTimestamp(System.currentTimeMillis())
                                    .build();
                            responseObserver.onNext(response);
                            return;
                        }

                        // Update inventory
                        product.setQuantity(newQuantity);
                        product.setUpdatedAt(System.currentTimeMillis());
                        productService.createProduct(product);

                        InventorySyncResponse response = InventorySyncResponse.newBuilder()
                                .setProductId(request.getProductId())
                                .setPreviousQuantity(previousQuantity)
                                .setNewQuantity(newQuantity)
                                .setSuccess(true)
                                .setMessage("Inventory updated successfully. Reason: " + request.getReason())
                                .setServerTimestamp(System.currentTimeMillis())
                                .build();

                        responseObserver.onNext(response);
                        log.debug("gRPC: InventorySync - Updated inventory for product {} from {} to {} (reason: {})",
                                request.getProductId(), previousQuantity, newQuantity, request.getReason());

                    } catch (ProductNotFoundException e) {
                        InventorySyncResponse response = InventorySyncResponse.newBuilder()
                                .setProductId(request.getProductId())
                                .setSuccess(false)
                                .setMessage("Product not found: " + e.getMessage())
                                .setServerTimestamp(System.currentTimeMillis())
                                .build();
                        responseObserver.onNext(response);
                        log.warn("gRPC: InventorySync - Product not found: {}", request.getProductId());

                    } catch (Exception e) {
                        InventorySyncResponse response = InventorySyncResponse.newBuilder()
                                .setProductId(request.getProductId())
                                .setSuccess(false)
                                .setMessage("Error updating inventory: " + e.getMessage())
                                .setServerTimestamp(System.currentTimeMillis())
                                .build();
                        responseObserver.onNext(response);
                        log.error("gRPC: InventorySync - Error processing request", e);
                    }

                } catch (Exception e) {
                    log.error("gRPC: InventorySync unexpected error", e);
                    InventorySyncResponse errorResponse = InventorySyncResponse.newBuilder()
                            .setProductId(request.getProductId())
                            .setSuccess(false)
                            .setMessage("Unexpected error: " + e.getMessage())
                            .setServerTimestamp(System.currentTimeMillis())
                            .build();
                    responseObserver.onNext(errorResponse);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC: InventorySync stream error", t);
                responseObserver.onError(Status.INTERNAL
                        .withDescription("Error in bidirectional stream: " + t.getMessage())
                        .asException());
            }

            @Override
            public void onCompleted() {
                log.info("gRPC: InventorySync stream completed. Total sync requests processed: {}", syncCount);
                responseObserver.onCompleted();
            }
        };
    }
}

