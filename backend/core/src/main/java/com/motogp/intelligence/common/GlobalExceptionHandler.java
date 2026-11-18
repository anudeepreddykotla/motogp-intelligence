package com.motogp.intelligence.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException exception,
                                                   HttpServletRequest request) {
        List<ApiError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError("VALIDATION_ERROR", error.getDefaultMessage(), error.getField()))
                .toList();
        return ResponseEntity.badRequest().body(new ApiResponse<>(requestId(request), null, errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> constraint(ConstraintViolationException exception,
                                                  HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiResponse<>(requestId(request), null,
                List.of(new ApiError("VALIDATION_ERROR", exception.getMessage()))));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(requestId(request), null,
                        List.of(new ApiError("INTERNAL_ERROR", "An unexpected error occurred."))));
    }

    private String requestId(HttpServletRequest request) {
        return request.getHeader(CorrelationIdFilter.HEADER);
    }
}
