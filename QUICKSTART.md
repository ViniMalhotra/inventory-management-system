# Quick Start Guide

## Setup & Installation

### 1. Clone and Navigate
```bash
cd inventory-management-system
```

### 2. Build the Project
```bash
./gradlew clean build
```

### 3. Run the Application
```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## Quick API Test Flow

### Step 1: Initialize Catalog
```bash
curl -X POST http://localhost:8080/api/v1/init_catalog \
  -H "Content-Type: application/json" \
  -d '[
    {"product_id": 0, "product_name": "RBC A+ Adult", "mass_g": 700},
    {"product_id": 1, "product_name": "RBC B+ Adult", "mass_g": 700},
    {"product_id": 10, "product_name": "FFP A+", "mass_g": 300}
  ]'
```

### Step 2: Process an Order
```bash
curl -X POST http://localhost:8080/api/v1/process_order \
  -H "Content-Type: application/json" \
  -d '{
    "order_id": 123,
    "requested": [
      {"product_id": 0, "quantity": 2},
      {"product_id": 10, "quantity": 4}
    ]
  }'
```

Response will show order status (likely PENDING if no inventory).

### Step 3: Restock Inventory
```bash
curl -X POST http://localhost:8080/api/v1/process_restock \
  -H "Content-Type: application/json" \
  -d '[
    {"product_id": 0, "quantity": 30},
    {"product_id": 10, "quantity": 5}
  ]'
```

This will create shipments for pending orders and update order statuses.

### Step 4: Retrieve Shipment Details
```bash
curl http://localhost:8080/api/v1/ship_package/1
```

Returns the shipment with all shipped items.

## Development

### Access H2 Console
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:testdb
Username: sa
Password: (blank)
```

### View Application Logs
```bash
# Enable debug logging
./gradlew bootRun --args='--logging.level.com.inventory=DEBUG'
```

## Troubleshooting

**Port 8080 already in use:**
```bash
./gradlew bootRun --args='--server.port=8081'
```

**Database connection errors:**
- Verify H2 is properly configured in application.yml
- Check that database path is writable

**Build failures:**
- Ensure Java 17+ is installed: `java -version`
- Clear gradle cache: `./gradlew clean`

## Next Steps

1. Read the full [README.md](README.md) for architecture details
2. Check the API documentation in README.md for request/response formats
3. Implement unit tests in `src/test/java/`
4. Consider adding integration tests
5. Deploy to production following deployment considerations in README.md

## Documentation Structure

- **README.md**: Complete system design, architecture, and API documentation
- **QUICKSTART.md**: This file - quick setup and testing
- **Code Comments**: Comprehensive JavaDoc in all classes
- **Inline Documentation**: Business logic explained in service classes

## Support

Refer to README.md for:
- Troubleshooting guide
- Extension points for future development
- Performance considerations
- Production deployment checklist
