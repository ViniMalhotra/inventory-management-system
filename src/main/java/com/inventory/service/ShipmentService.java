package com.inventory.service;

import com.inventory.entity.*;
import com.inventory.exception.ShipmentNotFoundException;
import com.inventory.repository.*;
import com.inventory.util.ShipmentPackagingOptimizer;
import com.inventory.util.ShipmentPackagingOptimizer.ShipmentPackage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * ShipmentService - Handles all shipment-related operations.
 * Creates shipments respecting weight constraints and optimizes packaging.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final InventoryService inventoryService;

    /**
     * Creates shipments for an order based on available inventory.
     * Optimizes packaging to minimize shipment count while respecting weight
     * limits.
     * Updates inventory and order item fulfillment status.
     * 
     * @param orderId             Order ID to create shipments for
     * @param orderItems          List of order items to ship
     * @param order               The order entity
     * @param orderItemRepository Repository for order items
     * @return List of created shipments
     */
    public List<Shipment> createShipments(Long orderId, List<OrderItem> orderItems,
            Order order, OrderItemRepository orderItemRepository) {
        List<Shipment> createdShipments = new ArrayList<>();

        // Extract product IDs from order items
        List<Long> productIds = orderItems.stream()
                .map(OrderItem::getProductId)
                .toList();

        // Load all product and inventory data with pessimistic locks
        // This prevents concurrent modifications to inventory during the entire
        // shipment creation loop
        Map<Long, InventoryService.ProductInventoryData> productInventoryMap = inventoryService
                .getProductsWithInventory(productIds, true);

        // Prepare items for packing optimization
        List<ShipmentPackagingOptimizer.ShipmentItem> itemsToPack = new ArrayList<>();
        Map<Long, OrderItem> orderItemMap = new HashMap<>();

        for (OrderItem item : orderItems) {
            InventoryService.ProductInventoryData data = productInventoryMap.get(item.getProductId());

            if (data == null) {
                log.warn("Product {} not found in inventory", item.getProductId());
                continue;
            }

            Long availableQty = data.getAvailableQty();

            // Only ship what's available and needed
            Long qtyToShip = Math.min(availableQty, item.getRequestedQty() - item.getFulfilledQty());

            if (qtyToShip > 0) {
                ShipmentPackagingOptimizer.ShipmentItem shipmentItem = new ShipmentPackagingOptimizer.ShipmentItem(
                        item.getProductId(),
                        qtyToShip,
                        data.getProduct().getMassG());
                itemsToPack.add(shipmentItem);
                orderItemMap.put(item.getProductId(), item);
            }
        }

        if (itemsToPack.isEmpty()) {
            log.info("No items to ship for order {}", orderId);
            return createdShipments;
        }

        // Optimize packing
        List<ShipmentPackage> optimizedPackages = ShipmentPackagingOptimizer.optimizePackaging(itemsToPack);

        // Create shipments from optimized packages
        for (ShipmentPackage pkg : optimizedPackages) {
            Shipment shipment = Shipment.builder()
                    .orderId(orderId)
                    .totalWeightG(pkg.totalWeightG)
                    .build();

            shipment = shipmentRepository.save(shipment);
            log.info("Created shipment {} for order {} with weight {}g",
                    shipment.getShipmentId(), orderId, pkg.totalWeightG);

            // Create shipment items and update order items
            for (ShipmentPackagingOptimizer.ShipmentItem pkgItem : pkg.items) {
                com.inventory.entity.ShipmentItem shipmentItem = com.inventory.entity.ShipmentItem.builder()
                        .shipmentId(shipment.getShipmentId())
                        .productId(pkgItem.productId)
                        .quantity(pkgItem.quantity)
                        .build();
                shipmentItemRepository.save(shipmentItem);

                // Update order item fulfillment
                OrderItem orderItem = orderItemMap.get(pkgItem.productId);
                if (orderItem != null) {
                    long newFulfilledQty = orderItem.getFulfilledQty() + pkgItem.quantity;
                    orderItem.setFulfilledQty(newFulfilledQty);

                    // Update order item status
                    if (newFulfilledQty >= orderItem.getRequestedQty()) {
                        orderItem.setStatus("FULFILLED");
                    } else {
                        orderItem.setStatus("PARTIALLY_FULFILLED");
                    }
                    orderItemRepository.save(orderItem);

                    // Reduce inventory
                    inventoryService.reduceInventory(pkgItem.productId, pkgItem.quantity);
                    log.info("Shipped {} units of product {} in shipment {}",
                            pkgItem.quantity, pkgItem.productId, shipment.getShipmentId());
                }
            }

            createdShipments.add(shipment);
        }

        return createdShipments;
    }

    /**
     * Retrieves shipment details by shipment ID.
     */
    public Shipment getShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found: " + shipmentId));
    }

    /**
     * Retrieves all shipment items for a shipment.
     */
    public List<com.inventory.entity.ShipmentItem> getShipmentItems(Long shipmentId) {
        return shipmentItemRepository.findByShipmentId(shipmentId);
    }

    /**
     * Gets all shipments for an order.
     */
    public List<Shipment> getShipmentsForOrder(Long orderId) {
        return shipmentRepository.findByOrderId(orderId);
    }

    /**
     * Validates shipment weight constraint.
     */
    public boolean isValidShipmentWeight(Integer totalWeightG) {
        return totalWeightG <= ShipmentPackagingOptimizer.getMaxShipmentWeightG();
    }
}
