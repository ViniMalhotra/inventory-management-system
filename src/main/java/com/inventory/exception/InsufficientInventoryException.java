package com.inventory.exception;

/**
 * InsufficientInventoryException - Thrown when inventory is insufficient for
 * order fulfillment.
 */
public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(String message) {
        super(message);
    }

    public InsufficientInventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
