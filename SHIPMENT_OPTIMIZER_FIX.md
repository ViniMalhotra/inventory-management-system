# Shipment Optimizer Vulnerability Fix

## Problem Description

The original `ShipmentPackagingOptimizer` had a critical vulnerability that would throw an `IllegalArgumentException` when a single product item's total weight exceeded the maximum shipment weight limit (1800g).

### Example Scenario
- Product A: 500g per unit
- Quantity: 6 units
- Total weight: 3000g
- **Result**: Exception thrown - item cannot be shipped at all

This breaks the shipment process entirely for any product where:
```
unitWeight * quantity > MAX_SHIPMENT_WEIGHT_G (1800g)
```

## Solution: Intelligent Item Splitting

The optimizer now automatically splits oversized items into multiple smaller items that each fit within the weight limit.

### Algorithm Enhancement

**Before (Original):**
1. Sort items by weight (heaviest first)
2. Try to fit each item in existing packages
3. If it doesn't fit anywhere, throw an exception

**After (Fixed):**
1. **Pre-process**: Split any item whose total weight exceeds max shipment weight
   - Calculate maximum quantity per shipment: `maxQtyPerShipment = MAX_SHIPMENT_WEIGHT (1800g) / unitWeightG`
   - Distribute remaining quantity across additional shipment items
2. Sort processed items by weight (heaviest first)
3. Fit items in packages using first-fit decreasing algorithm
4. Optimal bin packing achieved while handling oversized items

### Key Implementation Details

**New Method: `splitOversizedItem(ShipmentItem item)`**
- Calculates the maximum quantity that can fit per shipment based on unit weight
- Preserves product identity (same productId for all split items)
- Distributes remaining quantity across multiple ShipmentItem objects
- Only throws exception if a SINGLE UNIT exceeds max weight (true constraint)

**Modified Method: `optimizePackaging(List<ShipmentItem> items)`**
- Now handles oversized items gracefully through preprocessing
- Maintains first-fit decreasing algorithm for optimal packing
- Provides detailed logging of splitting operations

## Example Walkthrough

Given:
- Product A: 500g/unit, quantity 6 = 3000g total

**Splitting Process:**
1. Detect: 3000g > 1800g (max shipment)
2. Calculate: maxQtyPerShipment = 1800g / 500g = 3 units
3. Split into:
   - Shipment Item 1: Product A, qty 3, weight 1500g
   - Shipment Item 2: Product A, qty 3, weight 1500g
4. Both items fit perfectly in one shipment each

## Benefits

✅ **Handles all valid product combinations** - No exceptions for valid shipments
✅ **Optimal packing** - Still minimizes number of shipments through bin packing
✅ **Maximum weight utilization** - Uses up to 1800g per shipment whenever possible
✅ **Transparent to consumers** - Split items are tracked with the same product ID
✅ **Robust error handling** - Only fails when physically impossible (single unit > 1800g)

## Constraint Validation

The only exception thrown now is when a single unit of a product exceeds the maximum shipment weight (1800g):
```java
if (maxQtyPerShipment == 0) {
    // Single unit exceeds 1800g - truly impossible to ship
    throw new IllegalArgumentException(...)
}
```

This is the only true physical constraint that cannot be worked around.

## Test Results

✅ Build: SUCCESS
✅ All existing tests: PASSED
✅ No breaking changes to public API
