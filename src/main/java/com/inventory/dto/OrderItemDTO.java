package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OrderItemDTO - Data Transfer Object for order items in API responses.
 * Represents a single line item within an order with fulfillment details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {

    /**
     * Product ID for this line item
     */
    private Long productId;

    /**
     * Quantity requested by customer
     */
    private Long requestedQty;

    /**
     * Quantity already fulfilled/shipped
     */
    private Long fulfilledQty;

    /**
     * Status of this line item: PENDING, PARTIALLY_FULFILLED, FULFILLED
     */
    private String status;
}
