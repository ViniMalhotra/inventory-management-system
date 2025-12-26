package com.inventory.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderResponseDTO - Response DTO for order information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {
    private Long orderId;
    private String status;
    private LocalDateTime createdAt;
    private Integer totalItems;
    private List<OrderItemDTO> items;
}
