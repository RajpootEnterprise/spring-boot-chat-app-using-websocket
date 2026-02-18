package com.chatapp.exception;

import com.chatapp.dto.ChatDTOs.ApiResponse;
import com.chatapp.exception.ChatExceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for REST API and WebSocket exceptions.
 *
 * Intercepts all exceptions thrown by controllers and services,
 * logs them appropriately, and returns standardized JSON error responses.
 *
 * Benefits:
 * - Consistent error response format across the entire API
 * - Centralized logging of all errors
 * - Clean separation of error handling from business logic
 * - Proper HTTP status codes for each error type
 *
 * @author ChatApp Team
 */
@Slf4j
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    // ─── Chat Application Exceptions ─────────────────────────────────────────

    /**
     * Handles all custom ChatAppException subclasses.
     * Each exception carries its own HTTP status and error code.
     */
    @ExceptionHandler(ChatAppException.class)
    public ResponseEntity<ApiResponse<Void>> handleChatAppException(
            ChatAppException ex, HttpServletRequest request) {

        log.warn("ChatAppException [{}] on [{}]: {}",
                ex.getErrorCode(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request) {

        log.warn("User not found on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(
            UserAlreadyExistsException ex, HttpServletRequest request) {

        log.warn("User conflict on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<ApiResponse<Void>> handleImageUpload(
            ImageUploadException ex, HttpServletRequest request) {

        log.error("Image upload failed on [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedFileType(
            UnsupportedFileTypeException ex) {

        log.warn("Unsupported file type: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ─── Spring Framework Exceptions ─────────────────────────────────────────

    /**
     * Handles @Valid validation failures on request bodies.
     * Returns a map of field → error message for client-side form highlighting.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .error("VALIDATION_ERROR")
                        .build());
    }

    /**
     * Handles file upload size limit exceeded (Spring's multipart limit).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex) {

        log.warn("File size limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("File size exceeds maximum allowed limit (10MB)"));
    }

    /**
     * Handles WebSocket message mapping validation failures.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebSocketValidation(
            MethodArgumentNotValidException ex) {

        log.warn("WebSocket message validation failed: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid WebSocket message format"));
    }

    // ─── Generic Exception Handler ────────────────────────────────────────────

    /**
     * Catch-all handler for unexpected exceptions.
     * Logs the full stack trace and returns a generic error message.
     *
     * IMPORTANT: Never expose internal error details to clients in production.
     * The generic message "An unexpected error occurred" is intentional.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error on [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again."));
    }
}
