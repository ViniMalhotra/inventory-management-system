# Inventory Management System - Backend API

## Overview

The **Inventory Management System** is a robust, production-ready Spring Boot backend application designed to manage product inventory, order processing, and shipment operations. It provides REST APIs for initializing product catalogs, processing customer orders, restocking inventory, and tracking shipments.

### Primary Objectives

1. **Catalog Management**: Initialize and maintain a catalog of products with metadata (name, mass)
2. **Order Processing**: Accept and process customer orders with intelligent shipment creation
3. **Inventory Management**: Track and update stock levels for all products
4. **Shipment Optimization**: Minimize shipment count while respecting weight constraints (1.8 KG max per shipment)
5. **Pending Order Fulfillment**: Prioritize and fulfill pending orders during restock operations

---

## System Architecture

### High-Level Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                      REST API Layer (Controllers)                     │
│  - POST /v1/init_catalog       (Catalog Initialization)              │
│  - POST /v1/process_order      (Order Processing)                    │
│  - POST /v1/process_restock    (Inventory Restocking)                │
│  - GET  /v1/ship_package/{id}  (Shipment Retrieval)                 │
└─────────────────┬──────────────────────────────────────────────────┬─┘
                  │                                                  │
                  ▼                                                  ▼
    ┌──────────────────────────┐          ┌──────────────────────┐
    │   Service Layer          │          │   Business Logic     │
    ├──────────────────────────┤          ├──────────────────────┤
    │ - OrderService           │          │ - Order Validation   │
    │ - InventoryService       │          │ - Shipment Packing   │
    │ - ShipmentService        │          │ - Pending Item Mgmt  │
    └─────────────┬────────────┘          └──────────────────────┘
                  │
                  ▼
    ┌──────────────────────────────────┐
    │   Repository Layer               │
    │   (Spring Data JPA)              │
    ├──────────────────────────────────┤
    │ - ProductRepository              │
    │ - InventoryRepository            │
    │ - OrderRepository                │
    │ - OrderItemRepository            │
    │ - PendingOrderItemRepository     │
    │ - ShipmentRepository             │
    │ - ShipmentItemRepository         │
    └────────────┬─────────────────────┘
                 │
                 ▼
    ┌──────────────────────────────────┐
    │   Database (H2/PostgreSQL)       │
    │   7 Relational Tables            │
    └──────────────────────────────────┘
```

### Data Flow Diagram

#### Order Processing Flow
```
User Request (POST /process_order)
    │
    ├─▶ Validate Products Exist
    │
    ├─▶ Create Order Record
    │
    ├─▶ Create Order Items
    │
    ├─▶ ShipmentService.createShipments()
    │   ├─▶ Collect available inventory
    │   ├─▶ Optimize packing (bin packing algorithm)
    │   ├─▶ Create Shipment records
    │   ├─▶ Create ShipmentItem records
    │   ├─▶ Update OrderItem fulfillment status
    │   └─▶ Reduce Inventory
    │
    ├─▶ Create PendingOrderItem for unfulfilled portions
    │
    └─▶ Update Order Status (PENDING/PARTIALLY_FULFILLED/FULFILLED)
```

#### Restock Processing Flow
```
User Request (POST /process_restock)
    │
    ├─▶ For each restocked product:
    │   │
    │   ├─▶ Increase Inventory quantity
    │   │
    │   ├─▶ Find pending items (sorted by creation time - FIFO)
    │   │
    │   └─▶ For each pending item (oldest first):
    │       ├─▶ ShipmentService.createShipments()
    │       ├─▶ Remove fulfilled PendingOrderItems
    │       ├─▶ Update Order Status
    │       └─▶ Mark Order as COMPLETED if all pending items fulfilled
    │
    └─▶ Return summary of shipments created and orders updated
```

---

## Database Schema

### Entity Relationship Diagram

```
products (1)
    │
    ├─────────── (1:1) ─────────── inventory
    │
    ├─────────── (1:Many) ────────── order_items
    │
    └─────────── (1:Many) ────────── shipment_items

orders (1)
    │
    ├─────────── (1:Many) ────────── order_items
    │
    ├─────────── (1:Many) ────────── shipments
    │
    └─────────── (1:Many) ────────── pending_order_items

shipments (1)
    │
    └─────────── (1:Many) ────────── shipment_items
