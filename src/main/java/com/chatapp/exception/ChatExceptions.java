package com.chatapp.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom Exception Classes for the Chat Application.
 *
 * Hierarchy:
 *   ChatAppException (base)
 *   ├── UserNotFoundException
 *   ├── UserAlreadyExistsException
 *   ├── MessageNotFoundException
 *   ├── ImageUploadException
 *   ├── InvalidMessageException
 *   └── WebSocketException
 *
 * Each exception carries:
 * - HTTP status for REST API responses
 * - Error code for client-side handling
 * - Human-readable message
 *
 * @author ChatApp Team
 */
public class ChatExceptions {

    // ─── Base Exception ──────────────────────────────────────────────────────

    @Getter
    public static class ChatAppException extends RuntimeException {
        private final HttpStatus status;
        private final String errorCode;

        public ChatAppException(String message, HttpStatus status, String errorCode) {
            super(message);
            this.status = status;
            this.errorCode = errorCode;
        }

        public ChatAppException(String message, HttpStatus status, String errorCode, Throwable cause) {
            super(message, cause);
            this.status = status;
            this.errorCode = errorCode;
        }
    }

    // ─── User Exceptions ─────────────────────────────────────────────────────

    /**
     * Thrown when a requested user cannot be found in the database.
     */
    public static class UserNotFoundException extends ChatAppException {
        public UserNotFoundException(String username) {
            super("User not found: " + username, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
        }
    }

    /**
     * Thrown when attempting to register a username that already exists.
     */
    public static class UserAlreadyExistsException extends ChatAppException {
        public UserAlreadyExistsException(String username) {
            super("Username already taken: " + username, HttpStatus.CONFLICT, "USER_ALREADY_EXISTS");
        }
    }

    // ─── Message Exceptions ──────────────────────────────────────────────────

    /**
     * Thrown when a message cannot be found by ID.
     */
    public static class MessageNotFoundException extends ChatAppException {
        public MessageNotFoundException(Long id) {
            super("Message not found with id: " + id, HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND");
        }
    }

    /**
     * Thrown when message content fails validation.
     */
    public static class InvalidMessageException extends ChatAppException {
        public InvalidMessageException(String reason) {
            super("Invalid message: " + reason, HttpStatus.BAD_REQUEST, "INVALID_MESSAGE");
        }
    }

    // ─── Image Exceptions ────────────────────────────────────────────────────

    /**
     * Thrown when image upload fails (size limit, format, IO error).
     */
    public static class ImageUploadException extends ChatAppException {
        public ImageUploadException(String reason) {
            super("Image upload failed: " + reason, HttpStatus.BAD_REQUEST, "IMAGE_UPLOAD_FAILED");
        }

        public ImageUploadException(String reason, Throwable cause) {
            super("Image upload failed: " + reason, HttpStatus.BAD_REQUEST, "IMAGE_UPLOAD_FAILED", cause);
        }
    }

    /**
     * Thrown when uploaded file exceeds the maximum allowed size.
     */
    public static class FileSizeLimitExceededException extends ChatAppException {
        public FileSizeLimitExceededException(long maxSizeMb) {
            super("File size exceeds maximum allowed size of " + maxSizeMb + "MB",
                  HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE");
        }
    }

    /**
     * Thrown when the uploaded file type is not supported.
     */
    public static class UnsupportedFileTypeException extends ChatAppException {
        public UnsupportedFileTypeException(String contentType) {
            super("Unsupported file type: " + contentType + ". Only images are allowed.",
                  HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE");
        }
    }

    // ─── WebSocket Exceptions ────────────────────────────────────────────────

    /**
     * Thrown when a WebSocket operation fails.
     */
    public static class WebSocketException extends ChatAppException {
        public WebSocketException(String message) {
            super(message, HttpStatus.INTERNAL_SERVER_ERROR, "WEBSOCKET_ERROR");
        }

        public WebSocketException(String message, Throwable cause) {
            super(message, HttpStatus.INTERNAL_SERVER_ERROR, "WEBSOCKET_ERROR", cause);
        }
    }
}
