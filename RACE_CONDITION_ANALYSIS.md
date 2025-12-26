# Race Condition Analysis: Process Order vs Process Restock

## Problem Scenario

Two concurrent operations can race:

```
Timeline:
T1: processOrder() calls getAvailableQuantity() → returns 10 units
T2: processRestock() calls increaseInventory() → updates to 35 units (added 25)
T3: processOrder() calls reduceInventory(10) → sets to 25 units
     (Should be 25, but we lost the +25 from restock!)
```

### Race Condition Flow:

```
Process Order (Thread 1)        |  Process Restock (Thread 2)
                                |
Read Inventory (qty=10)         |
                                |  Read Inventory (qty=10)
                                |  Update qty to 35 (add 25)
                                |  Commit transaction
Reduce by 10 → qty=0            |
Commit transaction              |  (Final: 0, but should be 25!)
```

## Solution Comparison: Transactional Lock vs Local Lock

### Option 1: Local Lock (NOT RECOMMENDED)
```java
private Object inventoryLock = new Object();

synchronized(inventoryLock) {
    // Process inventory
}
```

**Problems:**
- ❌ Only works within a single JVM instance
- ❌ Fails in distributed/clustered systems
- ❌ No database-level enforcement
- ❌ Can still have phantom reads and dirty reads
- ❌ Lock is held in memory, lost on restart

### Option 2: Pessimistic Database Lock (RECOMMENDED) ✅
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<Inventory> findByProductIdInWithLock(@Param("productIds") List<Long> productIds);
```

**Advantages:**
- ✅ Works across all JVM instances (distributed)
- ✅ Database enforces lock at row level
- ✅ Prevents dirty reads, non-repeatable reads, phantom reads
- ✅ Lock is persistent and survives application restart
- ✅ Other transactions must wait - no race conditions
- ✅ Atomic operations guaranteed

## Recommended Implementation

Use **pessimistic write locks** at the point where inventory is first read:

### 1. Add Locking to Repository

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
Optional<Inventory> findByIdWithLock(@Param("productId") Long productId);
```

### 2. Update InventoryService Methods

```java
public void reduceInventory(Long productId, Long quantity) {
    // Lock prevents concurrent modification
    Inventory inventory = inventoryRepository.findByIdWithLock(productId).orElseThrow();
    
    if (inventory.getAvailableQty() < quantity) {
        throw new IllegalArgumentException("Insufficient inventory");
    }
    
    inventory.setAvailableQty(inventory.getAvailableQty() - quantity);
    inventoryRepository.save(inventory);
}

public void increaseInventory(Long productId, Long quantity) {
    // Lock prevents concurrent modification
    Inventory inventory = inventoryRepository.findByIdWithLock(productId).orElseThrow();
    inventory.setAvailableQty(inventory.getAvailableQty() + quantity);
    inventoryRepository.save(inventory);
}
```

### 3. Execution Flow with Locks

```
Process Order (Thread 1)           |  Process Restock (Thread 2)
                                   |
LOCK inventory for product 1       |
Read inventory (qty=10)            |
                                   |  WAITS for lock to be released
Reduce qty to 0                    |  (BLOCKED)
COMMIT & release lock              |
                                   |  ACQUIRES lock
                                   |  Read inventory (qty=0)
                                   |  Increase to 25
                                   |  COMMIT & release lock
```

## Database-Level Guarantees

With pessimistic write locks:

| Isolation Level | Dirty Read | Non-repeatable Read | Phantom Read |
|-----------------|-----------|-------------------|--------------|
| READ UNCOMMITTED | Yes | Yes | Yes |
| READ COMMITTED | No | Yes | Yes |
| REPEATABLE READ | No | No | Yes |
| SERIALIZABLE | No | No | No |
| **PESSIMISTIC_WRITE** | **No** | **No** | **No** |

## Implementation Decision Matrix

| Factor | Local Lock | Pessimistic Lock |
|--------|-----------|------------------|
| Multi-JVM Support | ❌ No | ✅ Yes |
| Database Enforcement | ❌ No | ✅ Yes |
| Scalability | ❌ Limited | ✅ Excellent |
| Complexity | ✅ Simple | 🟡 Moderate |
| Performance | 🟡 Good | 🟡 Slightly Lower |
| Race Condition Safety | ❌ No | ✅ Yes |
| Cloud/K8s Ready | ❌ No | ✅ Yes |

## Recommendation

**Use Pessimistic Database Locks** because:

1. **Enterprise-Grade**: Works across distributed deployments
2. **Database Enforced**: No application-level coordination needed
3. **Atomic**: Prevents all types of race conditions
4. **Production-Ready**: Used in major banking and finance systems
5. **Fallback-Safe**: Lock survives application crashes

## Implementation Steps

1. Add `findByIdWithLock()` to InventoryRepository
2. Update `reduceInventory()` to use locked query
3. Update `increaseInventory()` to use locked query
4. Both operations (Order and Restock) will safely serialize
