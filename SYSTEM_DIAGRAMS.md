# System Diagrams (PlantUML Format)

This file contains PlantUML diagrams that can be rendered using:
- PlantUML online editor: https://www.plantuml.com/plantuml/uml
- VS Code PlantUML extension
- Any PlantUML-compatible tool

## 1. Class Diagram

```plantuml
@startuml class_diagram
!define ENTITY_COLOR #FFE6E6
!define SERVICE_COLOR #E6F3FF
!define REPOSITORY_COLOR #E6FFE6
!define DTO_COLOR #FFF9E6
!define EXCEPTION_COLOR #FFE6F3

package "Entity Layer" {
    class Product <<entity>> #ENTITY_COLOR {
        - productId: Long
        - productName: String
        - massG: Integer
        + getters/setters()
    }

    class Inventory <<entity>> #ENTITY_COLOR {
        - productId: Long
        - availableQty: Long
        + getters/setters()
    }

    class Order <<entity>> #ENTITY_COLOR {
        - orderId: Long
        - status: String
        - createdAt: LocalDateTime
        + getters/setters()
    }

    class OrderItem <<entity>> #ENTITY_COLOR {
        - id: Long
        - orderId: Long
        - productId: Long
        - requestedQty: Long
        - fulfilledQty: Long
        - status: String
        + getters/setters()
    }

    class PendingOrderItem <<entity>> #ENTITY_COLOR {
        - id: Long
        - orderId: Long
        - productId: Long
        - pendingQty: Long
        - createdAt: LocalDateTime
        + getters/setters()
    }

    class Shipment <<entity>> #ENTITY_COLOR {
        - shipmentId: Long
        - orderId: Long
        - totalWeightG: Integer
        - createdAt: LocalDateTime
        + getters/setters()
    }

    class ShipmentItem <<entity>> #ENTITY_COLOR {
        - id: Long
        - shipmentId: Long
        - productId: Long
        - quantity: Long
        + getters/setters()
    }
}

package "Service Layer" {
    class OrderService <<service>> #SERVICE_COLOR {
        - orderRepository
        - inventoryService
        - shipmentService
        + processOrder()
        + updateOrderStatus()
        + completeOrderIfAllFulfilled()
    }

    class InventoryService <<service>> #SERVICE_COLOR {
        - inventoryRepository
        - productRepository
        + getAvailableQuantity()
        + reduceInventory()
        + increaseInventory()
        + getProductDetails()
    }

    class ShipmentService <<service>> #SERVICE_COLOR {
        - shipmentRepository
        - inventoryService
        + createShipments()
        + getShipment()
        + getShipmentItems()
    }
}

package "Repository Layer" {
    interface ProductRepository <<repository>> #REPOSITORY_COLOR {
    }

    interface InventoryRepository <<repository>> #REPOSITORY_COLOR {
        + findByProductIdIn()
    }

    interface OrderRepository <<repository>> #REPOSITORY_COLOR {
    }

    interface OrderItemRepository <<repository>> #REPOSITORY_COLOR {
        + findByOrderId()
        + findByOrderIdAndStatus()
    }

    interface PendingOrderItemRepository <<repository>> #REPOSITORY_COLOR {
        + findByProductIdOrderByCreatedAt()
        + findByOrderId()
    }

    interface ShipmentRepository <<repository>> #REPOSITORY_COLOR {
        + findByOrderId()
    }

    interface ShipmentItemRepository <<repository>> #REPOSITORY_COLOR {
        + findByShipmentId()
    }
}

package "Utility Layer" {
    class ShipmentPackagingOptimizer <<utility>> #FFF9E6 {
        - MAX_SHIPMENT_WEIGHT_G: int
        + optimizePackaging()
        + canFitInSingleShipment()
        + getMaxShipmentWeightG()
    }
}

package "Exception Layer" {
    class ProductNotFoundException #FFE6F3 {
    }

    class OrderNotFoundException #FFE6F3 {
    }

    class ShipmentNotFoundException #FFE6F3 {
    }

    class GlobalExceptionHandler #FFE6F3 {
        + handleProductNotFound()
        + handleOrderNotFound()
        + handleShipmentNotFound()
    }
}

' Relationships
Product "1" -- "1" Inventory
Order "1" -- "*" OrderItem
Order "1" -- "*" Shipment
Order "1" -- "*" PendingOrderItem
Shipment "1" -- "*" ShipmentItem
Product "1" -- "*" OrderItem
Product "1" -- "*" ShipmentItem

OrderService --> OrderRepository
OrderService --> InventoryService
OrderService --> ShipmentService

InventoryService --> InventoryRepository
InventoryService --> ProductRepository

ShipmentService --> ShipmentRepository
ShipmentService --> ShipmentItemRepository
ShipmentService --> InventoryService
ShipmentService --> ShipmentPackagingOptimizer

@enduml
```

