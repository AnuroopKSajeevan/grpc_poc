package com.aksgrpc.server.grpc;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.CountDownLatch;

/**
 * Sample gRPC client demonstrating Client Streaming RPCs
 * Usage:
 * 1. Start the gRPC server: ./gradlew bootRun
 * 2. Create some products first using CreateProduct RPC
 * 3. Run this client to test client streaming RPCs
 */
@Slf4j
public class ProductServiceClientStreamingClient {

    private final ProductServiceGrpc.ProductServiceStub asyncStub;

    public ProductServiceClientStreamingClient(Channel channel) {
        asyncStub = ProductServiceGrpc.newStub(channel);
    }

    /**
     * Demonstrates BulkCreateProducts client streaming RPC
     * Client streams multiple product creation requests and receives summary
     */
    public void bulkCreateProductsExample() {
        log.info("========== BULK CREATE PRODUCTS (Client Streaming) ==========");

        final CountDownLatch finish = new CountDownLatch(1);

        StreamObserver<CreateProductRequest> requestObserver = asyncStub.bulkCreateProducts(
                new StreamObserver<>() {
                    @Override
                    public void onNext(BulkCreateResponse response) {
                        log.info("Received bulk create response:");
                        log.info("  Total received: {}", response.getTotalReceived());
                        log.info("  Total created: {}", response.getTotalCreated());
                        log.info("  Total failed: {}", response.getTotalFailed());
                        log.info("  Created IDs: {}", response.getCreatedIdsList());
                        if (!response.getErrorMessagesList().isEmpty()) {
                            log.warn("  Errors: {}", response.getErrorMessagesList());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.error("Error in BulkCreateProducts: {}", t.getMessage());
                        finish.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        log.info("BulkCreateProducts stream completed");
                        finish.countDown();
                    }
                }
        );

        try {
            // Send first product
            CreateProductRequest product1 = CreateProductRequest.newBuilder()
                    .setName("MacBook Pro 14\"")
                    .setDescription("Apple laptop M3 Max")
                    .setPrice(1999.99)
                    .setQuantity(3)
                    .setCategory("Electronics")
                    .build();
            log.info("Sending product 1: {}", product1.getName());
            requestObserver.onNext(product1);

            Thread.sleep(100);

            // Send second product
            CreateProductRequest product2 = CreateProductRequest.newBuilder()
                    .setName("iPhone 15 Pro")
                    .setDescription("Latest Apple phone")
                    .setPrice(999.99)
                    .setQuantity(10)
                    .setCategory("Electronics")
                    .build();
            log.info("Sending product 2: {}", product2.getName());
            requestObserver.onNext(product2);

            Thread.sleep(100);

            // Send third product
            CreateProductRequest product3 = CreateProductRequest.newBuilder()
                    .setName("AirPods Pro")
                    .setDescription("Wireless earbuds")
                    .setPrice(249.99)
                    .setQuantity(25)
                    .setCategory("Electronics")
                    .build();
            log.info("Sending product 3: {}", product3.getName());
            requestObserver.onNext(product3);

            Thread.sleep(100);

            // Send invalid product (missing name) to test error handling
            CreateProductRequest invalidProduct = CreateProductRequest.newBuilder()
                    .setDescription("Missing name")
                    .setPrice(99.99)
                    .setQuantity(5)
                    .build();
            log.info("Sending invalid product (no name)");
            requestObserver.onNext(invalidProduct);

            // Complete the stream
            log.info("Completing stream...");
            requestObserver.onCompleted();

            // Wait for response
            finish.await();

        } catch (InterruptedException e) {
            log.error("Interrupted while sending requests", e);
            requestObserver.onError(e);
        }
    }

    /**
     * Demonstrates CalculateTotalValue client streaming RPC
     * Client streams product IDs and receives total value calculation
     */
    public void calculateTotalValueExample(String... productIds) {
        log.info("========== CALCULATE TOTAL VALUE (Client Streaming) ==========");

        final CountDownLatch finish = new CountDownLatch(1);

        StreamObserver<ProductIdRequest> requestObserver = asyncStub.calculateTotalValue(
                new StreamObserver<TotalValueResponse>() {
                    @Override
                    public void onNext(TotalValueResponse response) {
                        log.info("Received total value response:");
                        log.info("  Product count: {}", response.getProductCount());
                        log.info("  Total value: ${}", response.getTotalValue());
                        log.info("  Average price: ${}", response.getAveragePrice());
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.error("Error in CalculateTotalValue: {}", t.getMessage());
                        finish.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        log.info("CalculateTotalValue stream completed");
                        finish.countDown();
                    }
                }
        );

        try {
            if (productIds.length == 0) {
                log.warn("No product IDs provided for calculation");
                requestObserver.onCompleted();
            } else {
                // Send product IDs
                for (int i = 0; i < productIds.length; i++) {
                    ProductIdRequest request = ProductIdRequest.newBuilder()
                            .setId(productIds[i])
                            .build();
                    log.info("Sending product ID #{}: {}", i + 1, productIds[i]);
                    requestObserver.onNext(request);
                    Thread.sleep(50);
                }

                // Complete the stream
                log.info("Completing stream...");
                requestObserver.onCompleted();
            }

            // Wait for response
            finish.await();

        } catch (InterruptedException e) {
            log.error("Interrupted while sending requests", e);
            requestObserver.onError(e);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Create a channel to connect to the server
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        try {
            ProductServiceClientStreamingClient client = new ProductServiceClientStreamingClient(channel);

            log.info("\n========== STARTING GRPC CLIENT STREAMING CLIENT TESTS ==========\n");

            // Test 1: Bulk create products
            client.bulkCreateProductsExample();
            Thread.sleep(1000);

            // Test 2: Calculate total value (using some dummy IDs)
            // Note: These IDs should exist in your database, or you need to create products first
            client.calculateTotalValueExample("product-id-1", "product-id-2", "product-id-3");

            log.info("\n========== GRPC CLIENT STREAMING CLIENT TESTS COMPLETED ==========\n");

        } finally {
            channel.shutdownNow();
        }
    }
}

