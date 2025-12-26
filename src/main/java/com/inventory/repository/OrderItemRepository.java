package com.inventory.repository;

import com.inventory.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * OrderItemRepository - JPA repository for OrderItem entity.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    /**
     * Find order items by order ID.
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Find order items by order ID and status.
     */
    List<OrderItem> findByOrderIdAndStatus(Long orderId, String status);
}
