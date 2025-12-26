package com.inventory.dto;

import lombok.*;
import java.util.List;

/**
 * OrderRequestDTO - Represents a new order request.
 * Contains order ID and list of requested items.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDTO {
    private Long orderId;
    private List<OrderItemRequestDTO> requested;
}
