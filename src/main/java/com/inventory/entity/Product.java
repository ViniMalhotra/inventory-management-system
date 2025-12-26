package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Product Entity - Represents a product that can be ordered and stocked.
 * Contains product metadata including mass for shipment weight calculations.
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "mass_g", nullable = false)
    private Integer massG;

    /**
     * One product can have many inventory records (logically one-to-one in this
     * system).
     */
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
    private Inventory inventory;
}
