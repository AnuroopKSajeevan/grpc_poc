# gRPC Unary RPC Implementation Guide

This document describes the implementation of Unary RPCs (single request, single response) from the `product.proto` specification.

## Overview

The unary RPC implementation includes four main operations:
- **GetProduct**: Retrieve a product by ID
- **CreateProduct**: Create a new product
- **UpdateProduct**: Update an existing product
- **DeleteProduct**: Delete a product by ID

## Architecture

### Technology Stack
- **Framework**: Spring Boot 3.4.1
- **gRPC**: 1.75.0
- **Protocol Buffers**: 4.28.3
- **Database**: MongoDB
- **Java Version**: 21
- **Build Tool**: Gradle

### Project Structure

```
src/main/java/com/aksgrpc/server/
├── entity/
│   └── Product.java                    # MongoDB entity
├── repository/
│   └── ProductRepository.java          # MongoDB repository interface
├── service/
│   └── ProductService.java             # Business logic service
├── mapper/
│   └── ProductMapper.java              # Mapper for DTO conversions
├── grpc/
│   └── ProductServiceGrpcImpl.java      # gRPC service implementation
├── exception/
│   └── ProductNotFoundException.java    # Custom exception
├── config/
│   └── MongoDBConfig.java              # MongoDB configuration
└── ServerApplication.java              # Spring Boot application entry point
```

## Implementation Details

### 1. Entity Layer (Product.java)

The `Product` entity is mapped to MongoDB collection `products` with the following fields:

```java
@Document(collection = "products")
public class Product {
    @Id
    private String id;                  // MongoDB auto-generated ID
    private String name;
    private String description;
    private double price;
    private int quantity;
    private String category;
    private boolean active;             // Product active status
    private long createdAt;             // Unix timestamp
    private long updatedAt;             // Unix timestamp
}
```

### 2. Repository Layer (ProductRepository.java)

Spring Data MongoDB repository with custom query methods:

- `findById(String id)`: Get product by ID
- `findByCategory(String category)`: Filter by category
- `findByActiveTrue()`: Get all active products
- `findByNameContainingIgnoreCase(String name)`: Search by name
- `searchByNameAndMaxPrice(String name, double maxPrice)`: Search with price filter

### 3. Service Layer (ProductService.java)

Business logic service handling:

- **getProductById(String id)**: Retrieve product or throw `ProductNotFoundException`
- **createProduct(Product product)**: Save new product to MongoDB
- **updateProduct(String id, Product updates)**: Update existing product
- **deleteProduct(String id)**: Delete product and return success status
- **Search operations**: Support filtering by category, active status, and name

### 4. Mapper Layer (ProductMapper.java)

Converts between gRPC protocol buffer messages and Java entities:

- **toProductResponse()**: Entity → gRPC ProductResponse
- **createProductRequestToEntity()**: CreateProductRequest → Entity
- **updateProductFromRequest()**: UpdateProductRequest → Entity

### 5. gRPC Service Implementation (ProductServiceGrpcImpl.java)

Implements unary RPC methods:

#### GetProduct RPC
```java
public void getProduct(GetProductRequest request, 
                       StreamObserver<ProductResponse> responseObserver)
```
- Validates product ID is not empty
- Fetches product from service
- Returns ProductResponse or NOT_FOUND error

#### CreateProduct RPC
```java
public void createProduct(CreateProductRequest request,
                          StreamObserver<ProductResponse> responseObserver)
```
- Validates: name (not empty), price (> 0), quantity (≥ 0)
- Creates product with auto-generated ID and timestamps
- Returns ProductResponse with created product

#### UpdateProduct RPC
```java
public void updateProduct(UpdateProductRequest request,
                          StreamObserver<ProductResponse> responseObserver)
```
- Validates product ID exists
- Updates provided fields (name, description, price, quantity, category, active)
- Preserves unmodified fields
- Updates timestamp and returns ProductResponse

