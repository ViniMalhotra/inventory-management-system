package com.inventory.exception;

/**
 * ShipmentNotFoundException - Thrown when a requested shipment does not exist.
 */
public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(String message) {
        super(message);
    }

    public ShipmentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
