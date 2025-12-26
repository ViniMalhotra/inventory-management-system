package com.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.inventory.dto.ApiResponseDTO;

/**
 * GlobalExceptionHandler - Centralized exception handling for all controllers.
 * Converts exceptions to appropriate HTTP responses with consistent format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.<Void>builder()
                        .success(false)
                        .message("Product not found")
                        .error(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.<Void>builder()
                        .success(false)
                        .message("Order not found")
                        .error(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleShipmentNotFound(ShipmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.<Void>builder()
                        .success(false)
                        .message("Shipment not found")
                        .error(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleInsufficientInventory(InsufficientInventoryException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.<Void>builder()
                        .success(false)
                        .message("Insufficient inventory")
                        .error(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.<Void>builder()
                        .success(false)
                        .message("Invalid request")
                        .error(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.<Void>builder()
                        .success(false)
                        .message("Internal server error")
                        .error(ex.getMessage())
                        .build());
    }
}
