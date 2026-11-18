package com.motogp.intelligence.common;

public record ApiError(String code, String message, String field) {
    public ApiError(String code, String message) {
        this(code, message, null);
    }
}
