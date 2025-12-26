package com.inventory.dto;

import lombok.*;

/**
 * ShippedItemDTO - Represents a shipped product (inner class extracted).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippedItemDTO {
    private Long productId;
    private Long quantity;
}
