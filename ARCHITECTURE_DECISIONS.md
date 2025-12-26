# System Architecture Decision Log

## Overview
This document captures the key architectural decisions made during the design and implementation of the Inventory Management System backend.

## Decision 1: Microservice vs. Monolithic Architecture
**Decision:** Monolithic Spring Boot Application
**Rationale:**
- Simpler deployment for initial requirements
- Easier transaction management across entities
- Lower operational overhead
- Faster development cycle

**Trade-offs:**
- Cannot scale services independently
- Shared database becomes bottleneck at scale
- Deployment requires full application restart

**Future Evolution:**
- Extract ShipmentService into separate microservice when load requires it
- Use event streaming (Kafka) for async order processing

---

## Decision 2: SQL Database (Relational) vs. NoSQL
**Decision:** Relational Database (SQL)
**Rationale:**
- Inventory requires ACID transactions (no overselling)
- Complex relationships between orders, items, shipments
- SQL JOINs essential for reporting
- Normalized schema prevents data anomalies

**Implementation:**
- H2 for development/testing (in-memory)
- PostgreSQL for production (proven stability)

**Alternative Considered:**
- MongoDB: Rejected due to eventual consistency and transaction limitations

---

## Decision 3: ORM Framework (JPA/Hibernate) vs. Raw SQL
**Decision:** Spring Data JPA with Hibernate
**Rationale:**
- Reduces boilerplate code (70% less code than raw JDBC)
- Query derivation from method names
- Automatic transaction management
- Type-safe queries

**Trade-offs:**
- Slightly higher abstraction level
- N+1 query problem if not careful
- Less control over exact SQL generation

**Mitigation:**
- Proper use of lazy/eager loading
- Repository method design for common queries
- Monitoring and profiling in production

---

## Decision 4: Shipment Optimization Algorithm
**Decision:** First-Fit Decreasing (FFD) Bin Packing
**Rationale:**
- Simple to implement and understand
- Provides near-optimal solutions (FFD achieves ≤ 11/9 OPT)
- O(n log n) time complexity
- Practical performance for typical order sizes

**Algorithm Details:**
1. Sort items by weight descending
2. For each item, place in first bin with capacity
3. Create new bin if needed

**Example:**
```
Items: [(A, 1000g), (B, 500g), (C, 500g), (D, 300g)]
Limit: 1800g per shipment

Step 1: Sort descending
[(A, 1000g), (B, 500g), (C, 500g), (D, 300g)]

Step 2: Pack
Shipment 1: A(1000g) + B(500g) = 1500g ✓
Shipment 2: C(500g) + D(300g) = 800g ✓

Result: 2 shipments (optimal)
```

**Alternative Algorithms Considered:**
- Best-Fit: More computationally expensive, minimal improvement
- Next-Fit: Simpler but inferior results
- Dynamic Programming: Over-engineered for typical use cases

---

## Decision 5: Pending Order Item Fulfillment Strategy
**Decision:** FIFO (First-In-First-Out) with Timestamp Ordering
**Rationale:**
- Fair and predictable for customers
- Prevents order starvation
- Easy to understand and audit
- Aligns with customer expectations

**Implementation:**
```sql
SELECT * FROM pending_order_items 
WHERE product_id = ? 
ORDER BY created_at ASC
```

**Alternative Considered:**
- Random: Unfair, unpredictable
- Priority-based: Requires complex configuration
- Longest-waiting: Same as FIFO, more complex

---

## Decision 6: Order Status Model
**Decision:** 4-State Lifecycle (PENDING → PARTIALLY_FULFILLED → FULFILLED → COMPLETED)
**Rationale:**
- Clear distinction between initial fulfillment and final completion
- FULFILLED = all original items shipped (but may have pending items)
- COMPLETED = all items (including previously pending) fulfilled
- Supports audit trail

**State Transitions:**
```
PENDING (initial)
   ├─→ PARTIALLY_FULFILLED (some items shipped)
   │       └─→ FULFILLED (all items shipped)
   │           └─→ COMPLETED (all pending items fulfilled)
   │
   └─→ FULFILLED (all items shipped on first attempt)
       └─→ COMPLETED (if no pending items)
```

**Alternative Considered:**
- 3-state model: Conflates initial fulfillment with completion
- Event-based: Overkill for current requirements

---

## Decision 7: Spring Boot Version
**Decision:** Spring Boot 3.1.5
**Rationale:**
- Latest stable version (as of project date)
- Jakarta EE support (Java 17+ requirement)
- Latest Spring Data JPA features
- Active security updates and maintenance

**Compatibility:**
- Requires Java 17 minimum
- Target Java 17 for production deployment

---

