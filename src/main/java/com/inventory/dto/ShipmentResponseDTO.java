package com.inventory.dto;

import lombok.*;
import java.util.List;

/**
 * ShipmentResponseDTO - Represents shipment information in API responses.
 * Maps to the shipment details including order and shipped items.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentResponseDTO {
    private Long orderId;
    private List<ShippedItemDTO> shipped;
}
