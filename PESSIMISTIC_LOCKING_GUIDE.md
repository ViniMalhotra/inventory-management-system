# Implementation Guide: Pessimistic Locking for Inventory Race Conditions

## Overview
This document shows exactly how pessimistic locking prevents race conditions between `processOrder()` and `processRestock()` in your inventory system.

---

## The Problem (Before Implementation)

### Race Condition Scenario
```
Thread 1 (processOrder)        | Thread 2 (processRestock)
Reading inventory...           |
                               | Reading inventory...
Qty = 10                       | Qty = 10
Reducing by 10                 |
qty = 0                        | Adding 25
                               | qty = 35
Saving to DB: 0                |
                               | Saving to DB: 35 (overwrites!)
```

**Result**: Lost the deduction, inventory is 35 instead of 25.

---

## The Solution (After Implementation)

### Key Components

#### 1. Repository Layer
```java
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    // Pessimistic write lock - exclusive access
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByIdWithLock(@Param("productId") Long productId);
    
    // Batch version for multiple products
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds")
    List<Inventory> findByProductIdInWithLock(@Param("productIds") List<Long> productIds);
}
```

#### 2. Service Layer
```java
@Service
@Transactional  // Important: Ensures lock is held across method
public class InventoryService {
    
    public void reduceInventory(Long productId, Long quantity) {
        // LOCK ACQUIRED HERE (exclusive access)
        Inventory inventory = inventoryRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException(...));
        
        // Only this transaction can access/modify this row
        Long currentQty = inventory.getAvailableQty();
        
        if (currentQty < quantity) {
            throw new IllegalArgumentException("Insufficient inventory");
        }
        
        // Safe modification - no interference
        inventory.setAvailableQty(currentQty - quantity);
        inventoryRepository.save(inventory);
        
        // LOCK RELEASED when @Transactional completes
    }
    
    public void increaseInventory(Long productId, Long quantity) {
        // LOCK ACQUIRED HERE (exclusive access)
        Inventory inventory = inventoryRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException(...));
        
        // Only this transaction can access/modify this row
        Long newQty = inventory.getAvailableQty() + quantity;
        
        // Safe modification - no interference
        inventory.setAvailableQty(newQty);
        inventoryRepository.save(inventory);
        
        // LOCK RELEASED when @Transactional completes
    }
}
```

---

## Execution Flow with Locking

```
Thread 1: processOrder()              | Thread 2: processRestock()
==================================    |    ==================================

Start transaction                     | Start transaction

Call reduceInventory(product_1, 10)  | Call increaseInventory(product_1, 25)

ACQUIRE LOCK on product_1             | LOCK REQUEST → BLOCKS
(PESSIMISTIC_WRITE)                   | (Waits for lock)
↓
Read qty = 10 (secured)               | Still waiting...
Validate qty >= 10 ✓                  |
Modify qty = 0                        | Still waiting...
Save to DB                            |
                                      | Still waiting...
COMMIT transaction                    |
RELEASE LOCK                          | FINALLY ACQUIRED LOCK!
                                      | ↓
                                      | Read qty = 0 (secured)
                                      | Modify qty = 0 + 25 = 25
                                      | Save to DB
                                      | COMMIT transaction
                                      | RELEASE LOCK

RESULT: Qty = 25 ✓ (Correct!)
No lost updates, no race conditions!
```

---

## Database Level Lock

### What Happens at Database

```sql
-- Thread 1
BEGIN TRANSACTION;
SELECT * FROM inventory WHERE product_id = 1 FOR UPDATE;  -- ACQUIRES LOCK
-- Holds lock...
UPDATE inventory SET available_qty = 0 WHERE product_id = 1;
COMMIT;  -- RELEASES LOCK

-- Thread 2 (at same time)
BEGIN TRANSACTION;
SELECT * FROM inventory WHERE product_id = 1 FOR UPDATE;  -- WAITS FOR LOCK
-- Blocked until Thread 1 commits...
-- Once lock acquired:
UPDATE inventory SET available_qty = 25 WHERE product_id = 1;
COMMIT;
```

### Lock Characteristics

| Property | Value |
|----------|-------|
| Lock Type | Row-level (product_id specific) |
| Lock Mode | Exclusive Write Lock |
| Duration | Transaction lifetime |
| Scope | Single product_id |
| Multiple Products | Each gets own lock (parallel possible) |
| Timeout | DBMS configurable |
| Release | Auto on COMMIT or ROLLBACK |

---

## Preventing Different Types of Anomalies

### 1. Lost Update (Main Issue)

