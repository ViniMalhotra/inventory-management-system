# Database Query Optimization for Shipment Creation

## Problem: Multiple Database Calls Per Item

Previously, the `ShipmentService.createShipments()` method was making **two database calls per order item**:

```java
for (OrderItem item : orderItems) {
    Product product = inventoryService.getProductDetails(item.getProductId());  // Query 1
    Long availableQty = inventoryService.getAvailableQuantity(item.getProductId()); // Query 2
    // ... process item
}
```

For an order with 10 items, this resulted in **20 database queries**.

## Solution: Batch Loading

Implemented a new batch method `getProductsWithInventory()` that loads all product details and inventory quantities in **2 queries total** (regardless of number of items):

### Changes Made

#### 1. **InventoryService Enhancement**
- Added new method: `getProductsWithInventory(List<Long> productIds)`
- Created DTO: `ProductInventoryData` (holds Product + available quantity)
- Uses repository's `findByProductIdIn()` for batch inventory lookup
- Uses `findAllById()` for batch product lookup
- Returns a Map for O(1) lookup by productId

#### 2. **ShipmentService Optimization**
Refactored `createShipments()` to:
1. Extract all product IDs from order items once
2. Call single batch method to load all products + inventory
3. Iterate through items with data already loaded in memory

### Code Comparison

**Before (Multiple Calls):**
```java
for (OrderItem item : orderItems) {
    Product product = inventoryService.getProductDetails(item.getProductId());
    Long availableQty = inventoryService.getAvailableQuantity(item.getProductId());
    // 2 queries per item × N items = 2N queries total
}
```

**After (Batch Loading):**
```java
List<Long> productIds = orderItems.stream()
    .map(OrderItem::getProductId)
    .toList();

Map<Long, ProductInventoryData> productInventoryMap = 
    inventoryService.getProductsWithInventory(productIds);

for (OrderItem item : orderItems) {
    ProductInventoryData data = productInventoryMap.get(item.getProductId());
    // All data loaded in 2 queries total
}
```

### Database Impact

| Scenario | Before | After | Improvement |
|----------|--------|-------|-------------|
| 5 items | 10 queries | 2 queries | 80% reduction |
| 10 items | 20 queries | 2 queries | 90% reduction |
| 50 items | 100 queries | 2 queries | 98% reduction |

### Implementation Details

**ProductInventoryData DTO:**
```java
public static class ProductInventoryData {
    public Product product;        // Full product details
    public Long availableQty;      // Inventory quantity
}
```

**Batch Method:**
1. Validates input (handles null/empty lists)
2. Loads all inventory records in 1 query: `findByProductIdIn(productIds)`
3. Loads all products in 1 query: `findAllById(productIds)`
4. Combines data into a Map for O(1) lookups
5. Handles missing inventory gracefully (returns default 0 quantity)

### Benefits

✅ **Massive performance improvement**: O(n) queries reduced to O(1)
✅ **Scalable**: Query count independent of number of items
✅ **Database load reduction**: Fewer round-trips to database
✅ **Memory efficient**: Uses Map for O(1) lookups
✅ **Backward compatible**: Old methods still available if needed
✅ **Thread-safe**: Transactional service method
✅ **Error handling**: Gracefully handles missing products

### Test Updates

All ShipmentService tests updated to mock the new batch method:
- `testCreateShipmentsNoItemsToShip()`
- `testCreateShipmentsWithAvailableItems()`
- `testCreateShipmentsRespectWeightConstraints()`
- `testCreateShipmentsMultipleItems()`

### Verification

✅ Build: SUCCESS
✅ All tests: PASSED
✅ No breaking changes to public API
