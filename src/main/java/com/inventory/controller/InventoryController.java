package com.inventory.controller;

import com.inventory.dto.*;
import com.inventory.entity.*;
import com.inventory.service.InventoryService;
import com.inventory.service.OrderService;
import com.inventory.service.ShipmentService;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.PendingOrderItemRepository;
import com.inventory.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * InventoryController - REST controller for inventory management API endpoints.
 * Exposes endpoints for catalog initialization, order processing, restocking,
 * and shipment retrieval.
 */
@RestController
@RequestMapping("/v1")
@Slf4j
@RequiredArgsConstructor
public class InventoryController {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final ShipmentService shipmentService;
    private final PendingOrderItemRepository pendingOrderItemRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * POST /v1/init_catalog
     * Initializes the product catalog and inventory.
     * Creates product records and sets initial inventory to 0 for each product.
     * 
     * Request Body: List<ProductDTO>
     * Example: [{"product_id": 0, "product_name": "RBC A+ Adult", "mass_g": 700}]
     */
    @PostMapping("/init_catalog")
    public ResponseEntity<ApiResponseDTO<String>> initializeCatalog(@RequestBody List<ProductDTO> products) {
        log.info("Initializing catalog with {} products", products.size());

        try {
            for (ProductDTO productDTO : products) {
                // Create product
                Product product = Product.builder()
                        .productId(productDTO.getProductId())
                        .productName(productDTO.getProductName())
                        .massG(productDTO.getMassG())
                        .build();
                productRepository.save(product);

                // Initialize inventory with 0 quantity
                inventoryService.initializeInventoryForProduct(productDTO.getProductId());

                log.debug("Initialized product {} with mass {}g",
                        productDTO.getProductId(), productDTO.getMassG());
            }

            String message = "Catalog initialized successfully with " + products.size() + " products";
            log.info(message);

            return ResponseEntity.ok(ApiResponseDTO.<String>builder()
                    .success(true)
                    .message(message)
                    .data(message)
                    .build());
        } catch (Exception e) {
            log.error("Error initializing catalog", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.<String>builder()
                            .success(false)
                            .message("Failed to initialize catalog")
                            .error(e.getMessage())
                            .build());
        }
    }

