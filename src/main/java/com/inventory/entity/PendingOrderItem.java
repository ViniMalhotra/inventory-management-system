package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * PendingOrderItem Entity - Represents order items that could not be fulfilled
 * immediately.
 * Tracks unfulfilled quantities waiting for restocking.
 * Prioritized by creation timestamp for FIFO processing during restock.
 */
@Entity
@Table(name = "pending_order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "pending_qty", nullable = false)
    private Long pendingQty;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Initialize createdAt timestamp before persistence.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
