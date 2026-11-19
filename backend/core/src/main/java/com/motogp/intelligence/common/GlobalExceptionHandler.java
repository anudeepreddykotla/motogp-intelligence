package com.motogp.intelligence.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.motogp.intelligence.ingestion.InvalidRaceDataException;
import com.motogp.intelligence.ingestion.InvalidReferenceDataException;
import com.motogp.intelligence.ingestion.RaceDataConflictException;
import com.motogp.intelligence.ingestion.ReferenceDataConflictException;
import com.motogp.intelligence.ingestion.SourceRecordConflictException;
import com.motogp.intelligence.race.HistoricalResourceNotFoundException;
import com.motogp.intelligence.race.InvalidHistoricalQueryException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(HistoricalResourceNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> notFound(HistoricalResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler({InvalidRaceDataException.class, InvalidReferenceDataException.class})
    ResponseEntity<ApiResponse<Void>> invalidData(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "HISTORICAL_DATA_INVALID", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidHistoricalQueryException.class)
    ResponseEntity<ApiResponse<Void>> invalidHistoricalQuery(InvalidHistoricalQueryException exception,
                                                               HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage(), request);
    }

    @ExceptionHandler({RaceDataConflictException.class, ReferenceDataConflictException.class,
            SourceRecordConflictException.class})
    ResponseEntity<ApiResponse<Void>> conflictingData(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "HISTORICAL_DATA_CONFLICT", exception.getMessage(), request);
    }
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<Void>> invalidRequestParameter(MethodArgumentTypeMismatchException exception,
                                                                HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Invalid value for parameter: " + exception.getName(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<Void>> missingRequestParameter(MissingServletRequestParameterException exception,
                                                               HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Required parameter is missing: " + exception.getParameterName(), request);
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

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message,
                                                     HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiResponse<>(requestId(request), null,
                List.of(new ApiError(code, message))));
    }
}