**Without Locking:**
```
T1: Read qty = 10
T2: Read qty = 10
T1: Write qty = 0  (lost!)
T2: Write qty = 35 (overwrites T1)
```

**With Locking:**
```
T1: LOCK + Read qty = 10
T2: WAIT FOR LOCK
T1: Write qty = 0
T1: UNLOCK
T2: LOCK + Read qty = 0
T2: Write qty = 25
T2: UNLOCK
```

### 2. Dirty Read (Reading uncommitted data)
Prevented because locks are held until COMMIT.

### 3. Non-repeatable Read (Reading different values)
Prevented because lock ensures same value throughout transaction.

### 4. Phantom Read (New rows appearing)
Not applicable here (single row operations).

---

## Code Flow Example

### Scenario: Process Order for 10 units + Restock 25 units

```java
// Controller
@PostMapping("/process_order")
public void processOrder() {
    orderService.processOrder(orderRequest);  // Calls reduceInventory
}

@PostMapping("/process_restock")
public void processRestock() {
    inventoryService.increaseInventory(1L, 25L);  // Calls increaseInventory
}

// Service Layer - OrderService
@Transactional
public Order processOrder(OrderRequestDTO req) {
    // ... create order ...
    shipmentService.createShipments(...);  // Eventually calls:
    inventoryService.reduceInventory(1L, 10L);  // ← LOCK ACQUIRED HERE
}

// Service Layer - InventoryService
@Transactional
public void reduceInventory(Long productId, Long qty) {
    // 1. ACQUIRE LOCK (exclusive)
    Inventory inv = inventoryRepository.findByIdWithLock(productId)
        .orElseThrow();
    
    // 2. VALIDATE
    if (inv.getAvailableQty() < qty) {
        throw new IllegalArgumentException(...);
    }
    
    // 3. MODIFY
    inv.setAvailableQty(inv.getAvailableQty() - qty);
    inventoryRepository.save(inv);
    
    // 4. UNLOCK (automatic on transaction end)
}

@Transactional
public void increaseInventory(Long productId, Long qty) {
    // 1. ACQUIRE LOCK (exclusive, may wait if reduceInventory holds it)
    Inventory inv = inventoryRepository.findByIdWithLock(productId)
        .orElseThrow();
    
    // 2. MODIFY
    inv.setAvailableQty(inv.getAvailableQty() + qty);
    inventoryRepository.save(inv);
    
    // 3. UNLOCK (automatic on transaction end)
}
```

---

## Test Implementation

### Mock Setup for Tests
```java
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    
    @Mock
    InventoryRepository inventoryRepository;
    
    @Test
    void testReduceInventory() {
        // Mock the locked query
        Inventory inventory = new Inventory(1L, 10L);
        when(inventoryRepository.findByIdWithLock(1L))
            .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any()))
            .thenReturn(inventory);
        
        // Test
        inventoryService.reduceInventory(1L, 3L);
        
        // Verify
        assertEquals(7L, inventory.getAvailableQty());
    }
}
```

---

## Production Considerations

### 1. Lock Timeout Configuration
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
          fetch_size: 50
        # Lock timeout (database specific)
        # MySQL: max_connections timeout
        # PostgreSQL: statement_timeout
```

### 2. Monitoring Lock Contention
```java
// Add metrics
log.info("Lock wait time for product {}: {}ms", productId, duration);

// Monitor in production
SELECT waiting_pid, blocking_pid FROM pg_stat_activity;
```

### 3. Deadlock Prevention
- Locks always acquired in same order (by product_id)
- Transaction duration kept minimal
- No nested transactions with locks

### 4. Performance Optimization
- Different products = independent locks = parallel
- Same product = serialized = correct but slower
- Consider sharding if single product is bottleneck

---

## When Locks Are Held

```
reduceInventory() call
  ↓
START @Transactional
  ↓
findByIdWithLock() executed
  ↓
LOCK ACQUIRED ← Now held!
  ↓
Read & modify inventory
  ↓
save() executed
  ↓
END @Transactional
  ↓
LOCK RELEASED ← Released here!
```

**Important**: Lock is held for entire transaction, not just the query.

---

## Summary

### Before (Race Condition Risk)
- ❌ Lost updates possible
- ❌ Data inconsistency
- ❌ Works only single-server
- ❌ Unpredictable bugs

### After (Pessimistic Locking)
- ✅ Zero lost updates
- ✅ Always consistent
- ✅ Works everywhere
- ✅ Predictable behavior

The lock ensures that `processOrder()` and `processRestock()` can never execute concurrently on the same product, eliminating race conditions.