    /**
     * POST /v1/process_order
     * Processes a new order.
     * 
     * Flow:
     * 1. Validates products exist in inventory
     * 2. Creates order and order items
     * 3. Creates shipments for available inventory
     * 4. Creates pending items for unfulfilled portions
     * 
     * Request Body: OrderRequestDTO
     * Example: {"order_id": 123, "requested": [{"product_id": 0, "quantity": 2}]}
     */
    @PostMapping("/process_order")
    public ResponseEntity<ApiResponseDTO<OrderResponseDTO>> processOrder(
            @RequestBody OrderRequestDTO orderRequest) {
        log.info("Processing order {}", orderRequest.getOrderId());

        try {
            Order order = orderService.processOrder(orderRequest);

            // Build response with order details
            List<OrderItem> orderItems = orderService.getOrderItems(order.getOrderId());
            OrderResponseDTO response = OrderResponseDTO.builder()
                    .orderId(order.getOrderId())
                    .status(order.getStatus())
                    .createdAt(order.getCreatedAt())
                    .totalItems(orderItems.size())
                    .items(orderItems.stream()
                            .map(item -> OrderItemDTO.builder()
                                    .productId(item.getProductId())
                                    .requestedQty(item.getRequestedQty())
                                    .fulfilledQty(item.getFulfilledQty())
                                    .status(item.getStatus())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

            return ResponseEntity.ok(ApiResponseDTO.<OrderResponseDTO>builder()
                    .success(true)
                    .message("Order processed successfully")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error processing order", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.<OrderResponseDTO>builder()
                            .success(false)
                            .message("Failed to process order")
                            .error(e.getMessage())
                            .build());
        }
    }

    /**
     * POST /v1/process_restock
     * Restocks inventory and fulfills pending orders.
     * 
     * Flow:
     * 1. Updates inventory quantities
     * 2. Processes pending order items in FIFO order
     * 3. Creates shipments for fulfilled pending items
     * 4. Updates order statuses
     * 
     * Request Body: List<RestockItemDTO>
     * Example: [{"product_id": 0, "quantity": 30}]
     */
    @PostMapping("/process_restock")
    public ResponseEntity<ApiResponseDTO<RestockResponseDTO>> processRestock(
            @RequestBody List<RestockItemDTO> restockItems) {
        log.info("Processing restock for {} products", restockItems.size());

        try {
            List<Long> shipmentsCreated = new ArrayList<>();
            List<Long> ordersUpdated = new ArrayList<>();

            for (RestockItemDTO restockItem : restockItems) {
                Long productId = restockItem.getProductId();
                Long quantity = restockItem.getQuantity();

                log.info("Restocking product {} with quantity {}", productId, quantity);

                // Update inventory
                inventoryService.increaseInventory(productId, quantity);

                // Get all pending items for this product, ordered by creation time (oldest
                // first)
                List<PendingOrderItem> pendingItems = pendingOrderItemRepository
                        .findByProductIdOrderByCreatedAt(productId);

                long remainingQuantity = quantity;

                for (PendingOrderItem pending : pendingItems) {
                    if (remainingQuantity <= 0)
                        break;

                    Long orderId = pending.getOrderId();
                    Order order = orderService.getOrder(orderId);
                    List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

                    // Create shipments for pending items
                    List<Shipment> shipments = shipmentService.createShipments(
                            orderId, orderItems, order, orderItemRepository);

                    shipmentsCreated.addAll(shipments.stream()
                            .map(Shipment::getShipmentId)
                            .collect(Collectors.toList()));

                    // Remove fulfilled pending items
                    List<PendingOrderItem> remainingPending = orderService.getPendingItems(orderId);
                    if (remainingPending.isEmpty()) {
                        orderService.completeOrderIfAllFulfilled(orderId);
                        ordersUpdated.add(orderId);
                    }

                    // Update remaining quantity (simplified - in production, track more granularly)
                    remainingQuantity -= pending.getPendingQty();
                }
            }

            RestockResponseDTO response = RestockResponseDTO.builder()
                    .productsRestocked(restockItems.size())
                    .shipmentsCreated(shipmentsCreated.size())
                    .ordersUpdated(ordersUpdated.size())
                    .build();

            return ResponseEntity.ok(ApiResponseDTO.<RestockResponseDTO>builder()
                    .success(true)
                    .message("Restock processed successfully")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error processing restock", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDTO.<RestockResponseDTO>builder()
                            .success(false)
                            .message("Failed to process restock")
                            .error(e.getMessage())
                            .build());
        }
    }

    /**
     * GET /v1/ship_package/{shipmentId}
     * Retrieves shipment details for a given shipment ID.
     * 
     * Response: ShipmentResponseDTO with order ID and shipped items
     */
    @GetMapping("/ship_package/{shipmentId}")
    public ResponseEntity<ApiResponseDTO<ShipmentResponseDTO>> getShipment(
            @PathVariable Long shipmentId) {
        log.info("Retrieving shipment {}", shipmentId);

        try {
            Shipment shipment = shipmentService.getShipment(shipmentId);
            List<com.inventory.entity.ShipmentItem> items = shipmentService.getShipmentItems(shipmentId);

            List<ShippedItemDTO> shippedItems = items.stream()
                    .map(item -> ShippedItemDTO.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            ShipmentResponseDTO response = ShipmentResponseDTO.builder()
                    .orderId(shipment.getOrderId())
                    .shipped(shippedItems)
                    .build();

            return ResponseEntity.ok(ApiResponseDTO.<ShipmentResponseDTO>builder()
                    .success(true)
                    .message("Shipment retrieved successfully")
                    .data(response)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving shipment", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.<ShipmentResponseDTO>builder()
                            .success(false)
                            .message("Failed to retrieve shipment")
                            .error(e.getMessage())
                            .build());
        }
    }
}
