package com.inventory.dto;

import lombok.*;

/**
 * RestockItemDTO - Represents a product to be restocked.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestockItemDTO {
    private Long productId;
    private Long quantity;
}
