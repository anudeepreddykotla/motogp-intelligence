package com.motogp.intelligence.common;

import java.util.List;

public record ApiResponse<T>(String requestId, T data, List<ApiError> errors) {
    public static <T> ApiResponse<T> success(String requestId, T data) {
        return new ApiResponse<>(requestId, data, List.of());
    }
}
