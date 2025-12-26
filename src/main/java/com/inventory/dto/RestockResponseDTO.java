package com.inventory.dto;

import lombok.*;

/**
 * RestockResponseDTO - Response DTO for restock operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestockResponseDTO {
    private Integer productsRestocked;
    private Integer shipmentsCreated;
    private Integer ordersUpdated;
}
