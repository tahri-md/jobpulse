package com.jobpulse.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.jobpulse.dto.others.JobFailureReason;
import com.jobpulse.exception.BadRequestException;
import com.jobpulse.exception.JobExecutionException;
import com.jobpulse.exception.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST endpoints.
 * Provides consistent error responses across the application.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(JobExecutionException.class)
    public ResponseEntity<ErrorResponse> handleJobExecutionException(JobExecutionException ex, WebRequest request) {
        log.error("Job execution error - Reason: {}, Message: {}", ex.getReason(), ex.getMessage());
        
        HttpStatus status = determineStatusFromFailureReason(ex.getReason());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error("Job Execution Error")
            .message(ex.getMessage())
            .details(Map.of("failureReason", ex.getReason().toString()))
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return new ResponseEntity<>(errorResponse, status);
    }

    // @ExceptionHandler(MethodArgumentNotValidException.class)
    // public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
    //     log.warn("Validation error: {}", ex.getMessage());
        
    //     Map<String, String> fieldErrors = new HashMap<>();
    //     ex.getBindingResult().getAllErrors().forEach(error -> {
    //         String fieldName = ((FieldError) error).getField();
    //         String errorMessage = error.getDefaultMessage();
    //         fieldErrors.put(fieldName, errorMessage);
    //     });
        
    //     ErrorResponse errorResponse = ErrorResponse.builder()
    //         .timestamp(LocalDateTime.now())
    //         .status(HttpStatus.BAD_REQUEST.value())
    //         .error("Validation Failed")
    //         .message("Input validation failed")
    //         .details(fieldErrors)
    //         .path(request.getDescription(false).replace("uri=", ""))
    //         .build();
            
    //     return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    // }
    @Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
    MethodArgumentNotValidException ex,
    HttpHeaders headers,
    HttpStatusCode status,
    WebRequest request
) {
    log.warn("Validation error: {}", ex.getMessage());

    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error ->
        fieldErrors.put(error.getField(), error.getDefaultMessage())
    );

    ErrorResponse errorResponse = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Validation Failed")
        .message("Input validation failed")
        .details(fieldErrors)
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
}


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Argument")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus determineStatusFromFailureReason(JobFailureReason reason) {
        return switch (reason) {
            case INVALID_CONFIG, BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case AUTH_ERROR -> HttpStatus.UNAUTHORIZED;
            case NETWORK_ERROR, TIMEOUT, RATE_LIMITED, REMOTE_5XX -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    @lombok.Data
    @lombok.Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private Map<String, String> details;
        private String path;
    }
}
