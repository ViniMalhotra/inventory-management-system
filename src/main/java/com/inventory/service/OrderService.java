package com.inventory.service;

import com.inventory.dto.OrderRequestDTO;
import com.inventory.entity.*;
import com.inventory.exception.OrderNotFoundException;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/**
 * OrderService - Handles all order-related operations.
 * Manages order creation, processing, and status updates.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PendingOrderItemRepository pendingOrderItemRepository;
    private final InventoryService inventoryService;
    private final ShipmentService shipmentService;

    /**
     * Processes a new order.
     * 
     * Flow:
     * 1. Validate all products exist in inventory
     * 2. Create order and order items
     * 3. Create shipments for available inventory
     * 4. Create pending order items for unfulfilled portions
     * 5. Update order status
     */
    public Order processOrder(OrderRequestDTO orderRequest) {
        Long orderId = orderRequest.getOrderId();

        log.info("Processing order {}", orderId);

        // Validate all products exist in inventory
        for (var item : orderRequest.getRequested()) {
            if (!inventoryService.productExistsInInventory(item.getProductId())) {
                throw new ProductNotFoundException(
                        "Product not found in inventory: " + item.getProductId());
            }
        }

        // Create order
        Order order = Order.builder()
                .orderId(orderId)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .shipments(new ArrayList<>())
                .build();
        order = orderRepository.save(order);
        log.info("Created order {}", orderId);

        // Create order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (var item : orderRequest.getRequested()) {
            OrderItem orderItem = OrderItem.builder()
                    .orderId(orderId)
                    .productId(item.getProductId())
                    .requestedQty(item.getQuantity())
                    .fulfilledQty(0L)
                    .status("PENDING")
                    .build();
            orderItem = orderItemRepository.save(orderItem);
            orderItems.add(orderItem);
        }
        log.info("Created {} order items for order {}", orderItems.size(), orderId);

        // Create shipments for available inventory
        List<Shipment> shipments = shipmentService.createShipments(orderId, orderItems, order, orderItemRepository);
        log.info("Created {} shipments for order {}", shipments.size(), orderId);

        // Create pending order items for unfulfilled portions
        List<OrderItem> updatedItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : updatedItems) {
            long pendingQty = item.getRequestedQty() - item.getFulfilledQty();
            if (pendingQty > 0) {
                PendingOrderItem pending = PendingOrderItem.builder()
                        .orderId(orderId)
                        .productId(item.getProductId())
                        .pendingQty(pendingQty)
                        .createdAt(LocalDateTime.now())
                        .build();
                pendingOrderItemRepository.save(pending);
                log.info("Created pending item for product {} with qty {}", item.getProductId(), pendingQty);
            }
        }

        // Update order status based on fulfillment
        updateOrderStatus(orderId);

        return orderRepository.findById(orderId).orElseThrow();
    }

    /**
     * Updates order status based on the fulfillment of order items.
     * Status logic:
     * - All items fulfilled -> FULFILLED
     * - Some items fulfilled -> PARTIALLY_FULFILLED
     * - No items fulfilled -> PENDING
     */
    public void updateOrderStatus(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        if (items.isEmpty()) {
            return;
        }

        long fulfilledCount = items.stream()
                .filter(item -> "FULFILLED".equals(item.getStatus()))
                .count();

        String newStatus;
        if (fulfilledCount == items.size()) {
            newStatus = "FULFILLED";
        } else if (fulfilledCount > 0) {
            newStatus = "PARTIALLY_FULFILLED";
        } else {
            newStatus = "PENDING";
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        order.setStatus(newStatus);
        orderRepository.save(order);
        log.info("Updated order {} status to {}", orderId, newStatus);
    }

    /**
     * Updates order status to COMPLETED when all pending items are fulfilled.
     */
    public void completeOrderIfAllFulfilled(Long orderId) {
        List<PendingOrderItem> pendingItems = pendingOrderItemRepository.findByOrderId(orderId);

        if (pendingItems.isEmpty()) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
            order.setStatus("COMPLETED");
            orderRepository.save(order);
            log.info("Order {} completed", orderId);
        }
    }

    /**
     * Retrieves order details.
     */
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    /**
     * Retrieves all order items for an order.
     */
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    /**
     * Retrieves all pending items for an order.
     */
    public List<PendingOrderItem> getPendingItems(Long orderId) {
        return pendingOrderItemRepository.findByOrderId(orderId);
    }
}