```

### Table Descriptions

#### 1. **products**
Stores product metadata including weight for shipment calculations.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| product_id | BIGINT | PRIMARY KEY | Unique product identifier |
| product_name | VARCHAR(255) | NOT NULL | Product name/description |
| mass_g | INTEGER | NOT NULL | Product weight in grams (used for shipment calculations) |

#### 2. **inventory**
Tracks current stock levels for each product. Initialized with 0 quantity during catalog setup.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| product_id | BIGINT | PRIMARY KEY, FK→products | Product identifier |
| available_qty | BIGINT | NOT NULL | Current available quantity in stock |

#### 3. **orders**
Represents customer orders with status tracking.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| order_id | BIGINT | PRIMARY KEY | Unique order identifier |
| status | VARCHAR(50) | NOT NULL | PENDING\|PARTIALLY_FULFILLED\|FULFILLED\|COMPLETED |
| created_at | TIMESTAMP | NOT NULL | Order creation timestamp |

**Status Transitions:**
- `PENDING`: Initial state, no items fulfilled
- `PARTIALLY_FULFILLED`: Some items fulfilled
- `FULFILLED`: All items have been shipped (but may have pending items)
- `COMPLETED`: All items (including previously pending) fully fulfilled

#### 4. **order_items**
Line items within an order, tracking requested vs. fulfilled quantities.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY (auto) | Unique item record identifier |
| order_id | BIGINT | NOT NULL, FK→orders | Parent order |
| product_id | BIGINT | NOT NULL, FK→products | Product in order |
| requested_qty | BIGINT | NOT NULL | Total quantity requested |
| fulfilled_qty | BIGINT | NOT NULL | Quantity already shipped |
| status | VARCHAR(50) | NOT NULL | PENDING\|PARTIALLY_FULFILLED\|FULFILLED |

#### 5. **pending_order_items**
Tracks order items that could not be fulfilled immediately due to insufficient inventory. Used for future fulfillment during restock.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY (auto) | Unique pending item record |
| order_id | BIGINT | NOT NULL | Order waiting for fulfillment |
| product_id | BIGINT | NOT NULL | Product waiting in inventory |
| pending_qty | BIGINT | NOT NULL | Quantity pending fulfillment |
| created_at | TIMESTAMP | NOT NULL | Time item became pending (used for FIFO prioritization) |

#### 6. **shipments**
Represents a single shipment package constrained to 1.8 KG weight maximum.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| shipment_id | BIGINT | PRIMARY KEY (auto) | Unique shipment identifier |
| order_id | BIGINT | NOT NULL, FK→orders | Associated order |
| total_weight_g | INTEGER | NOT NULL | Total weight in grams (≤ 1800) |
| created_at | TIMESTAMP | NOT NULL | Shipment creation timestamp |

#### 7. **shipment_items**
Individual products included in a shipment with quantities.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY (auto) | Unique shipment item record |
| shipment_id | BIGINT | NOT NULL, FK→shipments | Parent shipment |
| product_id | BIGINT | NOT NULL, FK→products | Product in shipment |
| quantity | BIGINT | NOT NULL | Units shipped of this product |

---

## Key Design Decisions

### 1. **Shipment Weight Constraint (1.8 KG Maximum)**
- **Rationale**: Physical shipping limitations require packages under 1.8 KG
- **Implementation**: Enforced during shipment creation via `ShipmentPackagingOptimizer`
- **Impact**: May result in multiple shipments per order

### 2. **Bin Packing Algorithm for Optimization**
- **Algorithm**: First-Fit Decreasing (FFD)
- **Rationale**: Minimizes total shipment count while respecting weight limits
- **Process**:
  1. Sort items by weight (heaviest first)
  2. Place each item in first bin (shipment) with sufficient capacity
  3. Create new bin if no existing bin has space
- **Complexity**: O(n log n) due to sorting
- **Benefit**: Reduces shipping costs and complexity

### 3. **Pending Order Items with FIFO Processing**
- **Rationale**: Fair and predictable order fulfillment
- **Implementation**: Order by `created_at` timestamp (oldest first)
- **Benefit**: 
  - Older pending orders get priority during restock
  - Customer expectations aligned with order timeline
  - Reduces order aging risk

### 4. **Order Status Lifecycle**
```
           ┌─────────────────────────────┐
           │                             │
           ▼                             │
    ┌─────────────┐              ┌──────────────────┐
    │   PENDING   │─────────────▶│PARTIALLY_FULFILLED│
    └─────────────┘              └────────┬─────────┘
           │                              │
           │ (all available inventory)    │ (restock fulfills rest)
           │                              │
           └─────────────────────────────▶│
                                          ▼
                                   ┌──────────────┐
                                   │  FULFILLED   │
                                   └──────────────┘
                                          │
                                   (all pending fulfilled)
                                          │
                                          ▼
                                   ┌──────────────┐
                                   │  COMPLETED   │
                                   └──────────────┘
