package com.inventory.repository;

import com.inventory.entity.PendingOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * PendingOrderItemRepository - JPA repository for PendingOrderItem entity.
 */
@Repository
public interface PendingOrderItemRepository extends JpaRepository<PendingOrderItem, Long> {
    /**
     * Find pending items by product ID, ordered by creation time (oldest first).
     */
    List<PendingOrderItem> findByProductIdOrderByCreatedAt(Long productId);

    /**
     * Find all pending items for an order.
     */
    List<PendingOrderItem> findByOrderId(Long orderId);

    /**
     * Find pending items by order ID and product ID.
     */
    List<PendingOrderItem> findByOrderIdAndProductId(Long orderId, Long productId);
}
