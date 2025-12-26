# Project Index - Inventory Management System

## 📋 Table of Contents
1. [Project Overview](#project-overview)
2. [Documentation Files](#documentation-files)
3. [Source Code Structure](#source-code-structure)
4. [Build & Configuration](#build--configuration)
5. [Getting Started](#getting-started)
6. [API Reference](#api-reference)

---

## Project Overview

**Inventory Management System** - A Spring Boot REST API for managing blood product inventory with pessimistic locking, intelligent shipment optimization using FFD algorithm, and FIFO-based order fulfillment.

- **Framework:** Spring Boot 3.1.5
- **Language:** Java 17
- **Build Tool:** Gradle
- **Database:** H2 (dev/test), PostgreSQL (production)
- **Architecture:** Monolithic REST API with 3-layer design

---

## Documentation Files

### Quick Start & Overview
- **[README.md](README.md)** - Main project documentation with high-level overview, setup instructions, and API basics
- **[QUICKSTART.md](QUICKSTART.md)** - Quick start guide to get the application running
- **[INDEX.md](INDEX.md)** - Alternative index document

### API Documentation
- **[API_QUICK_REFERENCE.md](API_QUICK_REFERENCE.md)** - Quick reference for all API endpoints
- **[API_TESTING.md](API_TESTING.md)** - Complete curl command examples for testing all 4 API endpoints with 13-product blood catalog
- **[COMPLETE_API_TEST_RESULTS.md](COMPLETE_API_TEST_RESULTS.md)** - Detailed results from full API test execution

### Architecture & Design
- **[ARCHITECTURE_DECISIONS.md](ARCHITECTURE_DECISIONS.md)** - 17 major architectural decisions with rationales
- **[SYSTEM_DIAGRAMS.md](SYSTEM_DIAGRAMS.md)** - Entity relationships, data flow, and system architecture diagrams
- **[OVERVIEW.md](OVERVIEW.md)** - Project metrics, statistics, and high-level architecture

### Implementation & Progress
- **[IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)** - Summary of implemented features and components
- **[PROJECT_COMPLETION_REPORT.md](PROJECT_COMPLETION_REPORT.md)** - Final completion status and validation results
- **[BUILD_REPORT.md](BUILD_REPORT.md)** - Build process information and configuration details
- **[DELIVERABLES.md](DELIVERABLES.md)** - Specification and checklist of project deliverables

---

## Source Code Structure

```
src/
├── main/
│   ├── java/com/inventory/
│   │   ├── controller/
│   │   │   ├── InventoryController.java          # REST endpoints for inventory operations
│   │   │   ├── OrderController.java              # REST endpoints for order management
│   │   │   └── ShipmentController.java           # REST endpoints for shipment queries
│   │   │
│   │   ├── service/
│   │   │   ├── InventoryService.java             # Inventory management with pessimistic locking
│   │   │   ├── OrderService.java                 # Order orchestration and shipment generation
│   │   │   ├── ShipmentService.java              # Shipment creation and management
│   │   │   └── ShipmentPackagingOptimizer.java   # FFD bin packing algorithm
│   │   │
│   │   ├── repository/
│   │   │   ├── InventoryRepository.java          # Inventory data access with @Lock annotations
│   │   │   ├── OrderRepository.java              # Order persistence
│   │   │   ├── OrderItemRepository.java          # Order item persistence
│   │   │   ├── ShipmentRepository.java           # Shipment persistence
│   │   │   ├── ShipmentItemRepository.java       # Shipment item persistence
│   │   │   ├── ProductRepository.java            # Product persistence
│   │   │   └── PendingOrderItemRepository.java   # Pending order tracking
│   │   │
│   │   ├── entity/
│   │   │   ├── Inventory.java                    # Inventory entity with version control
│   │   │   ├── Order.java                        # Order entity with cascading relationships
│   │   │   ├── OrderItem.java                    # Order line items
│   │   │   ├── Shipment.java                     # Shipment entity
│   │   │   ├── ShipmentItem.java                 # Shipment contents
│   │   │   ├── Product.java                      # Product catalog
│   │   │   └── PendingOrderItem.java             # FIFO pending order tracking
│   │   │
│   │   ├── dto/
│   │   │   ├── ProductInitializationDto.java     # Catalog initialization request DTO
│   │   │   ├── OrderRequestDto.java              # Order creation request DTO
│   │   │   ├── RestockRequestDto.java            # Restock processing request DTO
│   │   │   └── ApiResponse.java                  # Consistent API response wrapper
│   │   │
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java       # Centralized exception handling
│   │   │   ├── ResourceNotFoundException.java    # Entity not found exception
│   │   │   └── InvalidOperationException.java    # Business logic violation exception
│   │   │
│   │   └── InventoryManagementApplication.java   # Spring Boot main application class
│   │
│   └── resources/
│       ├── application.yml                       # Spring Boot configuration
│       └── application-prod.yml                  # Production profile configuration
│
└── test/
    └── java/com/inventory/service/
        ├── InventoryServiceTest.java             # 8 nested test classes for InventoryService
        ├── OrderServiceTest.java                 # 6 nested test classes for OrderService
        └── ShipmentServiceTest.java              # 3 nested test classes for ShipmentService
```

### Key Components

| Component | File | Purpose | Key Methods |
|-----------|------|---------|-------------|
| **Inventory Control** | `InventoryService.java` | Manages stock levels with pessimistic locking | `reduceInventory()`, `increaseInventory()`, `getAvailableQuantity()` |
| **Order Processing** | `OrderService.java` | Orchestrates order creation and fulfillment | `processOrder()`, `updateOrderStatus()`, `getOrdersByStatus()` |
| **Shipment Optimization** | `ShipmentPackagingOptimizer.java` | FFD bin packing algorithm | `optimizePackaging()`, handles weight constraints |
| **Locking Strategy** | `InventoryRepository.java` | Pessimistic database locks | `@Lock(PESSIMISTIC_WRITE)` annotations |
| **API Gateway** | `InventoryController.java` | REST endpoint routing | `initCatalog()`, `processOrder()`, `processRestock()` |

---

## Build & Configuration

### Build Files
- **[build.gradle](build.gradle)** - Gradle build configuration with Spring Boot, JPA, H2, PostgreSQL drivers
- **[gradle.properties](gradle.properties)** - Gradle properties and version definitions
- **[settings.gradle](settings.gradle)** - Gradle multi-project settings
- **[gradlew](gradlew)** - Gradle wrapper for Unix/Linux/Mac
- **[gradlew.bat](gradlew.bat)** - Gradle wrapper for Windows

### Configuration Files
- **[src/main/resources/application.yml](src/main/resources/application.yml)** - Spring Boot application configuration
  - Server port: 8080
  - H2 database configuration
  - JPA/Hibernate settings
  - Logging configuration

---

## Getting Started

### Prerequisites
- Java 17 or higher
- Gradle 7.0+
- Git

### Quick Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/inventory-management-system.git
cd inventory-management-system

# Build the project
./gradlew clean build

# Run the application
./gradlew bootRun

# Application starts on http://localhost:8080
```

### Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests InventoryServiceTest

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Build Output
- **JAR File:** `build/libs/inventory-management-system-*.jar`
- **Test Results:** `build/test-results/test/`
- **Test Reports:** `build/reports/tests/test/`

---

## API Reference

### Overview
The API provides 4 main endpoints for managing blood product inventory:

### 1. Initialize Catalog
**Endpoint:** `POST /api/v1/init_catalog`
- Initialize product catalog with up to 13 blood product types
- Request: Array of ProductInitializationDto
- Response: ApiResponse with success status and 13 products initialized

**Example Curl:**
```bash
curl -X POST http://localhost:8080/api/v1/init_catalog \
  -H "Content-Type: application/json" \
  -d '[{"productId": 0, "productName": "RBC A+ Adult", "massG": 700}, ...]'
```

### 2. Process Order
**Endpoint:** `POST /api/v1/process_order`
- Create a new order with specified items
- Request: OrderRequestDto with orderId, items (productId, quantity)
- Response: ApiResponse with order details and status (PENDING, FULFILLED, etc.)

**Example Curl:**
```bash
curl -X POST http://localhost:8080/api/v1/process_order \
  -H "Content-Type: application/json" \
  -d '{"orderId": 123, "items": [{"productId": 0, "quantity": 2}, ...]}'
```

### 3. Process Restock
**Endpoint:** `POST /api/v1/process_restock`
- Add inventory to all products and automatically generate optimized shipments
- Request: RestockRequestDto with quantities for each product
- Response: ApiResponse with shipments created and weights optimized

**Example Curl:**
```bash
curl -X POST http://localhost:8080/api/v1/process_restock \
  -H "Content-Type: application/json" \
  -d '{"restockQuantities": [{"productId": 0, "quantity": 10}, ...]}'
```

### 4. Get Shipment Details
**Endpoint:** `GET /api/v1/ship_package/{shipmentId}`
- Retrieve detailed information about a specific shipment
- Response: ApiResponse with shipment contents, weight, and order association

**Example Curl:**
```bash
curl -X GET http://localhost:8080/api/v1/ship_package/1
```

### Response Format
All API responses follow a consistent wrapper format:
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { /* response payload */ },
  "error": null
}
```

### Error Handling
- **404 Not Found:** Resource doesn't exist
- **400 Bad Request:** Invalid input or business logic violation
- **500 Internal Server Error:** Unexpected server error

---

## Key Design Patterns

### Pessimistic Locking
- **Technology:** `@Lock(LockModeType.PESSIMISTIC_WRITE)` on repository methods
- **Purpose:** Prevents race conditions during concurrent inventory operations
- **Implementation:** `findByIdWithLock()` and `findByProductIdInWithLock()` in InventoryRepository
- **SQL:** Translates to `SELECT ... FOR UPDATE` in PostgreSQL/MySQL

### FFD Bin Packing Algorithm
- **Location:** `ShipmentPackagingOptimizer.java`
- **Purpose:** Minimize number of shipments while respecting 1800g weight limit
- **Strategy:** First-Fit Decreasing - sort items by weight (heaviest first), fit into first available bin
- **Feature:** Automatic item splitting for oversized products

### FIFO Order Fulfillment
- **Location:** `OrderService.java`, `PendingOrderItemRepository.java`
- **Purpose:** Process pending orders in FIFO order using timestamps
- **Implementation:** `ORDER BY createdAt ASC` in SQL queries

### API Response Wrapper
- **Location:** `ApiResponse.java`, controllers
- **Purpose:** Consistent JSON response format across all endpoints
- **Fields:** success (boolean), message (String), data (Object), error (String)

---

## Database Schema

### Core Entities (7 tables)
1. **Inventory** - Stock levels per product
2. **Order** - Order header information
3. **OrderItem** - Individual line items in orders
4. **Shipment** - Shipment header and weight tracking
5. **ShipmentItem** - Individual items within shipments
6. **Product** - Product catalog
7. **PendingOrderItem** - FIFO queue for pending fulfillment

### Relationships
- Order → OrderItems (1:N, cascading delete)
- Order → Shipments (1:N, cascading delete)
- Shipment → ShipmentItems (1:N, cascading delete)
- OrderItem → Product (N:1)
- ShipmentItem → Product (N:1)
- ShipmentItem → OrderItem (N:1)
- Inventory → Product (1:1)

---

## Performance Characteristics

| Operation | Complexity | Notes |
|-----------|-----------|-------|
| Get Available Quantity | O(1) | Direct database lock and retrieval |
| Batch Load Inventory | O(1) | Single query for multiple products |
| FFD Bin Packing | O(n log n) | Weight sorting + first-fit fitting |
| FIFO Fulfillment | O(n) | Single ORDER BY query with timestamp |
| Shipment Creation | O(n) | One shipment created per FFD result |

---

## Testing

### Test Coverage
- **InventoryServiceTest** - 8 nested test classes (GetAvailableQuantityTests, ReduceInventoryTests, etc.)
- **OrderServiceTest** - 6 nested test classes (ProcessOrderTests, UpdateOrderStatusTests, etc.)
- **ShipmentServiceTest** - 3 nested test classes (CreateShipmentsTests, etc.)

### Test Execution Results
All tests pass with 100% success rate. See [COMPLETE_API_TEST_RESULTS.md](COMPLETE_API_TEST_RESULTS.md) for detailed results.

---

## Deployment

### Development
```bash
./gradlew bootRun
```

### Production
```bash
# Build JAR
./gradlew build -x test

# Run JAR
java -jar build/libs/inventory-management-system-*.jar
```

### Docker
A Dockerfile can be generated using the project structure. The application is containerization-ready.

---

## Contributors
- **Author:** Kedar Nath Kurnool Gandla
- **Email:** vini.malhotra2@gmail.com

---

## License
This project is provided as-is for demonstration and educational purposes.

---

## Additional Resources

For more detailed information, see:
- **Architecture Decisions:** [ARCHITECTURE_DECISIONS.md](ARCHITECTURE_DECISIONS.md)
- **System Diagrams:** [SYSTEM_DIAGRAMS.md](SYSTEM_DIAGRAMS.md)
- **API Testing:** [API_TESTING.md](API_TESTING.md)
- **Implementation Details:** [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