## 2. Sequence Diagram - Process Order

```plantuml
@startuml sequence_process_order
participant Client
participant InventoryController
participant OrderService
participant InventoryService
participant ShipmentService
participant ShipmentPackagingOptimizer
database Database

Client -> InventoryController: POST /process_order
InventoryController -> OrderService: processOrder(request)

OrderService -> InventoryService: Validate products exist
InventoryService -> Database: Check products in inventory
Database --> InventoryService: Return products
InventoryService --> OrderService: Validation OK

OrderService -> Database: Create Order record
OrderService -> Database: Create OrderItem records

OrderService -> ShipmentService: createShipments(orderId, items)
ShipmentService -> InventoryService: Get available quantities
InventoryService -> Database: Query inventory
Database --> InventoryService: Available quantities
InventoryService --> ShipmentService: Return quantities

ShipmentService -> ShipmentPackagingOptimizer: optimizePackaging(items)
ShipmentPackagingOptimizer -> ShipmentPackagingOptimizer: FFD algorithm
ShipmentPackagingOptimizer --> ShipmentService: Optimized packages

loop For each package
    ShipmentService -> Database: Create Shipment
    ShipmentService -> Database: Create ShipmentItems
    ShipmentService -> InventoryService: reduceInventory()
    InventoryService -> Database: Update inventory
    ShipmentService -> Database: Update OrderItem status
end

OrderService -> Database: Create PendingOrderItem (if any)
OrderService -> OrderService: updateOrderStatus()
OrderService -> Database: Update Order status

OrderService --> InventoryController: Return Order details
InventoryController --> Client: 200 OK (Order + Items + Status)

@enduml
```

## 3. Sequence Diagram - Process Restock

```plantuml
@startuml sequence_process_restock
participant Client
participant InventoryController
participant OrderService
participant InventoryService
participant ShipmentService
database Database

Client -> InventoryController: POST /process_restock
InventoryController -> OrderService: For each restock item

loop For each restocked product
    OrderService -> InventoryService: increaseInventory(productId, qty)
    InventoryService -> Database: Update inventory quantity
    
    OrderService -> Database: Find pending items (ORDER BY created_at)
    Database --> OrderService: Pending items (FIFO)
    
    loop For each pending item
        OrderService -> Database: Get Order and OrderItems
        OrderService -> ShipmentService: createShipments()
        ShipmentService -> ShipmentService: Optimize & create shipments
        ShipmentService -> Database: Create shipments
        
        OrderService -> Database: Delete fulfilled PendingOrderItem
        OrderService -> OrderService: updateOrderStatus()
        OrderService -> OrderService: completeOrderIfAllFulfilled()
    end
end

OrderService --> InventoryController: Return summary (shipments, orders updated)
InventoryController --> Client: 200 OK (RestockResponse)

@enduml
```

## 4. State Diagram - Order Status Transitions

```plantuml
@startuml state_order_status
[*] --> PENDING: Create Order

PENDING --> PARTIALLY_FULFILLED: Some items shipped\nvia createShipments()
PENDING --> FULFILLED: All items shipped\n(no pending items)

PARTIALLY_FULFILLED --> FULFILLED: All items shipped\nvia restock

FULFILLED --> COMPLETED: All pending items\nfulfilled via restock

state COMPLETED {
    [*] --> FinalState
}

note right of PENDING
    Initial state when order created
    No items fulfilled yet
end note

note right of PARTIALLY_FULFILLED
    Some items shipped, rest pending
    Waiting for restock
end note

note right of FULFILLED
    All items shipped
    May still have pending items
end note

note right of COMPLETED
    All items (including previously pending)
    completely fulfilled
    Order finalized
end note

@enduml
```

## 5. Entity Relationship Diagram

