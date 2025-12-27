# gRPC Product Service

A Spring Boot microservice demonstrating all four gRPC communication patterns with MongoDB persistence.

## Tech Stack

- **Java 21**
- **Spring Boot 3.4.1**
- **gRPC** (grpc-spring-boot-starter)
- **MongoDB**
- **Gradle**

## Quick Start

### Prerequisites

- Java 21+
- MongoDB running on `localhost:27017`

### Run the Server

```bash
./gradlew bootRun
```

Server starts on **gRPC port 9090**

### Run Tests

```bash
./gradlew test
```

---

## API Reference

### gRPC Endpoint

```
localhost:9090
```

---

## 1. Unary RPCs (Single Request → Single Response)

### GetProduct

Retrieve a product by ID.

```protobuf
rpc GetProduct(GetProductRequest) returns (ProductResponse)
```

**Request:**
```json
{
  "id": "product-id-here"
}
```

**Response:**
```json
{
  "id": "abc123",
  "name": "MacBook Pro",
  "description": "Apple laptop",
  "price": 1999.99,
  "quantity": 10,
  "category": "Electronics",
  "active": true,
  "created_at": 1703721600000,
  "updated_at": 1703721600000
}
```

---

### CreateProduct

Create a new product.

```protobuf
rpc CreateProduct(CreateProductRequest) returns (ProductResponse)
```

**Request:**
```json
{
  "name": "MacBook Pro",
  "description": "Apple laptop M3 Max",
  "price": 1999.99,
  "quantity": 10,
  "category": "Electronics"
}
```

---

### UpdateProduct

Update an existing product.

```protobuf
rpc UpdateProduct(UpdateProductRequest) returns (ProductResponse)
```

**Request:**
```json
{
  "id": "product-id-here",
  "name": "Updated Name",
  "description": "Updated description",
  "price": 2499.99,
  "quantity": 5,
  "category": "Electronics",
  "active": true
}
```

---

### DeleteProduct

Delete a product by ID.

```protobuf
rpc DeleteProduct(DeleteProductRequest) returns (DeleteProductResponse)
```

**Request:**
```json
{
  "id": "product-id-here"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Product deleted successfully"
}
```

---

## 2. Server Streaming RPCs (Single Request → Stream of Responses)

### ListProducts

List products with optional filters. Server streams products one by one.

```protobuf
rpc ListProducts(ListProductsRequest) returns (stream ProductResponse)
```

**Request:**
```json
{
  "category": "Electronics",
  "active_only": true,
  "page_size": 10
}
```

**Response:** Stream of `ProductResponse` messages

---

### SearchProducts

Search products by name with optional filters.

```protobuf
rpc SearchProducts(SearchProductsRequest) returns (stream ProductResponse)
```

**Request:**
```json
{
  "query": "MacBook",
  "max_price": 3000.0,
  "min_quantity": 5
}
```

**Response:** Stream of matching `ProductResponse` messages

---

## 3. Client Streaming RPCs (Stream of Requests → Single Response)

### BulkCreateProducts

Create multiple products in a single stream. Server returns summary after all products are received.

```protobuf
rpc BulkCreateProducts(stream CreateProductRequest) returns (BulkCreateResponse)
```

**Request:** Stream of `CreateProductRequest` messages

**Response:**
```json
{
  "total_received": 5,
  "total_created": 4,
  "total_failed": 1,
  "created_ids": ["id1", "id2", "id3", "id4"],
  "error_messages": ["Product name cannot be empty"]
}
```

---

### CalculateTotalValue

Calculate total inventory value by streaming product IDs.

```protobuf
rpc CalculateTotalValue(stream ProductIdRequest) returns (TotalValueResponse)
```

**Request:** Stream of product IDs
```json
{"id": "product-1"}
{"id": "product-2"}
{"id": "product-3"}
```

**Response:**
```json
{
  "product_count": 3,
  "total_value": 15000.00,
  "average_price": 5000.00
}
```

---

## 4. Bi-Directional Streaming RPCs (Stream ↔ Stream)

### ProductUpdates

