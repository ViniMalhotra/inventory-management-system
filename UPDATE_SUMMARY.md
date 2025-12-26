# API Testing Documentation Updates - December 26, 2025

## Summary of Changes

All `init_catalog` API testing references in `API_TESTING.md` have been updated to use an expanded 13-product blood product inventory catalog.

---

## Updated Catalog

The API testing documentation now includes 13 products across multiple blood product categories:

### Red Blood Cells (RBC)
- **Product 0**: RBC A+ Adult (700g)
- **Product 1**: RBC B+ Adult (700g)
- **Product 2**: RBC AB+ Adult (750g)
- **Product 3**: RBC O- Adult (680g)
- **Product 4**: RBC A+ Child (350g)
- **Product 5**: RBC AB+ Child (200g)

### Platelets (PLT)
- **Product 6**: PLT AB+ (120g)
- **Product 7**: PLT O+ (80g)

### Cryoprecipitate (CRYO)
- **Product 8**: CRYO A+ (40g)
- **Product 9**: CRYO AB+ (80g)

### Fresh Frozen Plasma (FFP)
- **Product 10**: FFP A+ (300g)
- **Product 11**: FFP B+ (300g)
- **Product 12**: FFP AB+ (300g)

---

## Files Updated

### `/API_TESTING.md`

1. **Postman Collection (Line ~30)**
   - Updated JSON request body in Postman collection format
   - Changed from snake_case to camelCase format
   - Expanded from 4 products to 13 products

2. **Test Case 1 Description (Line ~107)**
   - Updated step 1: "Initialize catalog with 13 blood products (0-12)"
   - Updated assertion: "Catalog initialized with 13 products"

3. **Expected Response Example (Line ~229)**
   - Updated success message: "Catalog initialized successfully with 13 products"

4. **Curl Commands Section (Lines 310-324)**
   - **#1 Init Catalog**: Updated JSON with all 13 products
   - **#2 Process Order**: Updated to camelCase (`orderId`, `productId`)
   - **#3 Restock**: Updated to camelCase with inventory for all 13 products

---

## JSON Format Updates

### Before (snake_case)
```json
{
  "mass_g": 700,
  "product_name": "RBC A+ Adult",
  "product_id": 0
}
```

### After (camelCase)
```json
{
  "massG": 700,
  "productName": "RBC A+ Adult",
  "productId": 0
}
```

---

## Test Scenario Changes

### Previous Scenario
- 4 products in catalog
- Order requesting products 0 and 10 only
- Limited restock quantities

### New Scenario
- 13 products in catalog representing real blood bank inventory
- Order requesting products 0 (RBC A+ Adult) and 10 (FFP A+)
- Comprehensive restock quantities for all products:
  - RBC products: 30, 25, 25, 12, 15, 10 units
  - PLT products: 8, 8 units
  - CRYO products: 20, 10 units
  - FFP products: 5, 5, 5 units

---

## API Endpoints Tested

All 4 API endpoints remain the same:

1. **POST /api/v1/init_catalog**
   - Initialize 13-product blood inventory catalog
   - Each product has mass in grams and unique ID

2. **POST /api/v1/process_order**
   - Create order 123 requesting 2 units of RBC A+ Adult and 4 units of FFP A+
   - Expected status: PENDING (no inventory yet)

3. **POST /api/v1/process_restock**
   - Restock all 13 products with realistic quantities
   - Triggers automatic fulfillment of pending orders
   - Creates optimized shipments respecting 1800g weight limit

4. **GET /api/v1/ship_package/{shipmentId}**
   - Retrieve shipment details
   - Validate shipment contents

---

## Verification Checklist

When running the updated curl commands, verify:

- [ ] All 13 products initialized successfully
- [ ] Catalog response includes count: "13 products"
- [ ] Order created with PENDING status (no inventory)
- [ ] Restock processes all 13 products
- [ ] Shipments created with correct items and weights
- [ ] Weight constraints respected (max 1800g per shipment)
- [ ] Pessimistic locks prevent concurrent modification
- [ ] Error responses include product 0-12 only

---

## Ready to Use

The `API_TESTING.md` file is now updated with:
- ✅ 13-product catalog in all locations
- ✅ Proper camelCase JSON format
- ✅ Complete curl commands ready to copy/paste
- ✅ Realistic blood bank product inventory
- ✅ Updated test case descriptions

All curl commands in the file are ready to execute directly in the terminal.

---

## Next Steps

1. Execute the updated curl commands in order:
   ```bash
   # Copy the curl commands from API_TESTING.md sections
   # Commands are on lines 310-324
   ```

2. Verify all endpoints respond correctly:
   - Init catalog: 200 OK with 13 products
   - Process order: 200 OK with PENDING status
   - Process restock: 200 OK with shipments created
   - Get shipment: 200 OK with shipment details

3. Validate business logic:
   - Pessimistic locks active during inventory operations
   - Batch loading optimization working (O(1) queries)
   - Item splitting algorithm handling weight constraints
   - Order fulfillment following FIFO for pending items

---

**Updated**: December 26, 2025  
**Status**: ✅ Ready for Testing
