package com.inventory.dto;

import lombok.*;

/**
 * OrderItemRequestDTO - Represents a requested item in an order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequestDTO {
    private Long productId;
    private Long quantity;
}
