package com.inventory.repository;

import com.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * InventoryRepository - JPA repository for Inventory entity.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    /**
     * Find inventory records by product IDs.
     */
    List<Inventory> findByProductIdIn(List<Long> productIds);

    /**
     * Find inventory by product ID with pessimistic write lock.
     * Prevents concurrent modifications to the inventory record.
     * 
     * This lock ensures that only one transaction can read and modify
     * the inventory at a time, eliminating race conditions between
     * processOrder() and processRestock() operations.
     * 
     * @param productId Product ID to lock
     * @return Optional containing locked inventory record
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByIdWithLock(@Param("productId") Long productId);

    /**
     * Find multiple inventory records by product IDs with pessimistic write lock.
     * Prevents concurrent modifications to multiple inventory records.
     * 
     * @param productIds List of product IDs to lock
     * @return List of locked inventory records
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds ORDER BY i.productId")
    List<Inventory> findByProductIdInWithLock(@Param("productIds") List<Long> productIds);
}