Real-time CRUD operations. Send any action (CREATE/UPDATE/DELETE/GET) and receive immediate responses.

```protobuf
rpc ProductUpdates(stream ProductUpdateRequest) returns (stream ProductUpdateResponse)
```

**Request (CREATE):**
```json
{
  "request_id": "req-1",
  "create": {
    "name": "iPhone 15",
    "price": 999.99,
    "quantity": 20,
    "category": "Electronics"
  }
}
```

**Request (GET):**
```json
{
  "request_id": "req-2",
  "get": {
    "id": "product-id-here"
  }
}
```

**Request (UPDATE):**
```json
{
  "request_id": "req-3",
  "update": {
    "id": "product-id-here",
    "name": "Updated Name",
    "price": 899.99
  }
}
```

**Request (DELETE):**
```json
{
  "request_id": "req-4",
  "delete": {
    "id": "product-id-here"
  }
}
```

**Response:**
```json
{
  "request_id": "req-1",
  "success": true,
  "message": "Product created successfully",
  "product": { ... },
  "server_timestamp": 1703721600000
}
```

---

### InventorySync

Real-time inventory adjustments with validation.

```protobuf
rpc InventorySync(stream InventorySyncRequest) returns (stream InventorySyncResponse)
```

**Request:**
```json
{
  "product_id": "product-id-here",
  "quantity_change": 10,
  "reason": "Restock from warehouse",
  "timestamp": 1703721600000
}
```

Use negative values to decrease inventory:
```json
{
  "product_id": "product-id-here",
  "quantity_change": -5,
  "reason": "Sale",
  "timestamp": 1703721600000
}
```

**Response:**
```json
{
  "product_id": "product-id-here",
  "previous_quantity": 50,
  "new_quantity": 60,
  "success": true,
  "message": "Inventory updated successfully. Reason: Restock from warehouse",
  "server_timestamp": 1703721600000
}
```

---

## Testing with Postman

1. Create a new **gRPC Request**
2. Enter server URL: `localhost:9090`
3. Import `src/main/proto/product.proto`
4. Select the method and invoke

## Testing with grpcurl

```bash
# List services
grpcurl -plaintext localhost:9090 list

# Get a product
grpcurl -plaintext -d '{"id": "your-product-id"}' \
  localhost:9090 product.ProductService/GetProduct

# Create a product
grpcurl -plaintext -d '{"name": "Test", "price": 99.99, "quantity": 10}' \
  localhost:9090 product.ProductService/CreateProduct

# List products (server streaming)
grpcurl -plaintext -d '{"category": "Electronics"}' \
  localhost:9090 product.ProductService/ListProducts
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/aksgrpc/server/
│   │   ├── ServerApplication.java
│   │   ├── config/
│   │   │   └── MongoDBConfig.java
│   │   ├── entity/
│   │   │   └── Product.java
│   │   ├── exception/
│   │   │   └── ProductNotFoundException.java
│   │   ├── grpc/
│   │   │   └── ProductServiceGrpcImpl.java
│   │   ├── mapper/
│   │   │   └── ProductMapper.java
│   │   ├── repository/
│   │   │   └── ProductRepository.java
│   │   └── service/
│   │       └── ProductService.java
│   ├── proto/
│   │   └── product.proto
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/aksgrpc/server/
        ├── ServerApplicationTests.java
        ├── grpc/
        │   └── ProductServiceGrpcImplTest.java
        ├── mapper/
        │   └── ProductMapperTest.java
        └── service/
            └── ProductServiceTest.java
```

---

## Configuration

```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/productdb

# gRPC Server
grpc.server.port=9090
```

---

## gRPC Communication Patterns Summary

| Pattern | Request | Response | Use Case |
|---------|---------|----------|----------|
| **Unary** | Single | Single | Simple CRUD operations |
| **Server Streaming** | Single | Stream | Lists, search results, real-time feeds |
| **Client Streaming** | Stream | Single | Bulk uploads, aggregations |
| **Bi-Directional** | Stream | Stream | Chat, real-time sync, interactive operations |
