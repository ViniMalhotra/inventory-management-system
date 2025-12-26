package com.inventory.dto;

import lombok.*;

/**
 * ProductDTO - Data transfer object for product information.
 * Used in API requests and responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Long productId;
    private String productName;
    private Integer massG;
}
