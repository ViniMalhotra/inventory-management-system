package com.inventory.exception;

/**
 * OrderNotFoundException - Thrown when a requested order does not exist.
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
