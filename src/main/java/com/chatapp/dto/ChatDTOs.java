package com.chatapp.dto;

import com.chatapp.entity.Message;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Objects for Chat messaging.
 *
 * This file contains all DTO classes used for WebSocket messaging
 * and REST API communication. Keeping related DTOs together simplifies
 * imports and provides a single source of truth for message schemas.
 *
 * @author ChatApp Team
 */
public class ChatDTOs {

    // ─── INBOUND DTOs (Client → Server) ─────────────────────────────────────

    /**
     * DTO for sending a new chat message via WebSocket.
     * Used by @MessageMapping endpoints.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class SendMessageRequest {

        @NotBlank(message = "Sender username is required")
        @Size(max = 50)
        private String senderUsername;

        /** Null for group messages, set for private messages */
        @Size(max = 50)
        private String receiverUsername;

        /** Message text content. Can be null for image-only messages. */
        @Size(max = 5000, message = "Message content cannot exceed 5000 characters")
        private String content;

        @NotNull(message = "Message type is required")
        private Message.MessageType messageType;

        /** Base64 encoded image data (for small images sent via WebSocket) */
        private String imageBase64;

        /** Image MIME type (e.g., image/jpeg) */
        private String imageContentType;

        /** True = group/broadcast message, False = private message */
        private boolean groupMessage;
    }

    /**
     * DTO for user login/join request.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserJoinRequest {

        @NotBlank(message = "Username is required")
        @Size(min = 2, max = 50, message = "Username must be 2-50 characters")
        private String username;

        @Size(max = 100)
        private String displayName;
    }

    // ─── OUTBOUND DTOs (Server → Client) ────────────────────────────────────

    /**
     * DTO for delivering a message to WebSocket clients.
     * Sent back to clients after a message is processed and persisted.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageResponse {

        private Long id;
        private String senderUsername;
        private String senderDisplayName;
        private String receiverUsername;
        private String content;
        private Message.MessageType messageType;
        private String imageUrl;
        private String imageBase64;       // For small inline images
        private String imageContentType;
        private boolean groupMessage;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime timestamp;

        /** For read-receipt tracking */
        private boolean read;
    }

    /**
     * DTO representing a user's online presence.
     * Broadcast to all clients when a user joins or leaves.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserPresenceEvent {

        private String username;
        private String displayName;
        private UserStatus status;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime timestamp;

        public enum UserStatus {
            JOINED, LEFT, ONLINE, OFFLINE
        }
    }

    /**
     * DTO representing a user visible in the online users list.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OnlineUserDTO {
        private Long id;
        private String username;
        private String displayName;
        private boolean online;
        private String avatarUrl;
    }

    /**
     * Generic API response wrapper for REST endpoints.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private String error;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Builder.Default
        private LocalDateTime timestamp = LocalDateTime.now();

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> success(String message, T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String errorMessage) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .error(errorMessage)
                    .build();
        }
    }
}