```

### 5. **Separate Order Items vs. Pending Items Tables**
- **Rationale**: 
  - `order_items`: Captures original order request (immutable reference)
  - `pending_order_items`: Tracks evolving fulfillment status
- **Benefit**: Historical audit trail and clear fulfillment semantics

### 6. **Spring Boot & JPA Selection**
- **Spring Boot 3.1.5**: Latest stable version with modern Jakarta EE support
- **Spring Data JPA**: Eliminates boilerplate, provides query derivation
- **H2 Database**: Fast in-memory testing, PostgreSQL ready for production
- **Gradle Build**: Modern dependency management, efficient incremental builds

### 7. **REST API Versioning**
- **Path**: `/api/v1/*` ensures future API evolution without breaking clients
- **Consistency**: All endpoints use consistent request/response DTO structure
- **Error Handling**: Centralized exception handling with standard error format

---

## API Endpoints

### Base URL
```
http://localhost:8080/api
```

All endpoints return JSON responses with the following wrapper structure:
```json
{
  "success": true|false,
  "message": "descriptive message",
  "data": { ... },
  "error": "error details if success=false"
}
```

### 1. Initialize Catalog

**Endpoint:** `POST /v1/init_catalog`

**Purpose:** Creates product records and initializes inventory with 0 quantity for each product.

**Request Body:**
```json
[
  {
    "product_id": 0,
    "product_name": "RBC A+ Adult",
    "mass_g": 700
  },
  {
    "product_id": 1,
    "product_name": "RBC B+ Adult",
    "mass_g": 700
  }
]
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Catalog initialized successfully with 2 products",
  "data": "Catalog initialized successfully with 2 products"
}
```

**Error Response (500 Internal Server Error):**
```json
{
  "success": false,
  "message": "Failed to initialize catalog",
  "error": "error details"
}
```

**Side Effects:**
- Creates records in `products` table
- Creates records in `inventory` table with `available_qty = 0`

---

### 2. Process Order

**Endpoint:** `POST /v1/process_order`

**Purpose:** Creates an order, optimizes shipment packaging, and fulfills items from available inventory.

**Request Body:**
```json
{
  "order_id": 123,
  "requested": [
    {
      "product_id": 0,
      "quantity": 2
    },
    {
      "product_id": 10,
      "quantity": 4
    }
  ]
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Order processed successfully",
  "data": {
    "orderId": 123,
    "status": "PARTIALLY_FULFILLED",
    "createdAt": "2025-12-25T10:30:00",
    "totalItems": 2,
    "items": [
      {
        "productId": 0,
        "requestedQty": 2,
        "fulfilledQty": 1,
        "status": "PARTIALLY_FULFILLED"
      },
      {
        "productId": 10,
        "requestedQty": 4,
        "fulfilledQty": 0,
        "status": "PENDING"
      }
    ]
  }
}
```

**Error Responses:**

Product Not Found (400 Bad Request):
```json
{
  "success": false,
  "message": "Failed to process order",
  "error": "Product not found in inventory: 99"
}
```

Invalid Request (400 Bad Request):
```json
{
  "success": false,
  "message": "Failed to process order",
  "error": "Order already exists"
}
```

**Business Logic:**
1. Validates all requested products exist in inventory
2. Creates order record with `PENDING` status
3. Creates order_item records for each requested product
4. Calls `ShipmentService.createShipments()` which:
   - Optimizes packaging using bin-packing algorithm
   - Creates shipment records (multiple if needed for weight)
   - Creates shipment_item records
   - Updates `order_items.fulfilled_qty` and `status`
   - Reduces inventory quantities
5. Creates `pending_order_items` for any unfulfilled quantities
6. Updates order status to `FULFILLED` or `PARTIALLY_FULFILLED`

---

### 3. Process Restock

**Endpoint:** `POST /v1/process_restock`

**Purpose:** Adds inventory and fulfills pending orders in FIFO priority order.

**Request Body:**
```json
[
  {
    "product_id": 0,
    "quantity": 30
  },
  {
    "product_id": 10,
    "quantity": 5
  }
]
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Restock processed successfully",
  "data": {
    "productsRestocked": 2,
    "shipmentsCreated": 3,
    "ordersUpdated": 1
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Failed to process restock",
  "error": "Product not found: 99"
}
```

**Business Logic:**
1. For each restocked product:
   - Increases `inventory.available_qty` by restocked amount
   - Retrieves pending items ordered by `created_at` (FIFO)
   - For each pending item:
     - Gets associated order and order items
     - Calls `ShipmentService.createShipments()` to fulfill
     - Deletes fulfilled `pending_order_items`
     - Updates order status
     - Marks order `COMPLETED` if all pending items fulfilled
2. Returns summary statistics

**Note:** This endpoint handles the complex logic of:
- Respecting shipment weight constraints even for pending fulfillment
- Maintaining FIFO fairness across multiple pending orders
- Properly updating all related tables and statuses

---

### 4. Get Shipment Details

**Endpoint:** `GET /v1/ship_package/{shipmentId}`

**Purpose:** Retrieves shipment details including order and shipped items.

**URL Parameters:**
- `shipmentId` (Long): Unique shipment identifier

**Example Request:**
```
GET /v1/ship_package/1
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Shipment retrieved successfully",
  "data": {
    "orderId": 123,
    "shipped": [
      {
        "productId": 0,
        "quantity": 1
      },
      {
        "productId": 10,
        "quantity": 2
      }
    ]
  }
}
```

**Error Response (404 Not Found):**
```json
{
  "success": false,
  "message": "Failed to retrieve shipment",
  "error": "Shipment not found: 999"
}
```

**Query Logic:**
1. Retrieves shipment by ID
2. Gets all `shipment_items` for the shipment
3. Builds response with order and product quantities

---

## Implementation Details

### Project Structure
```
inventory-management-system/
├── src/main/java/com/inventory/
│   ├── InventoryManagementSystemApplication.java    (Main Spring Boot app)
│   │
│   ├── controller/
│   │   └── InventoryController.java                 (REST endpoints)
│   │
│   ├── service/
│   │   ├── OrderService.java                        (Order logic)
│   │   ├── InventoryService.java                    (Inventory logic)
│   │   └── ShipmentService.java                     (Shipment creation & optimization)
│   │
│   ├── entity/
│   │   ├── Product.java
│   │   ├── Inventory.java
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── PendingOrderItem.java
│   │   ├── Shipment.java
│   │   └── ShipmentItem.java
│   │
│   ├── repository/
│   │   ├── ProductRepository.java
│   │   ├── InventoryRepository.java
│   │   ├── OrderRepository.java
│   │   ├── OrderItemRepository.java
│   │   ├── PendingOrderItemRepository.java
│   │   ├── ShipmentRepository.java
│   │   └── ShipmentItemRepository.java
│   │
│   ├── dto/
│   │   ├── ProductDTO.java
│   │   ├── OrderRequestDTO.java
│   │   ├── OrderItemRequestDTO.java
│   │   ├── OrderResponseDTO.java
│   │   ├── OrderItemDTO.java
│   │   ├── RestockItemDTO.java
│   │   ├── RestockResponseDTO.java
│   │   ├── ShipmentResponseDTO.java
│   │   ├── ShippedItemDTO.java
│   │   └── ApiResponseDTO.java
│   │
│   ├── exception/
│   │   ├── ProductNotFoundException.java
│   │   ├── OrderNotFoundException.java
│   │   ├── ShipmentNotFoundException.java
│   │   ├── InsufficientInventoryException.java
│   │   └── GlobalExceptionHandler.java
│   │
│   └── util/
│       └── ShipmentPackagingOptimizer.java          (Bin packing algorithm)
│
├── src/main/resources/
│   └── application.yml                              (Configuration)
│
├── build.gradle                                     (Gradle build config)
├── settings.gradle
└── gradle.properties
```

### Key Classes and Their Responsibilities

#### **InventoryManagementSystemApplication**
- Entry point for Spring Boot application
- Enables component scanning and auto-configuration

#### **InventoryController**
- REST endpoint handlers for all four API operations
- Request validation and response mapping
- Error handling (delegated to GlobalExceptionHandler)

#### **OrderService**
- Creates and manages orders
- Validates product availability
- Manages order item fulfillment status
- Coordinates with ShipmentService and InventoryService
- Handles pending item creation

#### **InventoryService**
- Manages inventory levels (get, increase, reduce)
- Validates product existence
- Provides product details (mass_g for weight calculations)

#### **ShipmentService**
- Creates optimized shipments using bin-packing algorithm
- Updates order item fulfillment
- Manages shipment and shipment_item records
- Validates weight constraints

#### **ShipmentPackagingOptimizer**
- Implements First-Fit Decreasing (FFD) bin packing algorithm
- Minimizes shipment count
- Enforces 1.8 KG weight limit
- Core algorithm for shipment optimization

#### **GlobalExceptionHandler**
- Centralized exception handling
- Converts exceptions to standardized API error responses
- Provides appropriate HTTP status codes

### Transaction Management
- All service methods use `@Transactional` for ACID compliance
- Rollback on exception maintains data consistency
- Nested transactions supported for complex operations

### Performance Considerations

1. **Database Indexing:**
   - Primary keys indexed automatically
   - Foreign keys should be indexed for join performance
   - Consider composite index on (product_id, created_at) for pending items query

2. **Query Optimization:**
   - Repository methods derived from method names (Spring Data magic)
   - Lazy loading for relationships (N+1 problem mitigation)
   - Eager loading for frequently accessed relationships

3. **Caching Opportunities:**
   - Product metadata (mass_g) could be cached if frequently accessed
   - Product existence checks could use Redis for high-traffic scenarios

4. **Batch Operations:**
   - Restock with many products benefits from batch inserts
   - Consider using JdbcTemplate for bulk operations if needed

---

## Running the Application

### Prerequisites
- Java 17 or higher
- Gradle 7.x or higher (or use `./gradlew`)

### Build

```bash
cd inventory-management-system
gradle clean build
```

### Run

```bash
gradle bootRun
```

Application starts on `http://localhost:8080`

### Access H2 Console (Development)
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave blank)

### Test with cURL

Initialize catalog:
```bash
curl -X POST http://localhost:8080/api/v1/init_catalog \
  -H "Content-Type: application/json" \
  -d '[{"product_id": 0, "product_name": "RBC A+ Adult", "mass_g": 700}]'
```

Process order:
```bash
curl -X POST http://localhost:8080/api/v1/process_order \
  -H "Content-Type: application/json" \
  -d '{"order_id": 123, "requested": [{"product_id": 0, "quantity": 2}]}'
```

Process restock:
```bash
curl -X POST http://localhost:8080/api/v1/process_restock \
  -H "Content-Type: application/json" \
  -d '[{"product_id": 0, "quantity": 10}]'
```

Get shipment:
```bash
curl http://localhost:8080/api/v1/ship_package/1
```

---

## Extension Points for Future Development

### 1. **Authentication & Authorization**
- Add Spring Security for role-based access control
- Implement JWT token-based authentication
- Restrict endpoints to authorized users

### 2. **Advanced Shipment Strategies**
- Support multiple shipping tiers (e.g., standard vs. expedited)
- Implement ML-based demand forecasting
- Dynamic weight limits based on carrier tiers

### 3. **Payment Processing**
- Integrate payment gateway (Stripe, PayPal)
- Add invoice generation
- Track refunds and chargebacks

### 4. **Notification System**
- Send order confirmation emails
- Shipment tracking notifications
- Inventory low-level alerts

### 5. **Analytics & Reporting**
- Sales dashboard
- Inventory utilization reports
- Shipment cost analysis
- Order fulfillment metrics

### 6. **Distributed Caching**
- Redis integration for product catalog
- Cache invalidation strategies
- Session management

### 7. **Event-Driven Architecture**
- Kafka/RabbitMQ for async order processing
- Event sourcing for audit trail
- CQRS pattern separation

### 8. **API Rate Limiting**
- Spring Cloud Gateway for load balancing
- Rate limit configuration per client
- Quota management

### 9. **Database Optimization**
- Read replicas for reporting
- Sharding for horizontal scalability
- Archive old orders to cold storage

### 10. **Integration with External Systems**
- Carrier APIs (FedEx, UPS) for real shipping
- ERP system integration
- Multi-warehouse management

---

## Testing Strategy

### Unit Tests
- Service layer tests with mocked repositories
- Repository tests with in-memory H2 database
- Utility class tests (ShipmentPackagingOptimizer)

### Integration Tests
- Full request-to-database flow
- API endpoint contract tests
- Exception handling validation

### Performance Tests
- Load testing with high order volumes
- Shipment optimization algorithm benchmarks
- Database query performance profiling

### Test Scenarios
1. **Happy Path**: Normal order → shipment → restock flow
2. **Edge Cases**:
   - Order larger than initial inventory
   - Product not in catalog
   - Duplicate order ID
   - Shipment weight edge (exactly 1.8 KG)
3. **Concurrency**: Multiple orders and restocks simultaneously
4. **Data Consistency**: Verify inventory, order item, and shipment counts match

---

## Troubleshooting Guide

### Issue: Order not created
**Possible Causes:**
- Product not in inventory catalog
- Invalid product_id in request
- Database connectivity issues

**Solution:**
- Verify products exist in inventory table
- Check application logs for detailed error messages
- Confirm database is running

### Issue: Shipments not created
**Possible Causes:**
- Zero inventory for requested products
- Item weight exceeds 1.8 KG
- Missing order items

**Solution:**
- Run restock to populate inventory
- Check product mass_g values
- Verify order was successfully created

### Issue: Pending items not fulfilled on restock
**Possible Causes:**
- Pending items deleted prematurely
- Insufficient restocked quantity
- Order not found

**Solution:**
- Check pending_order_items table contents
- Verify restock quantity matches pending demand
- Ensure order records exist

---

## Production Deployment Considerations

### Database
- Switch from H2 to PostgreSQL 14+
- Configure connection pooling (HikariCP)
- Enable SSL for database connections
- Implement automated backups

### Environment Configuration
- Use environment variables for secrets
- Separate configuration for dev/test/prod
- Implement feature flags

### Monitoring & Logging
- Centralized logging (ELK stack)
- Application metrics (Micrometer + Prometheus)
- Distributed tracing (Sleuth + Zipkin)
- Health check endpoints

### Scaling
- Horizontal scaling with load balancer
- Stateless application design
- Database read replicas
- Distributed caching layer

### Security
- Input validation on all endpoints
- Rate limiting per IP/user
- Encrypted data in transit (HTTPS/TLS)
- Regular security audits and penetration testing
- Secrets management (HashiCorp Vault)

---

## Key Metrics & KPIs

1. **Order Fulfillment Rate**: % of orders completely fulfilled
2. **Shipment Efficiency**: Average shipments per order (lower is better)
3. **Pending Item Resolution Time**: Average days for pending items to be fulfilled
4. **Inventory Utilization**: % of inventory actually shipped
5. **API Response Time**: p50/p95/p99 latencies
6. **System Availability**: Uptime percentage
7. **Cost per Shipment**: Average shipping cost per package

---

## References & Documentation

### Spring Boot & JPA
- [Spring Boot 3.1 Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [Hibernate ORM Guide](https://hibernate.org/orm/)

### Algorithms
- [First-Fit Decreasing Bin Packing](https://en.wikipedia.org/wiki/Bin_packing_problem)

### REST API Design
- [RESTful Web Services](https://restfulapi.net/)
- [HTTP Status Codes](https://httpwww.org/http-status-codes)

---

## Contributing Guidelines

### Code Style
- Follow Google Java Style Guide
- Use meaningful variable names
- Add comprehensive JavaDoc comments
- Keep methods focused and testable

### Commit Standards
- Use conventional commits (feat:, fix:, docs:, test:)
- Reference issue numbers
- Write descriptive commit messages

### Pull Request Process
1. Create feature branch from `main`
2. Write tests for new functionality
3. Ensure all tests pass locally
4. Submit PR with detailed description
5. Address review comments
6. Squash commits if needed

---

## License

This project is licensed under the MIT License. See LICENSE file for details.

---

## Contact & Support

For issues, questions, or suggestions:
- Email: support@inventory-system.dev
- GitHub Issues: [Repository Issues](https://github.com/yourorg/inventory-management-system/issues)
- Slack: #inventory-system-dev

---

**Last Updated:** December 25, 2025  
**Version:** 1.0.0  
**Status:** Production Ready
