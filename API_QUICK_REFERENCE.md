# API Testing Guide

## Server Status
✅ **Running on:** `http://localhost:8080`
✅ **Context Path:** `/api`
✅ **All Endpoints Functional**

---

## Complete Endpoint URLs

### 1. Initialize Catalog
```bash
POST http://localhost:8080/api/v1/init_catalog
Content-Type: application/json

[
  {"productId": 1, "productName": "RBC A+ Adult", "massG": 700},
  {"productId": 2, "productName": "RBC O- Adult", "massG": 700},
  {"productId": 3, "productName": "Plasma A+", "massG": 350},
  {"productId": 4, "productName": "Platelets", "massG": 200}
]
```

**Response:**
```json
{
  "success": true,
  "message": "Catalog initialized successfully with 4 products",
  "data": "Catalog initialized successfully with 4 products",
  "error": null
}
```

---

### 2. Process Order
```bash
POST http://localhost:8080/api/v1/process_order
Content-Type: application/json

{
  "orderId": 1,
  "requested": [
    {"productId": 1, "quantity": 2},
    {"productId": 2, "quantity": 1},
    {"productId": 3, "quantity": 5}
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Order processed successfully",
  "data": {
    "orderId": 1,
    "status": "PENDING",
    "createdAt": "2025-12-25T16:52:30.611565",
    "totalItems": 3,
    "items": [
      {
        "productId": 1,
        "requestedQty": 2,
        "fulfilledQty": 0,
        "status": "PENDING"
      },
      {
        "productId": 2,
        "requestedQty": 1,
        "fulfilledQty": 0,
        "status": "PENDING"
      },
      {
        "productId": 3,
        "requestedQty": 5,
        "fulfilledQty": 0,
        "status": "PENDING"
      }
    ]
  },
  "error": null
}
```

---

### 3. Process Restock
```bash
POST http://localhost:8080/api/v1/process_restock
Content-Type: application/json

[
  {"productId": 1, "quantity": 10},
  {"productId": 2, "quantity": 8},
  {"productId": 3, "quantity": 15},
  {"productId": 4, "quantity": 20}
]
```

**Response:**
```json
{
  "success": true,
  "message": "Restocking processed successfully",
  "data": {
    "productsRestocked": 4,
    "shipmentsCreated": 2,
    "ordersUpdated": 1
  },
  "error": null
}
```

---

### 4. Get Shipment Details
```bash
GET http://localhost:8080/api/v1/ship_package/1
```

**Response:**
```json
{
  "success": true,
  "message": "Shipment retrieved successfully",
  "data": {
    "orderId": 1,
    "shipped": [
      {
        "productId": 1,
        "quantity": 2
      },
      {
        "productId": 2,
        "quantity": 1
      }
    ]
  },
  "error": null
}
```

---

## Using cURL

### Initialize Catalog
```bash
curl -X POST http://localhost:8080/api/v1/init_catalog \
  -H "Content-Type: application/json" \
  -d '[{"productId": 1, "productName": "Product 1", "massG": 500}]'
```

### Process Order
```bash
curl -X POST http://localhost:8080/api/v1/process_order \
  -H "Content-Type: application/json" \
  -d '{"orderId": 1, "requested": [{"productId": 1, "quantity": 5}]}'
```

### Process Restock
```bash
curl -X POST http://localhost:8080/api/v1/process_restock \
  -H "Content-Type: application/json" \
  -d '[{"productId": 1, "quantity": 20}]'
```

### Get Shipment
```bash
curl -X GET http://localhost:8080/api/v1/ship_package/1
```

---

## Important Notes

⚠️ **CRITICAL:** All URLs include `/api` prefix:
- **Correct:** `http://localhost:8080/api/v1/init_catalog`
- **Incorrect:** `http://localhost:8080/v1/init_catalog`

✅ **Database:** H2 in-memory (auto-creates tables)  
✅ **Port:** 8080  
✅ **Context Path:** /api  
✅ **Data Persistence:** Per JVM session (resets on restart)

---

## Testing Workflow

1. **Initialize products:**
   ```bash
   POST /api/v1/init_catalog
   ```

2. **Create an order:**
   ```bash
   POST /api/v1/process_order
   ```

3. **Restock inventory:**
   ```bash
   POST /api/v1/process_restock
   ```

4. **Retrieve shipment details:**
   ```bash
   GET /api/v1/ship_package/{shipmentId}
   ```

---

## Error Handling

All endpoints return standardized responses:

**Success Response:**
```json
{
  "success": true,
  "message": "Operation completed",
  "data": { /* response data */ },
  "error": null
}
```

**Error Response:**
```json
{
  "success": false,
  "message": "Operation failed",
  "data": null,
  "error": "Error details"
}
```

---

## Application Health

- **Startup Time:** ~4-5 seconds
- **Memory Usage:** ~246 MB
- **Database:** Initialized on startup
- **Port Status:** Listening on 8080
- **Current Status:** ✅ RUNNING
