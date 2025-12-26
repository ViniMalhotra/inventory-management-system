package com.inventory.dto;

import lombok.*;

/**
 * ApiResponseDTO - Generic response wrapper for all API endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponseDTO<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;
}
