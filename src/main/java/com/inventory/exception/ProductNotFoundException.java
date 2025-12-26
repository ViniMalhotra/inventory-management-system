package com.inventory.exception;

/**
 * ProductNotFoundException - Thrown when a requested product does not exist.
 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }

    public ProductNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
