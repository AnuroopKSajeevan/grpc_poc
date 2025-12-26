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
}

