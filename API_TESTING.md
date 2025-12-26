## API Testing Guide with Postman

### Postman Collection Export Format

Import this into Postman to test all endpoints:

```json
{
  "info": {
    "name": "Inventory Management System API",
    "description": "Collection for testing inventory management endpoints",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Initialize Catalog",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "url": {
          "raw": "http://localhost:8080/api/v1/init_catalog",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "init_catalog"]
        },
        "body": {
          "mode": "raw",
          "raw": "[{\"productId\": 0, \"productName\": \"RBC A+ Adult\", \"massG\": 700}, {\"productId\": 1, \"productName\": \"RBC B+ Adult\", \"massG\": 700}, {\"productId\": 2, \"productName\": \"RBC AB+ Adult\", \"massG\": 750}, {\"productId\": 3, \"productName\": \"RBC O- Adult\", \"massG\": 680}, {\"productId\": 4, \"productName\": \"RBC A+ Child\", \"massG\": 350}, {\"productId\": 5, \"productName\": \"RBC AB+ Child\", \"massG\": 200}, {\"productId\": 6, \"productName\": \"PLT AB+\", \"massG\": 120}, {\"productId\": 7, \"productName\": \"PLT O+\", \"massG\": 80}, {\"productId\": 8, \"productName\": \"CRYO A+\", \"massG\": 40}, {\"productId\": 9, \"productName\": \"CRYO AB+\", \"massG\": 80}, {\"productId\": 10, \"productName\": \"FFP A+\", \"massG\": 300}, {\"productId\": 11, \"productName\": \"FFP B+\", \"massG\": 300}, {\"productId\": 12, \"productName\": \"FFP AB+\", \"massG\": 300}]"
        }
      }
    },
    {
      "name": "2. Process Order (Partial Fulfillment)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "url": {
          "raw": "http://localhost:8080/api/v1/process_order",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "process_order"]
        },
        "body": {
          "mode": "raw",
          "raw": "{\"order_id\": 123, \"requested\": [{\"product_id\": 0, \"quantity\": 2}, {\"product_id\": 10, \"quantity\": 4}]}"
        }
      }
    },
    {
      "name": "3. Process Restock",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "url": {
          "raw": "http://localhost:8080/api/v1/process_restock",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "process_restock"]
        },
        "body": {
          "mode": "raw",
          "raw": "[{\"productId\": 0, \"quantity\": 30}, {\"productId\": 1, \"quantity\": 25}, {\"productId\": 2, \"quantity\": 25}, {\"productId\": 3, \"quantity\": 12}, {\"productId\": 4, \"quantity\": 15}, {\"productId\": 5, \"quantity\": 10}, {\"productId\": 6, \"quantity\": 8}, {\"productId\": 7, \"quantity\": 8}, {\"productId\": 8, \"quantity\": 20}, {\"productId\": 9, \"quantity\": 10}, {\"productId\": 10, \"quantity\": 5}, {\"productId\": 11, \"quantity\": 5}, {\"productId\": 12, \"quantity\": 5}]"
        }
      }
    },
    {
      "name": "4. Get Shipment Details",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/v1/ship_package/1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "ship_package", "1"]
        }
      }
    }
  ]
}
```

### Manual Test Cases

#### Test Case 1: Full Workflow

**Steps:**
1. Initialize catalog with 13 blood products (0-12)
2. Process order 123 requesting products 0 (qty 2) and 10 (qty 4)
3. Expected: Order PENDING (no inventory)
4. Process restock with all products
5. Expected: Order FULFILLED, shipments created
6. Get shipment details from shipment_id 1

**Assertions:**
- Catalog initialized with 13 products
- Order created with status PENDING
- Shipments created after restock
- Shipment contains correct products and quantities

#### Test Case 2: Weight Constraint Validation

**Scenario:** Test that shipments respect 1.8 KG limit

**Setup:**
- Product A: 1000g each
- Product B: 500g each
- Product C: 200g each

**Order:** 1 A (1000g) + 2 B (1000g total) + 3 C (600g total)
- Total: 2600g
- Expected shipments: 2
  - Shipment 1: 1A + 2B = 1500g
  - Shipment 2: 3C = 600g

#### Test Case 3: FIFO Pending Order Fulfillment

**Scenario:** Test older pending orders fulfill first

**Steps:**
1. Initialize catalog
2. Create Order 1 (request product 0, qty 10)
3. Create Order 2 (request product 0, qty 10) - Order 1 should be pending
4. Both orders PENDING (no inventory)
5. Restock product 0 with qty 15
6. Expected: Order 1 fulfilled (qty 10), Order 2 partially (qty 5)

#### Test Case 4: Invalid Product in Catalog

**Steps:**
1. Initialize catalog (skip product 99)
2. Try to process order requesting product 99

**Expected:** 400 Bad Request with error message

#### Test Case 5: Heavy Item Exceeding Limit

**Setup:**
- Product: 2000g

**Order:** Quantity 1

**Expected:** 
- Shipment creation fails with appropriate error
- OR item cannot fit in any shipment

