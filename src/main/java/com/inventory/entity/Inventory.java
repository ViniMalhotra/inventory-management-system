package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Inventory Entity - Tracks available quantity of each product.
 * Maintains the current stock level for order fulfillment and shipment
 * creation.
 */
@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "available_qty", nullable = false)
    private Long availableQty;

    /**
     * One-to-one relationship with Product.
     * Foreign key is product_id.
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
}
