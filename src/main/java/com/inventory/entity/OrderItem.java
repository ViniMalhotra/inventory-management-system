package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * OrderItem Entity - Represents a specific product request within an order.
 * Tracks requested quantity, fulfilled quantity, and status of each line item.
 * Status values: PENDING, PARTIALLY_FULFILLED, FULFILLED
 */
@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "requested_qty", nullable = false)
    private Long requestedQty;

    @Column(name = "fulfilled_qty", nullable = false)
    @Builder.Default
    private Long fulfilledQty = 0L;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, PARTIALLY_FULFILLED, FULFILLED

    /**
     * Many-to-one relationship with Order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    /**
     * Many-to-one relationship with Product.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
}
