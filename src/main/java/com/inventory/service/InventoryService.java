package com.inventory.service;

import com.inventory.entity.Inventory;
import com.inventory.entity.Product;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * InventoryService - Handles all inventory-related operations.
 * Manages product stock levels, updates, and availability checks.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    /**
     * Initializes inventory for a product with 0 quantity.
     * Called during catalog initialization.
     */
    public Inventory initializeInventoryForProduct(Long productId) {
        Inventory inventory = Inventory.builder()
                .productId(productId)
                .availableQty(0L)
                .build();
        return inventoryRepository.save(inventory);
    }

    /**
     * Retrieves the available quantity for a product.
     * Throws exception if product not found in inventory.
     */
    public Long getAvailableQuantity(Long productId) {
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found in inventory: " + productId));
        return inventory.getAvailableQty();
    }

    /**
     * Retrieves inventory details for a product.
     */
    public Inventory getInventory(Long productId) {
        return inventoryRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Inventory not found for product: " + productId));
    }

    /**
     * Reduces inventory quantity for a product with pessimistic locking.
     * Acquires exclusive lock to prevent concurrent modifications.
     * 
     * Used when items are shipped. Lock ensures that no concurrent
     * restock operations can modify this inventory during the operation.
     * 
     * @param productId Product ID to reduce inventory for
     * @param quantity  Quantity to reduce
     * @throws IllegalArgumentException if insufficient inventory
     */
    public void reduceInventory(Long productId, Long quantity) {
        // Acquire pessimistic lock on inventory record
        Inventory inventory = inventoryRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Inventory not found for product: " + productId));

        Long currentQty = inventory.getAvailableQty();

        if (currentQty < quantity) {
            throw new IllegalArgumentException(
                    "Cannot reduce inventory: available=" + currentQty + ", requested=" + quantity);
        }

        inventory.setAvailableQty(currentQty - quantity);
        inventoryRepository.save(inventory);
        log.info("Reduced inventory for product {} by {}. New quantity: {}",
                productId, quantity, inventory.getAvailableQty());
    }

    /**
     * Increases inventory quantity for a product with pessimistic locking.
     * Acquires exclusive lock to prevent concurrent modifications.
     * 
     * Used when restocking occurs. Lock ensures that no concurrent
     * order/shipment operations can modify this inventory during the operation.
     * 
     * @param productId Product ID to increase inventory for
     * @param quantity  Quantity to increase
     */
    public void increaseInventory(Long productId, Long quantity) {
        // Acquire pessimistic lock on inventory record
        Inventory inventory = inventoryRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Inventory not found for product: " + productId));

        Long newQty = inventory.getAvailableQty() + quantity;
        inventory.setAvailableQty(newQty);
        inventoryRepository.save(inventory);
        log.info("Increased inventory for product {} by {}. New quantity: {}",
                productId, quantity, newQty);
    }

    /**
     * Gets the product details associated with inventory.
     */
    public Product getProductDetails(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found: " + productId));
    }

    /**
     * Checks if a product exists in the inventory.
     */
    public boolean productExistsInInventory(Long productId) {
        return inventoryRepository.existsById(productId);
    }

    /**
     * Retrieves all inventory records.
     */
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    /**
     * Retrieves product details and inventory in a single batch operation.
     * Optimizes database calls for multiple products.
     *
     * @param productIds List of product IDs to fetch
     * @return Map of productId to ProductInventoryData containing product details
     *         and available quantity
     */
    public Map<Long, ProductInventoryData> getProductsWithInventory(List<Long> productIds) {
        return getProductsWithInventory(productIds, false);
    }

    /**
     * Retrieves product details and inventory in a single batch operation with
     * optional locking.
     * Optimizes database calls for multiple products.
     * 
     * When withLock=true, uses pessimistic write locks to prevent concurrent
     * modifications during shipment creation.
     *
     * @param productIds List of product IDs to fetch
     * @param withLock   If true, acquires pessimistic write locks on inventory
     *                   records
     * @return Map of productId to ProductInventoryData containing product details
     *         and available quantity
     */
    public Map<Long, ProductInventoryData> getProductsWithInventory(List<Long> productIds, boolean withLock) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        // Load all inventory records in one query (with optional locking)
        List<Inventory> inventories = withLock
                ? inventoryRepository.findByProductIdInWithLock(productIds)
                : inventoryRepository.findByProductIdIn(productIds);

        Map<Long, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, inv -> inv));

        // Load all products in one query
        List<Product> products = productRepository.findAllById(productIds);

        // Combine product and inventory data
        return products.stream()
                .collect(Collectors.toMap(
                        Product::getProductId,
                        product -> new ProductInventoryData(
                                product,
                                inventoryMap.getOrDefault(product.getProductId(), new Inventory())
                                        .getAvailableQty())));
    }

    /**
     * DTO to hold product details and available inventory quantity.
     */
    public static class ProductInventoryData {
        public Product product;
        public Long availableQty;

        public ProductInventoryData(Product product, Long availableQty) {
            this.product = product;
            this.availableQty = availableQty;
        }

        public Product getProduct() {
            return product;
        }

        public Long getAvailableQty() {
            return availableQty;
        }
    }
}