#### DeleteProduct RPC
```java
public void deleteProduct(DeleteProductRequest request,
                          StreamObserver<DeleteProductResponse> responseObserver)
```
- Validates product ID exists
- Removes product from MongoDB
- Returns success status with message

### 6. Configuration

#### Application Properties
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/productdb
spring.data.mongodb.auto-index-creation=true
grpc.server.port=9090
logging.level.com.aksgrpc=DEBUG
```

#### MongoDBConfig
Enables MongoDB repository scanning for the service layer.

## Error Handling

### gRPC Status Codes

| Status | Use Case |
|--------|----------|
| INVALID_ARGUMENT | Empty ID, invalid price/quantity |
| NOT_FOUND | Product doesn't exist |
| INTERNAL | Unexpected server errors |

### Custom Exception

`ProductNotFoundException` extends `RuntimeException` and is caught by gRPC service to return NOT_FOUND status.

## Testing

### Test Classes

1. **ProductServiceGrpcImplTest.java**
   - Tests gRPC service implementation
   - Validates unary RPC behavior
   - Tests error scenarios

2. **ProductServiceTest.java**
   - Tests business logic
   - Validates CRUD operations
   - Tests search functionality

3. **ProductMapperTest.java**
   - Tests entity-to-DTO conversions
   - Validates field mapping

### Running Tests
```bash
./gradlew test
```

## API Usage Examples

### Using gRPC Client

```java
ProductServiceGrpc.ProductServiceBlockingStub stub = 
    ProductServiceGrpc.newBlockingStub(channel);

// Create Product
CreateProductRequest createReq = CreateProductRequest.newBuilder()
    .setName("Laptop")
    .setDescription("High performance laptop")
    .setPrice(1299.99)
    .setQuantity(5)
    .setCategory("Electronics")
    .build();

ProductResponse response = stub.createProduct(createReq);
System.out.println("Created product ID: " + response.getId());

// Get Product
GetProductRequest getReq = GetProductRequest.newBuilder()
    .setId(response.getId())
    .build();

ProductResponse product = stub.getProduct(getReq);

// Update Product
UpdateProductRequest updateReq = UpdateProductRequest.newBuilder()
    .setId(response.getId())
    .setPrice(1199.99)
    .setQuantity(3)
    .build();

ProductResponse updated = stub.updateProduct(updateReq);

// Delete Product
DeleteProductRequest deleteReq = DeleteProductRequest.newBuilder()
    .setId(response.getId())
    .build();

DeleteProductResponse deleteResp = stub.deleteProduct(deleteReq);
```

## Building and Running

### Build the Project
```bash
./gradlew build
```

### Run the Application
```bash
./gradlew bootRun
```

The gRPC server will start on port 9090 (configured in `application.properties`).

## MongoDB Setup

Ensure MongoDB is running locally:
```bash
# Using Docker
docker run -d -p 27017:27017 --name mongodb mongo:latest

# Or using brew (macOS)
brew services start mongodb-community
```

## Future Enhancements

The implementation is ready to support:
- Server Streaming RPCs (ListProducts, SearchProducts)
- Client Streaming RPCs (BulkCreateProducts, CalculateTotalValue)
- Bi-directional Streaming RPCs (ProductUpdates, InventorySync)

These can be implemented in `ProductServiceGrpcImpl.java` following the same pattern.

## Dependencies

Key dependencies added to build.gradle:
```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
implementation 'io.grpc:grpc-netty-shaded:${grpcVersion}'
implementation 'io.grpc:grpc-protobuf:${grpcVersion}'
implementation 'io.grpc:grpc-stub:${grpcVersion}'
implementation 'net.devh:grpc-spring-boot-starter:3.1.0.RELEASE'
implementation 'com.google.protobuf:protobuf-java:${protobufVersion}'
```

All dependencies are already configured in the build.gradle file.