#### Test Case 6: Empty Order

**Steps:**
1. Process order with empty requested list

**Expected:** 400 Bad Request or successful creation with 0 items

#### Test Case 7: Duplicate Order ID

**Steps:**
1. Process order 123
2. Process order 123 again (same ID)

**Expected:** Second request fails (order already exists)

#### Test Case 8: Large Order Optimization

**Scenario:** Verify bin-packing optimization

**Setup:** 10 products with various weights

**Order:** Multiple quantities of each

**Assertion:** Shipment count is minimal (verify FFD algorithm works)

#### Test Case 9: Concurrent Orders

**Setup:** Send multiple order requests simultaneously

**Expected:** All orders processed correctly without data corruption

#### Test Case 10: Shipment Retrieval with Invalid ID

**Steps:**
1. GET /v1/ship_package/99999 (non-existent)

**Expected:** 404 Not Found with error message

### Performance Testing

#### Load Test: 100 Orders

```bash
# Using Apache JMeter or similar
- Ramp-up: 10 seconds
- Hold load: 30 seconds
- Threads: 50
- Requests per thread: 2

Metrics to monitor:
- Response time: p50, p95, p99
- Error rate
- Throughput (orders/second)
```

#### Benchmark: Shipment Optimization

```bash
Test FFD algorithm with:
- 100 items
- Weight range: 100-500g
- Shipment limit: 1800g

Expected:
- Execution time: < 1ms
- Shipment count: ~15-20 (optimal)
```

### Expected Behavior Reference

#### Successful Catalog Init Response
```json
{
  "success": true,
  "message": "Catalog initialized successfully with 13 products",
  "data": "Catalog initialized successfully with 13 products"
}
```

#### Successful Order Processing Response
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

#### Error Response Example
```json
{
  "success": false,
  "message": "Failed to process order",
  "error": "Product not found in inventory: 99"
}
```

### Integration with CI/CD

#### GitHub Actions Example
```yaml
name: API Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Build application
        run: ./gradlew build
      - name: Run application
        run: ./gradlew bootRun &
      - name: Wait for startup
        run: sleep 10
      - name: Run API tests
        run: ./gradlew test
```

### Curl Commands for Manual Testing

```bash
# 1. Initialize Catalog
curl -X POST http://localhost:8080/api/v1/init_catalog \
  -H "Content-Type: application/json" \
  -d '[{"massG": 700, "productName": "RBC A+ Adult", "productId": 0}, {"massG": 700, "productName": "RBC B+ Adult", "productId": 1}, {"massG": 750, "productName": "RBC AB+ Adult", "productId": 2}, {"massG": 680, "productName": "RBC O- Adult", "productId": 3}, {"massG": 350, "productName": "RBC A+ Child", "productId": 4}, {"massG": 200, "productName": "RBC AB+ Child", "productId": 5}, {"massG": 120, "productName": "PLT AB+", "productId": 6}, {"massG": 80, "productName": "PLT O+", "productId": 7}, {"massG": 40, "productName": "CRYO A+", "productId": 8}, {"massG": 80, "productName": "CRYO AB+", "productId": 9}, {"massG": 300, "productName": "FFP A+", "productId": 10}, {"massG": 300, "productName": "FFP B+", "productId": 11}, {"massG": 300, "productName": "FFP AB+", "productId": 12}]'

# 2. Process Order
curl -X POST http://localhost:8080/api/v1/process_order \
  -H "Content-Type: application/json" \
  -d '{"orderId": 123, "requested": [{"productId": 0, "quantity": 2}, {"productId": 10, "quantity": 4}]}'

# 3. Restock
curl -X POST http://localhost:8080/api/v1/process_restock \
  -H "Content-Type: application/json" \
  -d '[{"productId": 0, "quantity": 30}, {"productId": 1, "quantity": 25}, {"productId": 2, "quantity": 25}, {"productId": 3, "quantity": 12}, {"productId": 4, "quantity": 15}, {"productId": 5, "quantity": 10}, {"productId": 6, "quantity": 8}, {"productId": 7, "quantity": 8}, {"productId": 8, "quantity": 20}, {"productId": 9, "quantity": 10}, {"productId": 10, "quantity": 5}, {"productId": 11, "quantity": 5}, {"productId": 12, "quantity": 5}]'

# 4. Get Shipment
curl http://localhost:8080/api/v1/ship_package/1

# Pretty print JSON response
curl http://localhost:8080/api/v1/ship_package/1 | json_pp
```

### Verification Checklist

After testing, verify:
- [ ] All 4 endpoints return 200/4xx appropriately
- [ ] Error responses have standard format
- [ ] Shipments respect 1.8 KG weight limit
- [ ] Order status transitions are correct
- [ ] FIFO order for pending items is maintained
- [ ] Inventory quantities are accurate
- [ ] No data corruption on concurrent requests
- [ ] Response times acceptable (< 500ms)
- [ ] Database state consistent after each operation

---

**Test Version:** 1.0  
**Last Updated:** December 25, 2025
