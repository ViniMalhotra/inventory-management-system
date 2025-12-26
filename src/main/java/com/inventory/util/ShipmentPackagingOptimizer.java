package com.inventory.util;

import com.inventory.entity.Product;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * ShipmentPackagingOptimizer - Handles bin packing algorithm for shipments.
 * Uses a greedy first-fit decreasing algorithm to minimize shipment count
 * while respecting the 1.8 KG (1800g) weight limit per shipment.
 */
@Slf4j
public class ShipmentPackagingOptimizer {

    private static final int MAX_SHIPMENT_WEIGHT_G = 1800;

    /**
     * Represents a shipment package with items.
     */
    public static class ShipmentPackage {
        public List<ShipmentItem> items = new ArrayList<>();
        public int totalWeightG = 0;

        public boolean canAddItem(ShipmentItem item) {
            return (totalWeightG + item.totalWeightG) <= MAX_SHIPMENT_WEIGHT_G;
        }

        public void addItem(ShipmentItem item) {
            items.add(item);
            totalWeightG += item.totalWeightG;
        }
    }

    /**
     * Represents an item to be shipped.
     */
    public static class ShipmentItem {
        public Long productId;
        public Long quantity;
        public int unitWeightG;
        public int totalWeightG;

        public ShipmentItem(Long productId, Long quantity, int unitWeightG) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitWeightG = unitWeightG;
            this.totalWeightG = (int) (unitWeightG * quantity);
        }
    }

    /**
     * Optimizes packing of items into shipments using a greedy first-fit decreasing
     * algorithm with automatic splitting for oversized items.
     * 
     * Algorithm:
     * 1. Pre-process items: Split any item whose total weight exceeds max shipment
     * weight
     * into multiple smaller items to fit within the limit
     * 2. Sort items by weight in descending order (heaviest first)
     * 3. For each item, place it in the first bin (shipment) that has enough space
     * 4. If no bin has space, create a new bin
     * 
     * This approach minimizes the number of shipments needed while handling
     * oversized items.
     *
     * @param items List of items to ship
     * @return List of optimized shipment packages
     */
    public static List<ShipmentPackage> optimizePackaging(List<ShipmentItem> items) {
        // Pre-process: split items that exceed max weight
        List<ShipmentItem> processedItems = new ArrayList<>();
        for (ShipmentItem item : items) {
            if (item.totalWeightG <= MAX_SHIPMENT_WEIGHT_G) {
                processedItems.add(item);
            } else {
                // Split item into multiple smaller items
                List<ShipmentItem> splitItems = splitOversizedItem(item);
                processedItems.addAll(splitItems);
                log.debug("Split product {} (qty: {}) into {} shipment items",
                        item.productId, item.quantity, splitItems.size());
            }
        }

        // Sort items by total weight in descending order (heaviest first)
        processedItems.sort((a, b) -> Integer.compare(b.totalWeightG, a.totalWeightG));

        List<ShipmentPackage> packages = new ArrayList<>();

        // First-fit decreasing: place each item in the first package that fits
        for (ShipmentItem item : processedItems) {
            boolean placed = false;

            // Try to fit in existing packages
            for (ShipmentPackage pkg : packages) {
                if (pkg.canAddItem(item)) {
                    pkg.addItem(item);
                    placed = true;
                    break;
                }
            }

            // Create new package if item doesn't fit in any existing package
            if (!placed) {
                ShipmentPackage newPackage = new ShipmentPackage();
                // After splitting, every item should fit in an empty package
                newPackage.addItem(item);
                packages.add(newPackage);
            }
        }

        log.info("Optimized {} original items into {} shipments (after splitting {} items)",
                items.size(), packages.size(), items.size() - processedItems.size() +
                        processedItems.stream().filter(i -> i.totalWeightG <= MAX_SHIPMENT_WEIGHT_G).count());
        return packages;
    }

    /**
     * Splits an oversized item into multiple smaller items that fit within the max
     * shipment weight.
     * 
     * Strategy:
     * - Calculate how many units can fit per shipment based on unit weight
     * - Distribute remaining quantity across additional shipments
     * 
     * @param item The oversized item to split
     * @return List of split shipment items
     */
    private static List<ShipmentItem> splitOversizedItem(ShipmentItem item) {
        List<ShipmentItem> splitItems = new ArrayList<>();

        // Calculate max quantity that can fit in one shipment
        long maxQtyPerShipment = MAX_SHIPMENT_WEIGHT_G / item.unitWeightG;

        // If even a single unit exceeds max weight, we have a fundamental constraint
        if (maxQtyPerShipment == 0) {
            throw new IllegalArgumentException(
                    "Single unit of product (product_id: " + item.productId +
                            ", weight: " + item.unitWeightG + "g) exceeds maximum shipment weight of " +
                            MAX_SHIPMENT_WEIGHT_G + "g");
        }

        // Split quantity across multiple shipments
        long remainingQty = item.quantity;
        while (remainingQty > 0) {
            long qtyForThisShipment = Math.min(remainingQty, maxQtyPerShipment);
            ShipmentItem splitItem = new ShipmentItem(item.productId, qtyForThisShipment, item.unitWeightG);
            splitItems.add(splitItem);
            remainingQty -= qtyForThisShipment;
        }

        return splitItems;
    }

    /**
     * Validates if an item can fit in a single shipment.
     */
    public static boolean canFitInSingleShipment(Product product, Long quantity) {
        int totalWeight = product.getMassG() * (int) quantity.longValue();
        return totalWeight <= MAX_SHIPMENT_WEIGHT_G;
    }

    /**
     * Gets the maximum shipment weight limit in grams.
     */
    public static int getMaxShipmentWeightG() {
        return MAX_SHIPMENT_WEIGHT_G;
    }
}