```plantuml
@startuml erd
entity "products" as products {
    *product_id: BIGINT
    --
    product_name: VARCHAR
    mass_g: INTEGER
}

entity "inventory" as inventory {
    *product_id: BIGINT <<FK>>
    --
    available_qty: BIGINT
}

entity "orders" as orders {
    *order_id: BIGINT
    --
    status: VARCHAR
    created_at: TIMESTAMP
}

entity "order_items" as order_items {
    *id: BIGINT
    --
    order_id: BIGINT <<FK>>
    product_id: BIGINT <<FK>>
    requested_qty: BIGINT
    fulfilled_qty: BIGINT
    status: VARCHAR
}

entity "pending_order_items" as pending_order_items {
    *id: BIGINT
    --
    order_id: BIGINT
    product_id: BIGINT
    pending_qty: BIGINT
    created_at: TIMESTAMP
}

entity "shipments" as shipments {
    *shipment_id: BIGINT
    --
    order_id: BIGINT <<FK>>
    total_weight_g: INTEGER
    created_at: TIMESTAMP
}

entity "shipment_items" as shipment_items {
    *id: BIGINT
    --
    shipment_id: BIGINT <<FK>>
    product_id: BIGINT <<FK>>
    quantity: BIGINT
}

products ||--|| inventory: "1:1"
orders ||--o{ order_items: "1:*"
orders ||--o{ shipments: "1:*"
orders ||--o{ pending_order_items: "1:*"
shipments ||--o{ shipment_items: "1:*"
products ||--o{ order_items: "1:*"
products ||--o{ shipment_items: "1:*"

@enduml
```

## 6. Component Diagram - System Architecture

```plantuml
@startuml component_architecture
package "REST API Layer" {
    component [InventoryController] as controller
}

package "Service Layer" {
    component [OrderService] as orderService
    component [InventoryService] as inventoryService
    component [ShipmentService] as shipmentService
}

package "Repository Layer" {
    component [ProductRepository] as productRepo
    component [InventoryRepository] as inventoryRepo
    component [OrderRepository] as orderRepo
    component [OrderItemRepository] as orderItemRepo
    component [PendingOrderItemRepository] as pendingRepo
    component [ShipmentRepository] as shipmentRepo
    component [ShipmentItemRepository] as shipmentItemRepo
}

package "Utility Layer" {
    component [ShipmentPackagingOptimizer] as optimizer
}

package "Database" {
    component [H2/PostgreSQL] as database
}

controller --> orderService: uses
controller --> inventoryService: uses
controller --> shipmentService: uses

orderService --> inventoryService: uses
orderService --> shipmentService: uses
orderService --> orderRepo: uses
orderService --> orderItemRepo: uses
orderService --> pendingRepo: uses

inventoryService --> inventoryRepo: uses
inventoryService --> productRepo: uses

shipmentService --> shipmentRepo: uses
shipmentService --> shipmentItemRepo: uses
shipmentService --> inventoryService: uses
shipmentService --> optimizer: uses

productRepo --> database: "CRUD"
inventoryRepo --> database: "CRUD"
orderRepo --> database: "CRUD"
orderItemRepo --> database: "CRUD"
pendingRepo --> database: "CRUD"
shipmentRepo --> database: "CRUD"
shipmentItemRepo --> database: "CRUD"

@enduml
```

## 7. Activity Diagram - FFD Bin Packing Algorithm

```plantuml
@startuml activity_ffd_algorithm
start
:Input: List of items to pack;
:Max bin capacity: 1800g;

:Sort items by weight (descending);

partition "For each item" {
    :Item ← Next item from sorted list;
    
    if (Existing bin with space?) then (yes)
        :Place item in first bin with capacity;
    else (no)
        :Create new bin;
        :Place item in new bin;
    endif
    
    if (More items?) then (yes)
        :Continue;
    else (no)
        :Done;
    endif
}

:Output: List of packed bins (shipments);
stop

note right
    FFD Algorithm Complexity: O(n log n)
    Quality: ≤ 11/9 × OPT
    Practical: Near-optimal for typical use
end note

@enduml
```

## How to Use These Diagrams

### Option 1: PlantUML Online Editor
1. Go to https://www.plantuml.com/plantuml/uml
2. Copy and paste any diagram code
3. Click "Submit"
4. View rendered diagram

### Option 2: VS Code
1. Install "PlantUML" extension
2. Create `.puml` files with diagram code
3. Preview with Alt+D

### Option 3: Documentation
Include diagram references in markdown:
```markdown
![](<diagram-file>.svg)
```

## Diagram Legend

- **🟥 Red**: Entity/Database tables
- **🟦 Blue**: Service layer (business logic)
- **🟩 Green**: Repository layer (data access)
- **🟨 Yellow**: Utility/Helper classes
- **🟪 Purple**: Exception handling
- **→**: Dependency/Relationship
- **--o{**: One-to-many relationship
- **--||**: One-to-one relationship

---

**Diagrams Generated:** December 25, 2025  
**Format:** PlantUML  
**Status:** Ready for rendering