## Decision 8: API Response Structure
**Decision:** Consistent Wrapper with Success Flag
**Rationale:**
- All responses have same structure (predictable for clients)
- Success flag eliminates ambiguity (2xx status but business error)
- Data field carries actual response
- Error field provides details on failures

**Response Format:**
```json
{
  "success": true|false,
  "message": "user-friendly message",
  "data": { },
  "error": "technical error details (if failed)"
}
```

**Benefits:**
- Simplified client error handling
- Clear success/failure semantics
- Consistent across all endpoints

---

## Decision 9: Exception Handling Strategy
**Decision:** Centralized Exception Handler with Custom Exceptions
**Rationale:**
- Single point for error transformation
- Consistent error response format
- Reduces boilerplate in controllers
- Easy to add new error types

**Implementation:**
- Custom exception classes for domain errors
- GlobalExceptionHandler for centralized handling
- StandardException wrapping for unexpected errors

---

## Decision 10: Database Initialization Approach
**Decision:** API-driven Catalog Initialization
**Rationale:**
- Flexibility in deployment scenarios
- Can initialize from external configuration
- No hardcoded data in application
- Supports multiple catalog scenarios

**Trade-off:**
- Requires API call before order processing
- Could be improved with database migrations (Flyway/Liquibase)

---

## Decision 11: Inventory Validation Timing
**Decision:** Validate at Request Time, Not at Order Creation
**Rationale:**
- Catches invalid products immediately
- Prevents invalid order creation
- Simplifies later processing logic

**Flow:**
1. Request received
2. Validate all products exist in inventory
3. Proceed with order creation

**Alternative:**
- Validate during shipment creation: Leads to partial order creation

---

## Decision 12: Shipment Weight Enforcement
**Decision:** Hard Constraint (Exception on Violation)
**Rationale:**
- Physical shipping requirement (non-negotiable)
- Early detection prevents invalid shipments
- Forces optimization before creation

**Implementation:**
- ShipmentPackagingOptimizer validates all shipments
- Throws exception if single item exceeds limit
- Algorithm ensures compliance

---

## Performance Optimization Decisions

### Decision 13: Lazy vs. Eager Loading
**Decision:** Lazy loading for relationships, eager for critical paths
**Rationale:**
- Lazy: Prevents unnecessary queries by default
- Eager for Product in OrderItem: Mass needed for weight calculations

**Trade-off:**
- Lazy loading can cause LazyInitializationException if accessed outside transaction
- Eager loading can fetch unnecessary data

---

### Decision 14: Repository Query Derivation
**Decision:** Method naming convention over @Query annotation
**Rationale:**
- Simpler, more readable
- Spring Data derives optimal SQL automatically
- Less verbose than @Query for simple cases

**Example:**
```java
// Instead of:
@Query("SELECT p FROM PendingOrderItem p WHERE p.productId = ?1 ORDER BY p.createdAt ASC")

// We use:
List<PendingOrderItem> findByProductIdOrderByCreatedAt(Long productId);
```

---

## Testing Strategy Decisions

### Decision 15: H2 In-Memory Database for Testing
**Decision:** Use H2 for development and unit testing
**Rationale:**
- Fast test execution (no I/O overhead)
- No external dependencies
- Schema created automatically
- Perfect for CI/CD pipelines

**Production:**
- Switch to PostgreSQL via Spring profiles
- Separate application-prod.yml configuration

---

## Future Evolution Decisions

### Decision 16: Event-Driven Architecture (Future)
**Current:** Request/Response synchronous
**Future:** Kafka/RabbitMQ for async processing

**Trigger Points:**
- Order fulfillment reaches SLA limits
- Need for audit event log
- Integration with other systems

---

### Decision 17: Distributed Transactions (Future)
**Current:** Local database transactions
**Future:** Consider when services separated

**Technologies:**
- Saga pattern with compensating transactions
- Distributed tracing (Spring Cloud Sleuth)

---

## Rejected Decisions & Why

### Rejected: Optimistic Locking
**Reason:** Sequential order processing eliminates conflict risk

### Rejected: CQRS Pattern
**Reason:** Complexity not justified for current scale

### Rejected: Kafka Streaming
**Reason:** In-process transactions sufficient for MVP

### Rejected: GraphQL
**Reason:** REST simpler for batch operations like restock

---

## Assumption Document

### Business Assumptions
1. Shipment weight limit is fixed at 1.8 KG
2. Each product has a single, immutable mass value
3. FIFO is acceptable for pending order fulfillment
4. Orders with same ID cannot exist (no deduplication)

### Technical Assumptions
1. Synchronous request/response is acceptable
2. Single database instance (not distributed)
3. Data consistency more important than availability (CP over AP)
4. Typical order has < 10 items
5. Peak load < 1000 orders/hour

---

**Document Version:** 1.0  
**Last Updated:** December 25, 2025  
**Owner:** Architecture Review Board
